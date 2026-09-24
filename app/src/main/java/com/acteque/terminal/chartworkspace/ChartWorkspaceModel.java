package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart-workspace MVCI components. */
final class ChartWorkspaceModel {

  private final ReadOnlyObjectWrapper<ChartWorkspaceItem> root = new ReadOnlyObjectWrapper<>(this, "root");
  private final ReadOnlyObjectWrapper<Chart> activeChart = new ReadOnlyObjectWrapper<>(this, "activeChart");
  private final ReadOnlyBooleanWrapper multipleCharts = new ReadOnlyBooleanWrapper(this, "multipleCharts");

  /**
   * Returns the root of the recursively split workspace tree.
   *
   * @return the workspace root, or null before initialization
   */
  ChartWorkspaceItem getRoot() {
    return root.get();
  }

  /**
   * Returns the observable workspace root.
   *
   * @return the read-only root property
   */
  ReadOnlyObjectProperty<ChartWorkspaceItem> rootProperty() {
    return root.getReadOnlyProperty();
  }

  /**
   * Replaces the workspace tree root.
   *
   * @param value the new workspace root
   */
  void setRoot(ChartWorkspaceItem value) {
    root.set(value);
  }

  /**
   * Returns the chart that receives workspace commands.
   *
   * @return the active chart, or null before initialization
   */
  Chart getActiveChart() {
    return activeChart.get();
  }

  /**
   * Returns the observable active chart.
   *
   * @return the read-only active-chart property
   */
  ReadOnlyObjectProperty<Chart> activeChartProperty() {
    return activeChart.getReadOnlyProperty();
  }

  /**
   * Updates the chart that receives workspace commands.
   *
   * @param value the active chart
   */
  void setActiveChart(Chart value) {
    activeChart.set(value);
  }

  /**
   * Reports whether the workspace contains more than one chart.
   *
   * @return true when multiple charts are present
   */
  boolean hasMultipleCharts() {
    return multipleCharts.get();
  }

  /**
   * Returns the observable multiple-chart state.
   *
   * @return the read-only multiple-chart property
   */
  ReadOnlyBooleanProperty multipleChartsProperty() {
    return multipleCharts.getReadOnlyProperty();
  }

  /**
   * Updates whether the workspace contains multiple charts.
   *
   * @param value true when multiple charts are present
   */
  void setMultipleCharts(boolean value) {
    multipleCharts.set(value);
  }
}
