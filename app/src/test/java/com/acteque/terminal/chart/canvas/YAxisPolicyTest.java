package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class YAxisPolicyTest {

  @Test
  void limitsTheVisibleRangeToOneCentPerCollisionSafeInterval() {
    assertEquals(0.20, YAxisPolicy.minimumPriceSpan(480.0), 0.000_001);
  }

  @Test
  void providesSafeConstraintsForInvalidHeights() {
    assertEquals(1, YAxisPolicy.maximumYAxisIntervalCount(0.0));
    assertEquals(1, YAxisPolicy.maximumYAxisIntervalCount(Double.NaN));
    assertEquals(0.01, YAxisPolicy.minimumPriceSpan(-1.0), 0.000_001);
  }
}
