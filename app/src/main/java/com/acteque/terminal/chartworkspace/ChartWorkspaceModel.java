package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

final class ChartWorkspaceModel {

  private final ReadOnlyObjectWrapper<ChartWorkspaceItem> root = new ReadOnlyObjectWrapper<>(this, "root");
  private final ReadOnlyObjectWrapper<Chart> activeChart = new ReadOnlyObjectWrapper<>(this, "activeChart");

  ChartWorkspaceItem getRoot() {
    return root.get();
  }

  ReadOnlyObjectProperty<ChartWorkspaceItem> rootProperty() {
    return root.getReadOnlyProperty();
  }

  void setRoot(ChartWorkspaceItem value) {
    root.set(value);
  }

  Chart getActiveChart() {
    return activeChart.get();
  }

  ReadOnlyObjectProperty<Chart> activeChartProperty() {
    return activeChart.getReadOnlyProperty();
  }

  void setActiveChart(Chart value) {
    activeChart.set(value);
  }
}
