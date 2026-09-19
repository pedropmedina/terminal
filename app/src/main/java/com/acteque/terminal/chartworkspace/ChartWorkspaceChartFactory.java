package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;

@FunctionalInterface
interface ChartWorkspaceChartFactory {
  Chart create(ChartWorkspaceSettings settings);
}
