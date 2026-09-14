package com.acteque.terminal.chart.canvas;

/** Defines viewport constraints shared by chart interaction and axis layout. */
final class YAxisPolicy {

  private static final double MIN_Y_AXIS_LABEL_SPACING = 24.0;
  private static final double MIN_PRICE_INCREMENT = 0.01;

  private YAxisPolicy() {}

  static int maximumYAxisIntervalCount(double chartHeight) {
    if (!Double.isFinite(chartHeight) || chartHeight <= 0.0) {
      return 1;
    }
    return Math.max(1, (int) Math.floor(chartHeight / MIN_Y_AXIS_LABEL_SPACING));
  }

  static double minimumPriceSpan(double chartHeight) {
    return maximumYAxisIntervalCount(chartHeight) * MIN_PRICE_INCREMENT;
  }
}
