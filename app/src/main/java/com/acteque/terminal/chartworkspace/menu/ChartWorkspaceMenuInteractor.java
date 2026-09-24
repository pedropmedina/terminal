package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenuModel.Item;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies workspace-menu state transitions and routes requests without depending on layout. */
final class ChartWorkspaceMenuInteractor {

  private static final List<Item> SINGLE_CHART_ITEMS = List.of(
    Item.INSTRUMENT,
    Item.INTERVAL,
    Item.CHART_TYPE,
    Item.SPLIT
  );
  private static final List<Item> MULTIPLE_CHART_ITEMS = List.of(
    Item.INSTRUMENT,
    Item.INTERVAL,
    Item.CHART_TYPE,
    Item.SPLIT,
    Item.CLOSE
  );

  private final ChartWorkspaceMenuModel model;
  private Consumer<Item> actionRequestedHandler = ignored -> {};

  /**
   * Creates an interactor backed by the supplied workspace-menu state.
   *
   * @param model the observable workspace-menu state
   */
  ChartWorkspaceMenuInteractor(ChartWorkspaceMenuModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  /**
   * Initializes the active chart and the single-chart item set.
   *
   * @param activeChart the chart initially represented by the menu
   */
  void initialize(Chart activeChart) {
    setActiveChart(activeChart);
    setMultipleCharts(false);
  }

  /**
   * Rebinds menu state to the active chart.
   *
   * @param chart the active workspace chart
   */
  void setActiveChart(Chart chart) {
    model.bind(Objects.requireNonNull(chart, "chart cannot be null"));
  }

  /**
   * Updates the available actions for the current workspace size.
   *
   * @param multipleCharts true when the workspace contains multiple charts
   */
  void setMultipleCharts(boolean multipleCharts) {
    model.setItems(multipleCharts ? MULTIPLE_CHART_ITEMS : SINGLE_CHART_ITEMS);
  }

  /**
   * Registers the callback that receives validated menu requests.
   *
   * @param callback the menu-item request callback
   */
  void onActionRequested(Consumer<Item> callback) {
    actionRequestedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Forwards an available menu item to the registered request callback.
   *
   * @param item the requested menu item
   * @throws IllegalArgumentException when the item is not currently available
   */
  void request(Item item) {
    Item requestedItem = Objects.requireNonNull(item, "item cannot be null");
    if (!model.getItems().contains(requestedItem)) {
      throw new IllegalArgumentException("Item is not available: " + requestedItem);
    }

    actionRequestedHandler.accept(requestedItem);
  }

  /** Releases every property binding to the active chart. */
  void close() {
    model.unbind();
  }
}
