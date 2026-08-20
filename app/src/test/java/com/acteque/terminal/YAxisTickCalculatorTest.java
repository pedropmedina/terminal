package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class YAxisTickCalculatorTest {

  @Test
  void usesTenPriceIntervalsAtDefaultZoom() {
    List<Double> ticks = YAxisTickCalculator.calculate(100.0, 120.0, 660.0, 1.0);

    assertEquals(11, ticks.size());
    assertEquals(2.0, ticks.get(1) - ticks.get(0), 0.000_001);
  }

  @Test
  void increasesPriceDetailAsTheVisibleRangeIsZoomedIn() {
    List<Double> defaultZoom = YAxisTickCalculator.calculate(100.0, 120.0, 660.0, 1.0);
    List<Double> zoomedIn = YAxisTickCalculator.calculate(108.0, 112.0, 660.0, 0.2);

    assertTrue(zoomedIn.size() > defaultZoom.size());
  }

  @Test
  void keepsTenPriceIntervalsAtMaximumZoomScale() {
    List<Double> ticks = YAxisTickCalculator.calculate(60.0, 160.0, 660.0, 5.0);

    assertEquals(11, ticks.size());
    assertEquals(10.0, ticks.get(1) - ticks.get(0), 0.000_001);
  }

  @Test
  void neverUsesAnIntervalSmallerThanOneCent() {
    List<Double> ticks = YAxisTickCalculator.calculate(10.000, 10.025, 660.0, 0.2);

    assertEquals(List.of(10.00, 10.01, 10.02), ticks);
  }

  @Test
  void limitsTheVisibleRangeToOneCentPerCollisionSafeInterval() {
    assertEquals(0.20, YAxisTickCalculator.minimumPriceSpan(480.0), 0.000_001);
  }

  @Test
  void toleratesFloatingPointNoiseAtTheOneCentLimit() {
    List<Double> ticks = YAxisTickCalculator.calculate(265.0, 265.200_000_000_000_05, 480.0, 0.001);

    assertEquals(0.01, ticks.get(1) - ticks.get(0), 0.000_001);
  }

  @Test
  void keepsLabelsCollisionSafeOnShortCharts() {
    double chartHeight = 72.0;
    List<Double> ticks = YAxisTickCalculator.calculate(100.0, 101.0, chartHeight, 0.2);

    assertTrue(ticks.size() <= 4);
  }
}
