package com.acteque.terminal.chart;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.paint.Color;

/** Observable state shared by the chart's MVCI components. */
final class ChartModel {

  private final ReadOnlyBooleanWrapper instrumentSearchOpen = new ReadOnlyBooleanWrapper(this, "instrumentSearchOpen");
  private final ReadOnlyBooleanWrapper intervalSelectionOpen = new ReadOnlyBooleanWrapper(
    this,
    "intervalSelectionOpen"
  );
  private final ReadOnlyBooleanWrapper identifierVisible = new ReadOnlyBooleanWrapper(this, "identifierVisible");
  private final ReadOnlyObjectWrapper<Color> identifierColor = new ReadOnlyObjectWrapper<>(
    this,
    "identifierColor",
    Color.TRANSPARENT
  );
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(this, "chartType");
  private final ReadOnlyObjectWrapper<String> symbol = new ReadOnlyObjectWrapper<>(this, "symbol");
  private final ReadOnlyStringWrapper loadError = new ReadOnlyStringWrapper(this, "loadError");
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

  ReadOnlyBooleanProperty identifierVisibleProperty() {
    return identifierVisible.getReadOnlyProperty();
  }

  void setIdentifierVisible(boolean value) {
    identifierVisible.set(value);
  }

  ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return identifierColor.getReadOnlyProperty();
  }

  void setIdentifierColor(Color value) {
    identifierColor.set(value);
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

  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  void setChartType(ChartType value) {
    chartType.set(value);
  }

  String getSymbol() {
    return symbol.get();
  }

  ReadOnlyObjectProperty<String> symbolProperty() {
    return symbol.getReadOnlyProperty();
  }

  void setSymbol(String value) {
    symbol.set(value);
  }

  ReadOnlyStringProperty loadErrorProperty() {
    return loadError.getReadOnlyProperty();
  }

  void setLoadError(String value) {
    loadError.set(value);
  }
}
