package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Observable state shared by the workspace menu's MVCI components. */
final class ChartWorkspaceMenuModel {

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
  private final ReadOnlyObjectWrapper<Image> instrumentLogoImage = new ReadOnlyObjectWrapper<>(
    this,
    "instrumentLogoImage"
  );
  private final ReadOnlyStringWrapper instrumentSymbol = new ReadOnlyStringWrapper(this, "instrumentSymbol", "");
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(
    this,
    "chartType",
    ChartType.LINE
  );
  private final ReadOnlyObjectWrapper<Color> identifierColor = new ReadOnlyObjectWrapper<>(
    this,
    "identifierColor",
    Color.TRANSPARENT
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

  Image getInstrumentLogoImage() {
    return instrumentLogoImage.get();
  }

  ReadOnlyObjectProperty<Image> instrumentLogoImageProperty() {
    return instrumentLogoImage.getReadOnlyProperty();
  }

  ChartInterval getInterval() {
    return interval.get();
  }

  ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return interval.getReadOnlyProperty();
  }

  ChartType getChartType() {
    return chartType.get();
  }

  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return identifierColor.getReadOnlyProperty();
  }

  void bind(Chart chart) {
    Objects.requireNonNull(chart, "chart cannot be null");
    unbind();
    instrumentLogoImage.bind(chart.instrumentLogoImageProperty());
    instrumentSymbol.bind(chart.symbolProperty());
    interval.bind(chart.intervalProperty());
    chartType.bind(chart.chartTypeProperty());
    identifierColor.bind(chart.identifierColorProperty());
  }

  void unbind() {
    instrumentLogoImage.unbind();
    instrumentSymbol.unbind();
    interval.unbind();
    chartType.unbind();
    identifierColor.unbind();
  }
}
