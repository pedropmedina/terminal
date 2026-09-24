package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Toggle;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.dialog.DialogContent;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.inputgroup.InputGroup;
import com.acteque.terminal.ui.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.inputgroup.InputGroupButton;
import com.acteque.terminal.ui.inputgroup.InputGroupInput;
import com.acteque.terminal.ui.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.togglegroup.ToggleGroupItem;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for chart interval selection. */
final class ChartWorkspaceIntervalSelectionViewBuilder implements Builder<Dialog>, ReloadTarget {

  private static final int COLUMN_COUNT = 6;

  private final ChartWorkspaceIntervalSelectionModel model;
  private final Consumer<ChartInterval> intervalSelectedHandler;
  private final Dialog root = new Dialog();
  private final InputGroupInput intervalField = new InputGroupInput();
  private final InputGroupButton addIntervalButton = new InputGroupButton(
    "Add interval",
    new LucideIcon(LucideIcons.PLUS)
  );
  private final ChartWorkspaceAddIntervalDialogView addIntervalDialog;
  private final VBox categories = new VBox();
  private final Label noMatches = new Label("No matching intervals");
  private final Map<ChartInterval, ToggleGroupItem> intervalItems = new HashMap<>();

  /**
   * Creates and connects the interval-selection JavaFX composition.
   *
   * @param model the observable interval-selection state
   * @param queryChangedHandler the filter-query callback
   * @param intervalAddedHandler the custom-interval callback
   * @param intervalSelectedHandler the interval-selection callback
   * @param soleMatchRequestedHandler the sole-match selection callback
   * @param closeRequestHandler the dialog-close callback
   */
  ChartWorkspaceIntervalSelectionViewBuilder(
    ChartWorkspaceIntervalSelectionModel model,
    Consumer<String> queryChangedHandler,
    Consumer<ChartInterval> intervalAddedHandler,
    Consumer<ChartInterval> intervalSelectedHandler,
    Runnable soleMatchRequestedHandler,
    Runnable closeRequestHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.intervalSelectedHandler = Objects.requireNonNull(
      intervalSelectedHandler,
      "intervalSelectedHandler cannot be null"
    );
    Consumer<String> validatedQueryChangedHandler = Objects.requireNonNull(
      queryChangedHandler,
      "queryChangedHandler cannot be null"
    );
    Consumer<ChartInterval> validatedIntervalAddedHandler = Objects.requireNonNull(
      intervalAddedHandler,
      "intervalAddedHandler cannot be null"
    );
    Runnable validatedSoleMatchRequestedHandler = Objects.requireNonNull(
      soleMatchRequestedHandler,
      "soleMatchRequestedHandler cannot be null"
    );
    Runnable validatedCloseRequestHandler = Objects.requireNonNull(
      closeRequestHandler,
      "closeRequestHandler cannot be null"
    );
    addIntervalDialog = new ChartWorkspaceAddIntervalDialogView(model::isAvailable);

    configureDialog();
    configureSearchField();
    configureAddIntervalDialog(validatedIntervalAddedHandler);
    configureResults();
    connectComponents(validatedQueryChangedHandler, validatedSoleMatchRequestedHandler, validatedCloseRequestHandler);

    refreshView();
    displayOpenState(model.isOpen());
    ReloadHooks.register(this);
  }

  /**
   * Returns the assembled interval-selection dialog.
   *
   * @return the interval-selection dialog
   */
  @Override
  public Dialog build() {
    return root;
  }

  /** Rebuilds reloadable dialog content while retaining model listeners and controls. */
  @Override
  public void refreshView() {
    intervalField.setPromptText("Change interval e.g. 5m, 1h");
    intervalField.setAccessibleText("Filter chart intervals");
    rebuildCategories();

    InputGroup searchGroup = new InputGroup(
      intervalField,
      new InputGroupAddon(InputGroupAlignment.INLINE_END, addIntervalButton)
    );
    DialogContent card = new DialogContent(searchGroup, categories, noMatches);
    card.getStyleClass().add("chart-workspace-interval-selection-card");
    card.setShowCloseButton(false);
    card.setMaxHeight(Dialog.USE_PREF_SIZE);
    root.setContent(card);
  }

  /** Configures the outer dialog's stable structural styling. */
  private void configureDialog() {
    root.getStyleClass().add("chart-workspace-interval-selection-dialog");
  }

  /** Configures the interval filter field and add-interval affordance. */
  private void configureSearchField() {
    intervalField.getStyleClass().add("chart-workspace-interval-search-field");
    addIntervalButton.getStyleClass().add("chart-workspace-interval-add-button");
    addIntervalButton.setAccessibleText("Add chart interval");
    addIntervalButton.setOnAction(ignored -> addIntervalDialog.openForEntry());
  }

  /**
   * Configures the nested custom-interval dialog and its modal relationship to the outer dialog.
   *
   * @param intervalAddedHandler the validated custom-interval callback
   */
  private void configureAddIntervalDialog(Consumer<ChartInterval> intervalAddedHandler) {
    addIntervalDialog.onIntervalAdded(intervalAddedHandler);
    addIntervalDialog.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (root.getContent() != null) {
        root.getContent().setDisable(isOpen);
      }
      root.setDismissible(!isOpen);
    });
    root.getChildren().add(addIntervalDialog);
  }

  /** Configures the categorized result and empty-state containers. */
  private void configureResults() {
    categories.getStyleClass().add("chart-workspace-interval-categories");
    noMatches.getStyleClass().add("chart-workspace-interval-no-matches");
    noMatches.setMaxWidth(Double.MAX_VALUE);
  }

  /**
   * Connects model observations and forwards control intents to the interactor.
   *
   * @param queryChangedHandler the validated filter-query callback
   * @param soleMatchRequestedHandler the validated sole-match selection callback
   * @param closeRequestHandler the validated dialog-close callback
   */
  private void connectComponents(
    Consumer<String> queryChangedHandler,
    Runnable soleMatchRequestedHandler,
    Runnable closeRequestHandler
  ) {
    intervalField.textProperty().addListener((ignored, oldValue, newValue) -> queryChangedHandler.accept(newValue));
    intervalField.setOnAction(ignored -> soleMatchRequestedHandler.run());

    model.matchingIntervalsProperty().addListener(ignored -> rebuildCategories());
    model.currentIntervalProperty().addListener(ignored -> updateSelectedInterval());
    model.availabilityProperty().addListener(ignored -> {
      addIntervalDialog.refreshAvailability();
      rebuildCategories();
    });

    model.openProperty().addListener((ignored, wasOpen, isOpen) -> displayOpenState(isOpen));
    root.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && model.isOpen()) {
        closeRequestHandler.run();
      }
    });
  }

  /**
   * Mirrors model visibility into the dialog and prepares the filter field when opened.
   *
   * @param open true to open the dialog
   */
  private void displayOpenState(boolean open) {
    root.setOpen(open);
    if (open) {
      intervalField.clear();
      Platform.runLater(intervalField::requestFocus);
    }
  }

  /** Rebuilds categorized interval controls from the current model results. */
  private void rebuildCategories() {
    Map<String, List<ChartInterval>> matchingByCategory = model.getMatchingIntervals();
    intervalItems.clear();
    categories.getChildren().setAll(
      matchingByCategory
        .entrySet()
        .stream()
        .map(entry -> createCategory(entry.getKey(), entry.getValue()))
        .toList()
    );
    boolean hasMatches = !matchingByCategory.isEmpty();
    categories.setManaged(hasMatches);
    categories.setVisible(hasMatches);
    noMatches.setManaged(!hasMatches);
    noMatches.setVisible(!hasMatches);
  }

  /**
   * Creates a labeled category containing rows of interval controls.
   *
   * @param name the category display name
   * @param intervals the category's matching intervals
   * @return the composed category
   */
  private VBox createCategory(String name, List<ChartInterval> intervals) {
    Label heading = new Label(name);
    heading.getStyleClass().add("chart-workspace-interval-category-title");

    VBox rows = new VBox();
    rows.getStyleClass().add("chart-workspace-interval-toggle-rows");
    for (int start = 0; start < intervals.size(); start += COLUMN_COUNT) {
      int end = Math.min(start + COLUMN_COUNT, intervals.size());
      ToggleGroupItem[] items = intervals
        .subList(start, end)
        .stream()
        .map(this::createIntervalItem)
        .toArray(ToggleGroupItem[]::new);
      ToggleGroup group = new ToggleGroup(items);
      group.getStyleClass().add("chart-workspace-interval-toggle-group");
      group.setVariant(Toggle.Variant.OUTLINE);
      rows.getChildren().add(group);
    }

    VBox category = new VBox(heading, rows);
    category.getStyleClass().add("chart-workspace-interval-category");
    return category;
  }

  /**
   * Creates an interval control that reflects availability and forwards selection intent.
   *
   * @param interval the represented interval
   * @return the configured interval control
   */
  private ToggleGroupItem createIntervalItem(ChartInterval interval) {
    ToggleGroupItem item = new ToggleGroupItem(interval.name());
    item.getStyleClass().add("chart-workspace-interval-button");
    item.setFocusTraversable(true);
    item.setAccessibleText(ChartIntervalText.description(interval));
    item.setSelected(interval.equals(model.getCurrentInterval()));
    item.setDisable(!model.isAvailable(interval));
    if (item.isDisabled()) {
      item.setAccessibleText(ChartIntervalText.description(interval) + ", unavailable from this provider");
    }
    intervalItems.put(interval, item);

    item.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        item.fire();
        event.consume();
      }
    });
    item.setOnAction(ignored -> intervalSelectedHandler.accept(interval));
    return item;
  }

  /** Updates the currently rendered interval controls to reflect the model selection. */
  private void updateSelectedInterval() {
    ChartInterval currentInterval = model.getCurrentInterval();
    intervalItems.forEach((candidate, item) -> item.setSelected(candidate.equals(currentInterval)));
  }
}
