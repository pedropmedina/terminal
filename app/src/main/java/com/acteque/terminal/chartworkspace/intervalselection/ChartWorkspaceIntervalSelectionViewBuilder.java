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
import java.util.function.Predicate;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
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
  private final ChartWorkspaceAddIntervalDialogView addIntervalDialog = new ChartWorkspaceAddIntervalDialogView();
  private final VBox categories = new VBox();
  private final Label noMatches = new Label("No matching intervals");
  private final Map<ChartInterval, ToggleGroupItem> intervalItems = new HashMap<>();

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
    Objects.requireNonNull(queryChangedHandler, "queryChangedHandler cannot be null");
    Objects.requireNonNull(intervalAddedHandler, "intervalAddedHandler cannot be null");
    Objects.requireNonNull(soleMatchRequestedHandler, "soleMatchRequestedHandler cannot be null");
    Objects.requireNonNull(closeRequestHandler, "closeRequestHandler cannot be null");
    ObservableBooleanValue open = model.openProperty();

    root.getStyleClass().add("chart-workspace-interval-selection-dialog");
    intervalField.getStyleClass().add("chart-workspace-interval-search-field");
    addIntervalButton.getStyleClass().add("chart-workspace-interval-add-button");
    addIntervalButton.setAccessibleText("Add chart interval");
    addIntervalButton.setOnAction(ignored -> addIntervalDialog.openForEntry());
    addIntervalDialog.onIntervalAdded(intervalAddedHandler);
    addIntervalDialog.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (root.getContent() != null) {
        root.getContent().setDisable(isOpen);
      }
      root.setDismissible(!isOpen);
    });
    root.getChildren().add(addIntervalDialog);

    categories.getStyleClass().add("chart-workspace-interval-categories");
    noMatches.getStyleClass().add("chart-workspace-interval-no-matches");
    noMatches.setMaxWidth(Double.MAX_VALUE);
    intervalField.textProperty().addListener((ignored, oldValue, newValue) -> queryChangedHandler.accept(newValue));
    intervalField.setOnAction(ignored -> soleMatchRequestedHandler.run());

    model.matchingIntervalsProperty().addListener(ignored -> rebuildCategories());
    model.currentIntervalProperty().addListener(ignored -> updateSelectedInterval());

    open.addListener((ignored, wasOpen, isOpen) -> {
      root.setOpen(isOpen);
      if (isOpen) {
        intervalField.clear();
        Platform.runLater(intervalField::requestFocus);
      }
    });
    root.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && open.get()) {
        closeRequestHandler.run();
      }
    });
    root.setOpen(open.get());

    refreshView();
    ReloadHooks.register(this);
  }

  @Override
  public Dialog build() {
    return root;
  }

  void setAvailability(Predicate<ChartInterval> availability) {
    addIntervalDialog.setAvailability(availability);
    rebuildCategories();
  }

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

  private void updateSelectedInterval() {
    ChartInterval currentInterval = model.getCurrentInterval();
    intervalItems.forEach((candidate, item) -> item.setSelected(candidate.equals(currentInterval)));
  }
}
