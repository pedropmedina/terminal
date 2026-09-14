package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies chart-menu state transitions and routes menu requests without depending on its layout. */
final class ChartMenuInteractor {

  private final ChartMenuModel model;
  private Consumer<Item> actionRequestedHandler = ignored -> {};

  ChartMenuInteractor(ChartMenuModel model) {
    this.model = Objects.requireNonNull(model, "model");
  }

  void initialize() {
    model.setItems(List.of(Item.INSTRUMENT, Item.INTERVAL, Item.CHART_TYPE));
  }

  void onActionRequested(Consumer<Item> callback) {
    actionRequestedHandler = Objects.requireNonNull(callback, "callback");
  }

  void request(Item item) {
    Objects.requireNonNull(item, "item");
    if (!model.getItems().contains(item)) {
      throw new IllegalArgumentException("Item is not available: " + item);
    }
    actionRequestedHandler.accept(item);
  }
}
