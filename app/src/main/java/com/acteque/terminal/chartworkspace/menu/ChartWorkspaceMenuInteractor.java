package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenuModel.Item;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies workspace-menu state transitions and routes requests without depending on layout. */
final class ChartWorkspaceMenuInteractor {

  private final ChartWorkspaceMenuModel model;
  private Consumer<Item> actionRequestedHandler = ignored -> {};

  ChartWorkspaceMenuInteractor(ChartWorkspaceMenuModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  void initialize(Chart activeChart) {
    setActiveChart(activeChart);
    setMultipleCharts(false);
  }

  void setActiveChart(Chart chart) {
    model.bind(Objects.requireNonNull(chart, "chart cannot be null"));
  }

  void setMultipleCharts(boolean value) {
    model.setItems(
      value
        ? List.of(Item.INSTRUMENT, Item.INTERVAL, Item.CHART_TYPE, Item.SPLIT, Item.CLOSE)
        : List.of(Item.INSTRUMENT, Item.INTERVAL, Item.CHART_TYPE, Item.SPLIT)
    );
  }

  void onActionRequested(Consumer<Item> callback) {
    actionRequestedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void request(Item item) {
    Objects.requireNonNull(item, "item cannot be null");
    if (!model.getItems().contains(item)) {
      throw new IllegalArgumentException("Item is not available: " + item);
    }
    actionRequestedHandler.accept(item);
  }

  void close() {
    model.unbind();
  }
}
