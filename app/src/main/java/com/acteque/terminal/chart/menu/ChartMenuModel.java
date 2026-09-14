package com.acteque.terminal.chart.menu;

import java.util.List;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.FXCollections;

/** Observable state shared by the chart menu's MVCI components. */
final class ChartMenuModel {

  enum Item {
    INSTRUMENT("S", "Symbol or instrument"),
    INTERVAL("I", "Interval"),
    CHART_TYPE("C", "Chart type");

    private final String label;
    private final String description;

    Item(String label, String description) {
      this.label = label;
      this.description = description;
    }

    String label() {
      return label;
    }

    String description() {
      return description;
    }
  }

  private final ReadOnlyListWrapper<Item> items = new ReadOnlyListWrapper<>(
    this,
    "items",
    FXCollections.observableArrayList()
  );

  List<Item> getItems() {
    return List.copyOf(items);
  }

  ReadOnlyListProperty<Item> itemsProperty() {
    return items.getReadOnlyProperty();
  }

  void setItems(List<Item> values) {
    items.setAll(values);
  }
}
