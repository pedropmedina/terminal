package com.acteque.terminal.chartworkspace.inspector;

import com.acteque.terminal.chart.ChartType;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart inspector MVCI components. */
final class ChartWorkspaceInspectorModel {

  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(this, "chartType");
  private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");

  /**
   * Returns the chart type displayed as selected.
   *
   * @return the selected chart type
   */
  ChartType getChartType() {
    return chartType.get();
  }

  /**
   * Returns the observable selected chart type.
   *
   * @return the read-only chart-type property
   */
  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  /**
   * Updates the chart type displayed as selected.
   *
   * @param value the selected chart type
   */
  void setChartType(ChartType value) {
    chartType.set(value);
  }

  /**
   * Reports whether the inspector is open.
   *
   * @return true when the inspector is open
   */
  boolean isOpen() {
    return open.get();
  }

  /**
   * Returns the observable inspector state.
   *
   * @return the read-only open property
   */
  ReadOnlyBooleanProperty openProperty() {
    return open.getReadOnlyProperty();
  }

  /**
   * Updates the inspector's open state.
   *
   * @param value true to open the inspector
   */
  void setOpen(boolean value) {
    open.set(value);
  }
}
