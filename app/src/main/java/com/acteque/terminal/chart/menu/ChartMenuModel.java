package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import java.util.List;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;

/** Observable state shared by the chart menu's MVCI components. */
final class ChartMenuModel {

  enum Item {
    INSTRUMENT("Symbol or instrument"),
    INTERVAL("Interval"),
    CHART_TYPE("Chart type"),
    SPLIT("Split chart"),
    CLOSE("Close chart");

    private final String description;

    Item(String description) {
      this.description = description;
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

  private final ReadOnlyStringWrapper instrumentSymbol = new ReadOnlyStringWrapper(this, "instrumentSymbol", "");
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");

  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(
    this,
    "chartType",
    ChartType.LINE
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

  String getInstrumentSymbol() {
    return instrumentSymbol.get();
  }

  ReadOnlyStringProperty instrumentSymbolProperty() {
    return instrumentSymbol.getReadOnlyProperty();
  }

  void setInstrumentSymbol(String value) {
    instrumentSymbol.set(value);
  }

  ChartInterval getInterval() {
    return interval.get();
  }

  ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return interval.getReadOnlyProperty();
  }

  void setInterval(ChartInterval value) {
    interval.set(value);
  }

  ChartType getChartType() {
    return chartType.get();
  }

  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  void setChartType(ChartType value) {
    chartType.set(value);
  }
}
