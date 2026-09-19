package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import java.util.Objects;

record ChartWorkspaceLeaf(Chart chart) implements ChartWorkspaceItem {
  ChartWorkspaceLeaf {
    Objects.requireNonNull(chart, "chart cannot be null");
  }
}
