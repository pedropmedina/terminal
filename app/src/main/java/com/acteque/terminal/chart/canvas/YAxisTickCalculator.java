package com.acteque.terminal.chart.canvas;

import java.util.ArrayList;
import java.util.List;

/** Calculates cent-aligned labels for a displayed vertical price range. */
final class YAxisTickCalculator {

  private static final int DEFAULT_INTERVAL_COUNT = 10;
  private static final double CENTS_PER_DOLLAR = 100.0;
  private static final double CENT_ROUNDING_EPSILON = 0.000_000_1;
  private static final long MIN_TICK_SIZE_IN_CENTS = 1L;

  /** Prevents utility-class instantiation. */
  private YAxisTickCalculator() {}

  /**
   * Calculates collision-safe price ticks.
   *
   * @param minPrice the displayed minimum price
   * @param maxPrice the displayed maximum price
   * @param chartHeight the drawable chart height
   * @param zoomScale the current vertical zoom scale
   * @return immutable ascending price ticks, or an empty list for invalid geometry
   */
  static List<Double> calculate(double minPrice, double maxPrice, double chartHeight, double zoomScale) {
    if (
      !Double.isFinite(minPrice) ||
      !Double.isFinite(maxPrice) ||
      !Double.isFinite(chartHeight) ||
      !Double.isFinite(zoomScale) ||
      maxPrice <= minPrice ||
      chartHeight <= 0.0 ||
      zoomScale <= 0.0
    ) {
      return List.of();
    }

    int zoomIntervalCount = Math.max(DEFAULT_INTERVAL_COUNT, (int) Math.ceil(DEFAULT_INTERVAL_COUNT / zoomScale));
    int collisionSafeIntervalCount = YAxisPolicy.maximumYAxisIntervalCount(chartHeight);
    int targetIntervalCount = Math.min(zoomIntervalCount, collisionSafeIntervalCount);

    double priceSpan = maxPrice - minPrice;
    long tickSizeInCents = Math.max(
      MIN_TICK_SIZE_IN_CENTS,
      (long) Math.ceil((priceSpan * CENTS_PER_DOLLAR) / targetIntervalCount - CENT_ROUNDING_EPSILON)
    );
    long firstTickInCents =
      (long) Math.ceil((minPrice * CENTS_PER_DOLLAR) / tickSizeInCents - CENT_ROUNDING_EPSILON) * tickSizeInCents;
    long lastTickInCents =
      (long) Math.floor((maxPrice * CENTS_PER_DOLLAR) / tickSizeInCents + CENT_ROUNDING_EPSILON) * tickSizeInCents;

    List<Double> ticks = new ArrayList<>();
    for (long tickInCents = firstTickInCents; tickInCents <= lastTickInCents; tickInCents += tickSizeInCents) {
      ticks.add(tickInCents / CENTS_PER_DOLLAR);
    }
    return List.copyOf(ticks);
  }
}
