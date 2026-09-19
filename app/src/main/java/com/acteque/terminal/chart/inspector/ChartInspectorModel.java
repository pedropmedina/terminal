package com.acteque.terminal.chart.inspector;

import com.acteque.terminal.chart.ChartType;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart inspector MVCI components. */
final class ChartInspectorModel {

  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(this, "chartType");
  private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");

  ChartType getChartType() {
    return chartType.get();
  }

  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  void setChartType(ChartType value) {
    chartType.set(value);
  }

  boolean isOpen() {
    return open.get();
  }

  ReadOnlyBooleanProperty openProperty() {
    return open.getReadOnlyProperty();
  }

  void setOpen(boolean value) {
    open.set(value);
  }
}
