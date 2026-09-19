package com.acteque.terminal.chart;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableBooleanValue;

/** Observable state shared by the chart's MVCI components. */
final class ChartModel {

  private final ReadOnlyBooleanWrapper instrumentSearchOpen = new ReadOnlyBooleanWrapper(this, "instrumentSearchOpen");
  private final ReadOnlyBooleanWrapper intervalSelectionOpen = new ReadOnlyBooleanWrapper(
    this,
    "intervalSelectionOpen"
  );
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(this, "chartType");
  private final ReadOnlyObjectWrapper<String> symbol = new ReadOnlyObjectWrapper<>(this, "symbol");
  private final BooleanBinding modalOpen = instrumentSearchOpen.or(intervalSelectionOpen);

  boolean isInstrumentSearchOpen() {
    return instrumentSearchOpen.get();
  }

  ReadOnlyBooleanProperty instrumentSearchOpenProperty() {
    return instrumentSearchOpen.getReadOnlyProperty();
  }

  void setInstrumentSearchOpen(boolean value) {
    instrumentSearchOpen.set(value);
  }

  boolean isIntervalSelectionOpen() {
    return intervalSelectionOpen.get();
  }

  ReadOnlyBooleanProperty intervalSelectionOpenProperty() {
    return intervalSelectionOpen.getReadOnlyProperty();
  }

  void setIntervalSelectionOpen(boolean value) {
    intervalSelectionOpen.set(value);
  }

  boolean isModalOpen() {
    return modalOpen.get();
  }

  ObservableBooleanValue modalOpenProperty() {
    return modalOpen;
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

  void setChartType(ChartType value) {
    chartType.set(value);
  }

  String getSymbol() {
    return symbol.get();
  }

  void setSymbol(String value) {
    symbol.set(value);
  }
}
