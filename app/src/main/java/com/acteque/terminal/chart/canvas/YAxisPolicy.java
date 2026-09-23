package com.acteque.terminal.chart.canvas;

/** Defines viewport constraints shared by chart interaction and axis layout. */
final class YAxisPolicy {

  private static final double MIN_Y_AXIS_LABEL_SPACING = 24.0;
  private static final double MIN_PRICE_INCREMENT = 0.01;

  /** Prevents utility-class instantiation. */
  private YAxisPolicy() {}

  /**
   * @param chartHeight the drawable chart height
   * @return the greatest collision-safe vertical interval count
   */
  static int maximumYAxisIntervalCount(double chartHeight) {
    if (!Double.isFinite(chartHeight) || chartHeight <= 0.0) {
      return 1;
    }
    return Math.max(1, (int) Math.floor(chartHeight / MIN_Y_AXIS_LABEL_SPACING));
  }

  /**
   * @param chartHeight the drawable chart height
   * @return the minimum price span that can retain cent-sized ticks
   */
  static double minimumPriceSpan(double chartHeight) {
    return maximumYAxisIntervalCount(chartHeight) * MIN_PRICE_INCREMENT;
  }
}
