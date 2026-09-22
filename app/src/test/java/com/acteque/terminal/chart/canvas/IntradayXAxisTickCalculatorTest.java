package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class IntradayXAxisTickCalculatorTest {

  @Test
  void labelsMarketTimeAndNewYorkDayBoundaries() {
    List<Instant> bars = List.of(
      Instant.parse("2026-08-20T13:30:00Z"),
      Instant.parse("2026-08-20T13:35:00Z"),
      Instant.parse("2026-08-21T13:30:00Z")
    );
    var ticks = IntradayXAxisTickCalculator.calculate(bars, 0, 3, 600, 56);

    assertEquals(
      List.of("Aug 20", "9:35 AM", "Aug 21"),
      ticks.stream().map(XAxisTickCalculator.XAxisTick::label).toList()
    );
    assertEquals("Aug 20, 2026 9:30 AM", ChartDateFormatter.crosshair(bars.getFirst()));
    assertTrue(ticks.stream().allMatch(tick -> !tick.label().isBlank()));
  }
}
