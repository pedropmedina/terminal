package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.chart.ChartInterval;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CrosshairRendererTest {

  private final CrosshairRenderer crosshair = new CrosshairRenderer(ChartInterval.DAILY);

  @Test
  void formatsThePriceBadge() {
    assertEquals("1234.50", crosshair.priceText(1_234.5));
  }

  @Test
  void formatsTheDateBadgeForTheChartInterval() {
    assertEquals("Mon Aug 24, 2026", crosshair.dateText(LocalDate.of(2026, 8, 24)));
  }
}
