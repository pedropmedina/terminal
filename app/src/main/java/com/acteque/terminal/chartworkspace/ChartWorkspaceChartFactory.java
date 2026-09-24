package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;

/** Creates independently owned charts for a workspace. */
@FunctionalInterface
interface ChartWorkspaceChartFactory {
  /**
   * Creates a chart initialized from workspace settings.
   *
   * @param settings the chart settings to apply
   * @return the created chart
   */
  Chart create(ChartWorkspaceSettings settings);
}
