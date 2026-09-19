package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import java.util.Objects;

record ChartWorkspaceSettings(String symbol, ChartInterval interval, ChartType chartType) {
  ChartWorkspaceSettings {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");
    Objects.requireNonNull(chartType, "chartType cannot be null");
  }
}
