package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.XAxisTickCalculator.XAxisTick;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class XAxisTickCalculatorTest {

  @Test
  void increasesDailyDetailAsTheVisibleWindowShrinks() {
    List<XAxisTick> zoomedOut = XAxisTickCalculator.calculate(100, 0, 100, 660.0, ChartInterval.DAILY);
    List<XAxisTick> zoomedIn = XAxisTickCalculator.calculate(100, 88, 12, 660.0, ChartInterval.DAILY);

    assertTrue(zoomedIn.size() > zoomedOut.size());
    assertEquals(12, zoomedIn.size());
  }

  @Test
  void displaysEveryTradingSessionAtMaximumZoom() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(100, 92, 8, 660.0, ChartInterval.DAILY);

    assertEquals(8, ticks.size());
    assertEquals(List.of(92, 93, 94, 95, 96, 97, 98, 99), ticks.stream().map(XAxisTick::dataIndex).toList());
  }

  @Test
  void keepsZoomedOutLabelsCollisionSafe() {
    int visibleSlots = 100;
    double chartWidth = 660.0;
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(100, 0, visibleSlots, chartWidth, ChartInterval.DAILY);
    double pointSpacing = chartWidth / (visibleSlots - 1);

    for (int index = 1; index < ticks.size(); index++) {
      int slotDifference = ticks.get(index).slotIndex() - ticks.get(index - 1).slotIndex();
      assertTrue(slotDifference * pointSpacing >= ChartInterval.DAILY.minimumLabelSpacing());
    }
  }

  @Test
  void alignsTicksToDataIndicesWhilePanning() {
    List<XAxisTick> firstWindow = XAxisTickCalculator.calculate(100, 40, 20, 660.0, ChartInterval.DAILY);
    List<XAxisTick> pannedWindow = XAxisTickCalculator.calculate(100, 41, 20, 660.0, ChartInterval.DAILY);

    List<Integer> firstIndices = firstWindow.stream().map(XAxisTick::dataIndex).toList();
    List<Integer> pannedIndices = pannedWindow.stream().map(XAxisTick::dataIndex).toList();
    assertTrue(firstIndices.stream().anyMatch(pannedIndices::contains));
  }

  @Test
  void plansUnlabelledSlotsBeyondTheNewestPoint() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(100, 94, 10, 660.0, ChartInterval.DAILY);

    assertTrue(ticks.stream().anyMatch(tick -> tick.dataIndex() >= 100));
    assertFalse(ticks.isEmpty());
  }

  @Test
  void formatsDailyLabels() {
    assertEquals("07/16", ChartInterval.DAILY.format(LocalDate.of(2026, 7, 16)));
  }
}
