package com.acteque.terminal.chartworkspace.inspector;

import com.acteque.terminal.chart.ChartType;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies chart-inspector state transitions without depending on its layout. */
final class ChartWorkspaceInspectorInteractor {

  private final ChartWorkspaceInspectorModel model;
  private Consumer<ChartType> chartTypeSelectedHandler = ignored -> {};

  ChartWorkspaceInspectorInteractor(ChartWorkspaceInspectorModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  void initialize(ChartType chartType) {
    setChartType(chartType);
    setOpen(false);
  }

  void showChartTypes() {
    setOpen(true);
  }

  void close() {
    setOpen(false);
  }

  void setOpen(boolean value) {
    model.setOpen(value);
  }

  void setChartType(ChartType chartType) {
    model.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  void onChartTypeSelected(Consumer<ChartType> callback) {
    chartTypeSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void selectChartType(ChartType chartType) {
    ChartType selectedChartType = Objects.requireNonNull(chartType, "chartType cannot be null");
    model.setChartType(selectedChartType);
    model.setOpen(false);
    chartTypeSelectedHandler.accept(selectedChartType);
  }
}
