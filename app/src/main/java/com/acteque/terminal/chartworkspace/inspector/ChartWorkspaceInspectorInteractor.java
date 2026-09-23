package com.acteque.terminal.chartworkspace.inspector;

import com.acteque.terminal.chart.ChartType;
import java.util.Objects;
import java.util.function.Consumer;

/** Applies chart-inspector state transitions without depending on its layout. */
final class ChartWorkspaceInspectorInteractor {

  private final ChartWorkspaceInspectorModel model;
  private Consumer<ChartType> chartTypeSelectedHandler = ignored -> {};

  /**
   * Creates an interactor backed by the supplied inspector state.
   *
   * @param model the observable inspector state
   */
  ChartWorkspaceInspectorInteractor(ChartWorkspaceInspectorModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  /**
   * Initializes the selected chart type and closes the inspector.
   *
   * @param chartType the initial chart type
   */
  void initialize(ChartType chartType) {
    setChartType(chartType);
    setOpen(false);
  }

  /** Opens the inspector to display chart-type choices. */
  void showChartTypes() {
    setOpen(true);
  }

  /** Closes the inspector. */
  void close() {
    setOpen(false);
  }

  /**
   * Updates the inspector's open state.
   *
   * @param value true to open the inspector
   */
  void setOpen(boolean value) {
    model.setOpen(value);
  }

  /**
   * Updates the chart type displayed as selected without notifying the selection callback.
   *
   * @param chartType the chart type to display as selected
   */
  void setChartType(ChartType chartType) {
    model.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  /**
   * Registers the action invoked after a chart type is selected.
   *
   * @param callback the selected-chart-type callback
   */
  void onChartTypeSelected(Consumer<ChartType> callback) {
    chartTypeSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Applies a user-selected chart type, closes the inspector, and notifies its listener.
   *
   * @param chartType the selected chart type
   */
  void selectChartType(ChartType chartType) {
    ChartType selectedChartType = Objects.requireNonNull(chartType, "chartType cannot be null");

    model.setChartType(selectedChartType);
    model.setOpen(false);
    chartTypeSelectedHandler.accept(selectedChartType);
  }
}
