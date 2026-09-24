package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import java.util.Objects;

/**
 * A terminal workspace-tree node containing one chart.
 *
 * @param chart the chart contained by the leaf
 */
record ChartWorkspaceLeaf(Chart chart) implements ChartWorkspaceItem {
  /**
   * Validates the contained chart.
   *
   * @param chart the chart contained by the leaf
   */
  ChartWorkspaceLeaf {
    Objects.requireNonNull(chart, "chart cannot be null");
  }
}
