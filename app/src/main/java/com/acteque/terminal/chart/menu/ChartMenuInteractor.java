package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies chart-menu state transitions and routes menu requests without depending on its layout. */
final class ChartMenuInteractor {

  private final ChartMenuModel model;
  private Consumer<Item> actionRequestedHandler = ignored -> {};

  ChartMenuInteractor(ChartMenuModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  void initialize(String instrumentSymbol, ChartInterval interval) {
    setInstrumentSymbol(instrumentSymbol);
    setInterval(interval);
    model.setItems(List.of(Item.INSTRUMENT, Item.INTERVAL, Item.CHART_TYPE));
  }

  void onActionRequested(Consumer<Item> callback) {
    actionRequestedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void setChartType(ChartType chartType) {
    model.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  void setInstrumentSymbol(String symbol) {
    model.setInstrumentSymbol(Objects.requireNonNull(symbol, "symbol cannot be null"));
  }

  void setInterval(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  void request(Item item) {
    Objects.requireNonNull(item, "item cannot be null");
    if (!model.getItems().contains(item)) {
      throw new IllegalArgumentException("Item is not available: " + item);
    }
    actionRequestedHandler.accept(item);
  }
}
