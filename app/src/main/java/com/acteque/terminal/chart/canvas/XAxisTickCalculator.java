package com.acteque.terminal.chart.canvas;

import java.time.LocalDate;
import java.time.Period;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

final class XAxisTickCalculator {

  private XAxisTickCalculator() {}

  static List<XAxisTick> calculate(
    List<LocalDate> dates,
    int firstVisibleDataIndex,
    int visibleSlotCount,
    double chartWidth,
    double minimumLabelSpacing
  ) {
    return calculate(dates, firstVisibleDataIndex, visibleSlotCount, chartWidth, minimumLabelSpacing, Period.ofDays(1));
  }

  static List<XAxisTick> calculate(
    List<LocalDate> dates,
    int firstVisibleDataIndex,
    int visibleSlotCount,
    double chartWidth,
    double minimumLabelSpacing,
    Period period
  ) {
    if (dates.isEmpty() || visibleSlotCount <= 0 || chartWidth <= 0) {
      return List.of();
    }

    if (visibleSlotCount == 1) {
      LocalDate date = dateAt(dates, firstVisibleDataIndex, period);
      return List.of(tick(firstVisibleDataIndex, firstVisibleDataIndex, date, dates, period));
    }

    double pointSpacing = chartWidth / (visibleSlotCount - 1);
    int minimumSlotSpacing = Math.max(1, (int) Math.ceil(minimumLabelSpacing / pointSpacing));
    boolean showDayDetail = pointSpacing >= minimumLabelSpacing / 2.0;
    int lastVisibleDataIndex = firstVisibleDataIndex + visibleSlotCount - 1;

    TreeSet<Integer> boundaryIndices = new TreeSet<>();
    for (int dataIndex = firstVisibleDataIndex; dataIndex <= lastVisibleDataIndex; dataIndex++) {
      if (isPeriodBoundary(dates, dataIndex, period)) {
        boundaryIndices.add(dataIndex);
      }
    }

    TreeSet<Integer> tickIndices = new TreeSet<>(boundaryIndices);
    if (showDayDetail) {
      for (int dataIndex = firstVisibleDataIndex; dataIndex <= lastVisibleDataIndex; dataIndex += minimumSlotSpacing) {
        if (isFarEnoughFromEvery(dataIndex, tickIndices, minimumSlotSpacing)) {
          tickIndices.add(dataIndex);
        }
      }
    }

    List<XAxisTick> ticks = new ArrayList<>();
    for (int dataIndex : tickIndices) {
      LocalDate date = dateAt(dates, dataIndex, period);
      ticks.add(tick(dataIndex, firstVisibleDataIndex, date, dates, period));
    }
    return List.copyOf(ticks);
  }

  private static boolean isFarEnoughFromEvery(int candidate, TreeSet<Integer> selected, int minimumSpacing) {
    Integer lower = selected.floor(candidate);
    Integer higher = selected.ceiling(candidate);
    return (
      (lower == null || candidate - lower >= minimumSpacing) && (higher == null || higher - candidate >= minimumSpacing)
    );
  }

  private static boolean isPeriodBoundary(List<LocalDate> dates, int dataIndex, Period period) {
    LocalDate date = dateAt(dates, dataIndex, period);
    if (dataIndex == 0) {
      return true;
    }

    LocalDate previousDate = dateAt(dates, dataIndex - 1, period);
    return !YearMonth.from(date).equals(YearMonth.from(previousDate));
  }

  private static XAxisTick tick(
    int dataIndex,
    int firstVisibleDataIndex,
    LocalDate date,
    List<LocalDate> dates,
    Period period
  ) {
    LocalDate previousDate = dataIndex == 0 ? null : dateAt(dates, dataIndex - 1, period);
    String label;
    boolean startsYear =
      (previousDate == null && date.getMonthValue() == 1) ||
      (previousDate != null && date.getYear() != previousDate.getYear());
    if (startsYear) {
      label = ChartDateFormatter.year(date);
    } else if (previousDate == null || !YearMonth.from(date).equals(YearMonth.from(previousDate))) {
      label = ChartDateFormatter.month(date);
    } else {
      label = ChartDateFormatter.day(date);
    }
    return new XAxisTick(dataIndex - firstVisibleDataIndex, dataIndex, label);
  }

  private static LocalDate dateAt(List<LocalDate> dates, int dataIndex, Period period) {
    if (dataIndex < dates.size()) {
      return dates.get(dataIndex);
    }
    return dates.get(dates.size() - 1).plus(period.multipliedBy(dataIndex - dates.size() + 1));
  }

  record XAxisTick(int slotIndex, int dataIndex, String label) {}
}
