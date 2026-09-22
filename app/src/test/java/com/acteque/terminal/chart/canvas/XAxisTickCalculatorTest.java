package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.chart.canvas.XAxisTickCalculator.XAxisTick;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import org.junit.jupiter.api.Test;

class XAxisTickCalculatorTest {

  @Test
  void projectsFutureMonthlySlotsByCalendarMonths() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(
      List.of(LocalDate.of(2026, 1, 1), LocalDate.of(2026, 2, 1)),
      0,
      4,
      660.0,
      LABEL_SPACING,
      Period.ofMonths(1)
    );

    assertEquals(List.of("2026", "Feb", "Mar", "Apr"), ticks.stream().map(XAxisTick::label).toList());
  }

  private static final double LABEL_SPACING = 56.0;
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
    List<XAxisTick> zoomedOut = XAxisTickCalculator.calculate(dates, 0, 100, 660.0, LABEL_SPACING);
    List<XAxisTick> zoomedIn = XAxisTickCalculator.calculate(dates, 88, 12, 660.0, LABEL_SPACING);

    assertTrue(zoomedIn.size() > zoomedOut.size());
    assertEquals(12, zoomedIn.size());
  }

  @Test
  void displaysEveryTradingSessionAtMaximumZoom() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(tradingDates(100), 92, 8, 660.0, LABEL_SPACING);

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
      LABEL_SPACING
    );
    double pointSpacing = chartWidth / (visibleSlots - 1);

    for (int index = 1; index < ticks.size(); index++) {
      int slotDifference = ticks.get(index).slotIndex() - ticks.get(index - 1).slotIndex();
      assertTrue(slotDifference * pointSpacing >= LABEL_SPACING);
    }
  }

  @Test
  void alignsTicksToDataIndicesWhilePanning() {
    List<LocalDate> dates = tradingDates(100);
    List<XAxisTick> firstWindow = XAxisTickCalculator.calculate(dates, 40, 20, 660.0, LABEL_SPACING);
    List<XAxisTick> pannedWindow = XAxisTickCalculator.calculate(dates, 41, 20, 660.0, LABEL_SPACING);

    List<Integer> firstIndices = firstWindow.stream().map(XAxisTick::dataIndex).toList();
    List<Integer> pannedIndices = pannedWindow.stream().map(XAxisTick::dataIndex).toList();
    assertTrue(firstIndices.stream().anyMatch(pannedIndices::contains));
  }

  @Test
  void plansUnlabelledSlotsBeyondTheNewestPoint() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(tradingDates(100), 94, 10, 660.0, LABEL_SPACING);

    assertTrue(ticks.stream().anyMatch(tick -> tick.dataIndex() >= 100));
    assertFalse(ticks.isEmpty());
  }

  @Test
  void showsMonthNamesAtTheFirstTradingSessionWhenZoomedOut() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(TRADING_DATES, 0, 8, 100.0, LABEL_SPACING);

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
      LABEL_SPACING
    );

    assertEquals(List.of("May"), ticks.stream().map(XAxisTick::label).toList());
  }

  @Test
  void showsDayNumbersAndKeepsPeriodBoundariesWhenZoomedIn() {
    List<XAxisTick> ticks = XAxisTickCalculator.calculate(TRADING_DATES, 0, 8, 660.0, LABEL_SPACING);

    assertEquals(
      List.of("Dec", "31", "2026", "5", "6", "Feb", "3", "4"),
      ticks.stream().map(XAxisTick::label).toList()
    );
  }

  @Test
  void formatsCrosshairLabelsWithWeekdayAndFullDate() {
    assertEquals("Thu Jul 09, 2026", ChartDateFormatter.crosshair(LocalDate.of(2026, 7, 9)));
  }

  private static List<LocalDate> tradingDates(int count) {
    return java.util.stream.IntStream.range(0, count)
      .mapToObj(index -> LocalDate.of(2026, 1, 2).plusDays(index))
      .toList();
  }
}
