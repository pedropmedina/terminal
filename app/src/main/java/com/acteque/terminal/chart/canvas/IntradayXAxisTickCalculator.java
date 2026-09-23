package com.acteque.terminal.chart.canvas;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

/** Chooses spaced intraday time labels with dates at trading-day boundaries. */
final class IntradayXAxisTickCalculator {

  private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");
  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm a", Locale.US);
  private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MMM d", Locale.US);

  /** Prevents utility-class instantiation. */
  private IntradayXAxisTickCalculator() {}

  /**
   * Calculates visible intraday labels without collisions.
   *
   * @param times complete ordered bar timestamps
   * @param firstVisibleDataIndex source index of the first visible bar
   * @param visibleSlotCount number of visible and projected slots
   * @param chartWidth drawable chart width
   * @param minimumLabelSpacing minimum desired label spacing
   * @return immutable axis ticks
   */
  static List<XAxisTickCalculator.XAxisTick> calculate(
    List<Instant> times,
    int firstVisibleDataIndex,
    int visibleSlotCount,
    double chartWidth,
    double minimumLabelSpacing
  ) {
    if (times.isEmpty() || firstVisibleDataIndex >= times.size() || visibleSlotCount <= 0 || chartWidth <= 0) {
      return List.of();
    }
    int minimumSlotSpacing = Math.max(
      1,
      (int) Math.ceil(minimumLabelSpacing / (chartWidth / Math.max(1, visibleSlotCount - 1)))
    );
    int lastExclusive = Math.min(times.size(), firstVisibleDataIndex + visibleSlotCount);
    TreeSet<Integer> selected = new TreeSet<>();
    selected.add(firstVisibleDataIndex);
    for (int index = firstVisibleDataIndex + 1; index < lastExclusive; index++) {
      if (startsDay(times, index) && farEnough(index, selected, minimumSlotSpacing)) {
        selected.add(index);
      }
    }
    for (int index = firstVisibleDataIndex; index < lastExclusive; index++) {
      if (farEnough(index, selected, minimumSlotSpacing)) {
        selected.add(index);
      }
    }
    List<XAxisTickCalculator.XAxisTick> result = new ArrayList<>();
    for (int index : selected) {
      Instant time = times.get(index);
      String label = time
        .atZone(MARKET_ZONE)
        .format(index == firstVisibleDataIndex || startsDay(times, index) ? DATE : TIME);
      result.add(new XAxisTickCalculator.XAxisTick(index - firstVisibleDataIndex, index, label));
    }
    return List.copyOf(result);
  }

  /** @return true when the indexed timestamp starts a new New York trading date */
  private static boolean startsDay(List<Instant> times, int index) {
    return !times
      .get(index)
      .atZone(MARKET_ZONE)
      .toLocalDate()
      .equals(
        times
          .get(index - 1)
          .atZone(MARKET_ZONE)
          .toLocalDate()
      );
  }

  /** @return true when a candidate is sufficiently far from selected indices */
  private static boolean farEnough(int candidate, TreeSet<Integer> selected, int minimumSpacing) {
    Integer lower = selected.floor(candidate);
    Integer higher = selected.ceiling(candidate);
    return (
      (lower == null || candidate - lower >= minimumSpacing) && (higher == null || higher - candidate >= minimumSpacing)
    );
  }
}
