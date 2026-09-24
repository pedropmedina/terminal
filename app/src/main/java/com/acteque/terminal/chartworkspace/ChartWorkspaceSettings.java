package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import java.util.Objects;

/**
 * Settings required to create a chart within the workspace.
 *
 * @param symbol the chart instrument symbol
 * @param interval the chart interval
 * @param chartType the chart rendering type
 */
record ChartWorkspaceSettings(String symbol, ChartInterval interval, ChartType chartType) {
  /**
   * Validates every required chart setting.
   *
   * @param symbol the chart instrument symbol
   * @param interval the chart interval
   * @param chartType the chart rendering type
   */
  ChartWorkspaceSettings {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");
    Objects.requireNonNull(chartType, "chartType cannot be null");
  }
}
