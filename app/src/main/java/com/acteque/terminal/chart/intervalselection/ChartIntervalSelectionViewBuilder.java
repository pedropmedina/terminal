package com.acteque.terminal.chart.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Toggle;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.dialog.DialogContent;
import com.acteque.terminal.ui.core.inputgroup.InputGroup;
import com.acteque.terminal.ui.core.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.core.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.core.inputgroup.InputGroupButton;
import com.acteque.terminal.ui.core.inputgroup.InputGroupInput;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroupItem;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for chart interval selection. */
final class ChartIntervalSelectionViewBuilder implements Builder<Dialog>, RefreshableView {

  private static final int COLUMN_COUNT = 6;

  private final ChartIntervalSelectionModel model;
  private final Consumer<String> queryChangedHandler;
  private final Consumer<ChartInterval> intervalSelectedHandler;
  private final Runnable soleMatchRequestedHandler;
  private final Dialog root = new Dialog();
  private final InputGroupInput intervalField = new InputGroupInput();
  private final InputGroupButton addIntervalButton = new InputGroupButton(
    "Add interval",
    new LucideIcon(LucideIcons.PLUS)
  );
  private final ChartAddIntervalDialog addIntervalDialog = new ChartAddIntervalDialog();
  private final VBox categories = new VBox();
  private final Label noMatches = new Label("No matching intervals");
  private final Map<ChartInterval, ToggleGroupItem> intervalItems = new HashMap<>();

  ChartIntervalSelectionViewBuilder(
    ChartIntervalSelectionModel model,
    ObservableBooleanValue open,
    Consumer<String> queryChangedHandler,
    Consumer<ChartInterval> intervalAddedHandler,
    Consumer<ChartInterval> intervalSelectedHandler,
    Runnable soleMatchRequestedHandler,
    Runnable closeRequestHandler
  ) {
    this.model = Objects.requireNonNull(model, "model");
    this.queryChangedHandler = Objects.requireNonNull(queryChangedHandler, "queryChangedHandler");
    this.intervalSelectedHandler = Objects.requireNonNull(intervalSelectedHandler, "intervalSelectedHandler");
    this.soleMatchRequestedHandler = Objects.requireNonNull(soleMatchRequestedHandler, "soleMatchRequestedHandler");
    Objects.requireNonNull(intervalAddedHandler, "intervalAddedHandler");
    Objects.requireNonNull(closeRequestHandler, "closeRequestHandler");
    Objects.requireNonNull(open, "open");

    root.getStyleClass().add("chart-interval-selection-dialog");
    intervalField.getStyleClass().add("chart-interval-search-field");
    addIntervalButton.getStyleClass().add("chart-interval-add-button");
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

    categories.getStyleClass().add("chart-interval-categories");
    noMatches.getStyleClass().add("chart-interval-no-matches");
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
    ChartReloadHooks.register(this);
  }

  @Override
  public Dialog build() {
    return root;
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
    card.getStyleClass().add("chart-interval-selection-card");
    card.setShowCloseButton(false);
    card.setMaxHeight(Dialog.USE_PREF_SIZE);
    root.setContent(card);
  }

  void close() {
    root.close();
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
    heading.getStyleClass().add("chart-interval-category-title");

    VBox rows = new VBox();
    rows.getStyleClass().add("chart-interval-toggle-rows");
    for (int start = 0; start < intervals.size(); start += COLUMN_COUNT) {
      int end = Math.min(start + COLUMN_COUNT, intervals.size());
      ToggleGroupItem[] items = intervals
        .subList(start, end)
        .stream()
        .map(this::createIntervalItem)
        .toArray(ToggleGroupItem[]::new);
      ToggleGroup group = new ToggleGroup(items);
      group.getStyleClass().add("chart-interval-toggle-group");
      group.setVariant(Toggle.Variant.OUTLINE);
      rows.getChildren().add(group);
    }

    VBox category = new VBox(heading, rows);
    category.getStyleClass().add("chart-interval-category");
    return category;
  }

  private ToggleGroupItem createIntervalItem(ChartInterval interval) {
    ToggleGroupItem item = new ToggleGroupItem(interval.name());
    item.getStyleClass().add("chart-interval-button");
    item.setFocusTraversable(true);
    item.setAccessibleText(interval.description());
    item.setSelected(interval == model.getCurrentInterval());
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
    intervalItems.forEach((candidate, item) -> item.setSelected(candidate == currentInterval));
  }
}
