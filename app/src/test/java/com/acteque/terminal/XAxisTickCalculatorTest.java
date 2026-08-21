package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.XAxisTickCalculator.XAxisTick;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class XAxisTickCalculatorTest {

  private static final List<LocalDate> TRADING_DATES = List.of(
    LocalDate.of(2025, 12, 30),
    LocalDate.of(2025, 12, 31),
    LocalDate.of(2026, 1, 2),
    LocalDate.of(2026, 1, 5),
    LocalDate.of(2026, 1, 6),
    LocalDate.of(2026, 2, 2),
    LocalDate.of(2026, 2, 3),
    LocalDate.of(2026, 2, 4)
  );

  @Test
  void increasesDailyDetailAsTheVisibleWindowShrinks() {
    List<LocalDate> dates = tradingDates(100);
    List<XAxisTick> zoomedOut = XAxisTickCalculator.calculate(dates, 0, 100, 660.0, ChartInterval.DAILY);
    List<XAxisTick> zoomedIn = XAxisTickCalculator.calculate(dates, 88, 12, 660.0, ChartInterval.DAILY);

    assertTrue(zoomedIn.size() > zoomedOut.size());
    assertEquals(12, zoomedIn.size());
  }

  @Test
  void displaysEveryTradingSessionAtMaximumZoom() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(tradingDates(100), 92, 8, 660.0, ChartInterval.DAILY);

    assertEquals(8, ticks.size());
    assertEquals(List.of(92, 93, 94, 95, 96, 97, 98, 99), ticks.stream().map(XAxisTick::dataIndex).toList());
  }

  @Test
  void keepsZoomedOutLabelsCollisionSafe() {
    int visibleSlots = 100;
    double chartWidth = 660.0;
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(
      tradingDates(100),
      0,
      visibleSlots,
      chartWidth,
      ChartInterval.DAILY
    );
    double pointSpacing = chartWidth / (visibleSlots - 1);

    for (int index = 1; index < ticks.size(); index++) {
      int slotDifference = ticks.get(index).slotIndex() - ticks.get(index - 1).slotIndex();
      assertTrue(slotDifference * pointSpacing >= ChartInterval.DAILY.minimumLabelSpacing());
    }
  }

  @Test
  void alignsTicksToDataIndicesWhilePanning() {
    List<LocalDate> dates = tradingDates(100);
    List<XAxisTick> firstWindow = XAxisTickCalculator.calculate(dates, 40, 20, 660.0, ChartInterval.DAILY);
    List<XAxisTick> pannedWindow = XAxisTickCalculator.calculate(dates, 41, 20, 660.0, ChartInterval.DAILY);

    List<Integer> firstIndices = firstWindow.stream().map(XAxisTick::dataIndex).toList();
    List<Integer> pannedIndices = pannedWindow.stream().map(XAxisTick::dataIndex).toList();
    assertTrue(firstIndices.stream().anyMatch(pannedIndices::contains));
  }

  @Test
  void plansUnlabelledSlotsBeyondTheNewestPoint() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(tradingDates(100), 94, 10, 660.0, ChartInterval.DAILY);

    assertTrue(ticks.stream().anyMatch(tick -> tick.dataIndex() >= 100));
    assertFalse(ticks.isEmpty());
  }

  @Test
  void showsMonthNamesAtTheFirstTradingSessionWhenZoomedOut() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(TRADING_DATES, 0, 8, 100.0, ChartInterval.DAILY);

    assertEquals(List.of("Dec", "2026", "Feb"), ticks.stream().map(XAxisTick::label).toList());
    assertEquals(List.of(0, 2, 5), ticks.stream().map(XAxisTick::dataIndex).toList());
  }

  @Test
  void labelsAHistoryStartingMidyearWithItsMonth() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(
      List.of(LocalDate.of(2026, 5, 4), LocalDate.of(2026, 5, 5)),
      0,
      2,
      20.0,
      ChartInterval.DAILY
    );

    assertEquals(List.of("May"), ticks.stream().map(XAxisTick::label).toList());
  }

  @Test
  void showsDayNumbersAndKeepsPeriodBoundariesWhenZoomedIn() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(TRADING_DATES, 0, 8, 660.0, ChartInterval.DAILY);

    assertEquals(
      List.of("Dec", "31", "2026", "5", "6", "Feb", "3", "4"),
      ticks.stream().map(XAxisTick::label).toList()
    );
  }

  @Test
  void formatsCrosshairLabelsWithWeekdayAndFullDate() {
    assertEquals("Thu Jul 09, 2026", ChartInterval.DAILY.formatCrosshair(LocalDate.of(2026, 7, 9)));
  }

  private static List<LocalDate> tradingDates(int count) {
    return java.util.stream.IntStream.range(0, count)
      .mapToObj(index -> LocalDate.of(2026, 1, 2).plusDays(index))
      .toList();
  }
}
