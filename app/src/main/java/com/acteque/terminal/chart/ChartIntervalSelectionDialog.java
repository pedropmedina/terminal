package com.acteque.terminal.chart;

import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.Toggle;
import com.acteque.terminal.ui.core.Tooltip;
import com.acteque.terminal.ui.core.Tooltip.Side;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.dialog.DialogContent;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroupItem;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** A transient, searchable picker for chart intervals. */
public final class ChartIntervalSelectionDialog extends Dialog implements RefreshableView {

  private static final int COLUMN_COUNT = 6;
  private final Input intervalField = new Input();
  private final Button addIntervalButton = new Button(
    null,
    new LucideIcon(LucideIcons.PLUS),
    Button.Variant.OUTLINE,
    Button.Size.ICON
  );
  private final AddChartIntervalDialog addIntervalDialog = new AddChartIntervalDialog();
  private final VBox categories = new VBox();
  private final Label noMatches = new Label("No matching intervals");
  private final List<ChartInterval> intervals = new ArrayList<>(Arrays.asList(ChartInterval.values()));
  private final Map<ChartInterval, ToggleGroupItem> intervalItems = new HashMap<>();
  private ChartInterval currentInterval;
  private Runnable closeRequestHandler = () -> {};
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  public ChartIntervalSelectionDialog(ChartInterval currentInterval, ObservableBooleanValue open) {
    this.currentInterval = Objects.requireNonNull(currentInterval, "currentInterval");
    Objects.requireNonNull(open, "open");

    getStyleClass().add("chart-interval-selection-dialog");
    intervalField.getStyleClass().add("chart-interval-search-field");
    addIntervalButton.getStyleClass().add("chart-interval-add-button");
    addIntervalButton.setAccessibleText("Add chart interval");
    addIntervalButton.setOnAction(ignored -> addIntervalDialog.openForEntry());
    addIntervalDialog.onIntervalAdded(interval -> {
      intervals.add(interval);
      rebuildCategories(intervalField.getText());
    });
    addIntervalDialog.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (getContent() != null) {
        getContent().setDisable(isOpen);
      }
      setDismissible(!isOpen);
    });
    getChildren().add(addIntervalDialog);
    categories.getStyleClass().add("chart-interval-categories");
    noMatches.getStyleClass().add("chart-interval-no-matches");
    noMatches.setMaxWidth(Double.MAX_VALUE);
    intervalField.textProperty().addListener((ignored, oldValue, newValue) -> rebuildCategories(newValue));
    intervalField.setOnAction(ignored -> selectSoleMatch());

    open.addListener((ignored, wasOpen, isOpen) -> {
      setOpen(isOpen);
      if (isOpen) {
        intervalField.clear();
        Platform.runLater(intervalField::requestFocus);
      }
    });
    openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && open.get()) {
        closeRequestHandler.run();
      }
    });
    setOpen(open.get());

    refreshView();
    ChartReloadHooks.register(this);
  }

  @Override
  public void refreshView() {
    intervalField.setPromptText("Change interval e.g. 5m, 1h");
    intervalField.setAccessibleText("Filter chart intervals");
    rebuildCategories(intervalField.getText());

    HBox searchRow = new HBox(intervalField, addIntervalButton);
    searchRow.getStyleClass().add("chart-interval-search-row");
    HBox.setHgrow(intervalField, Priority.ALWAYS);
    DialogContent card = new DialogContent(searchRow, categories, noMatches);
    card.getStyleClass().add("chart-interval-selection-card");
    card.setShowCloseButton(false);
    card.setMaxHeight(USE_PREF_SIZE);
    setContent(card);
  }

  public void setCurrentInterval(ChartInterval interval) {
    currentInterval = Objects.requireNonNull(interval, "interval");
    rebuildCategories(intervalField.getText());
  }

  public void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback");
  }

  private void rebuildCategories(String query) {
    String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    Map<String, List<ChartInterval>> matchingByCategory = new LinkedHashMap<>();
    intervals
      .stream()
      .filter(interval -> interval.matches(normalizedQuery))
      .forEach(interval ->
        matchingByCategory.computeIfAbsent(interval.category(), ignored -> new ArrayList<>()).add(interval)
      );

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
    ToggleGroupItem item = new ToggleGroupItem(interval.displayName());
    item.getStyleClass().add("chart-interval-button");
    item.setFocusTraversable(true);
    item.setAccessibleText(interval.description());
    item.setSelected(interval == currentInterval);
    intervalItems.put(interval, item);
    Tooltip tooltip = new Tooltip(interval.description());
    tooltip.setSide(Side.RIGHT);
    tooltip.install(item);

    item.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ENTER) {
        item.fire();
        event.consume();
      }
    });
    item.setOnAction(ignored -> select(interval));
    return item;
  }

  private void selectSoleMatch() {
    String query = intervalField.getText() == null ? "" : intervalField.getText().strip().toLowerCase(Locale.ROOT);
    List<ChartInterval> matches = intervals
      .stream()
      .filter(interval -> interval.matches(query))
      .toList();
    if (matches.size() == 1) {
      select(matches.getFirst());
    }
  }

  private void select(ChartInterval interval) {
    currentInterval = interval;
    intervalItems.forEach((candidate, item) -> item.setSelected(candidate == interval));
    close();
    intervalSelectedHandler.accept(interval);
  }
}
