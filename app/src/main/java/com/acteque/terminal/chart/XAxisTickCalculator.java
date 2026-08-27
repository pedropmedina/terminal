package com.acteque.terminal.chart;

import java.time.LocalDate;
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
    ChartInterval interval
  ) {
    if (dates.isEmpty() || visibleSlotCount <= 0 || chartWidth <= 0) {
      return List.of();
    }

    if (visibleSlotCount == 1) {
      LocalDate date = dateAt(dates, firstVisibleDataIndex);
      return List.of(tick(firstVisibleDataIndex, firstVisibleDataIndex, date, dates, interval));
    }

    double pointSpacing = chartWidth / (visibleSlotCount - 1);
    int minimumSlotSpacing = Math.max(1, (int) Math.ceil(interval.minimumLabelSpacing() / pointSpacing));
    boolean showDayDetail = pointSpacing >= interval.minimumLabelSpacing() / 2.0;
    int lastVisibleDataIndex = firstVisibleDataIndex + visibleSlotCount - 1;

    TreeSet<Integer> boundaryIndices = new TreeSet<>();
    for (int dataIndex = firstVisibleDataIndex; dataIndex <= lastVisibleDataIndex; dataIndex++) {
      if (isPeriodBoundary(dates, dataIndex)) {
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
      LocalDate date = dateAt(dates, dataIndex);
      ticks.add(tick(dataIndex, firstVisibleDataIndex, date, dates, interval));
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

  private static boolean isPeriodBoundary(List<LocalDate> dates, int dataIndex) {
    LocalDate date = dateAt(dates, dataIndex);
    if (dataIndex == 0) {
      return true;
    }

    LocalDate previousDate = dateAt(dates, dataIndex - 1);
    return !YearMonth.from(date).equals(YearMonth.from(previousDate));
  }

  private static XAxisTick tick(
    int dataIndex,
    int firstVisibleDataIndex,
    LocalDate date,
    List<LocalDate> dates,
    ChartInterval interval
  ) {
    LocalDate previousDate = dataIndex == 0 ? null : dateAt(dates, dataIndex - 1);
    String label;
    boolean startsYear =
      (previousDate == null && date.getMonthValue() == 1) ||
      (previousDate != null && date.getYear() != previousDate.getYear());
    if (startsYear) {
      label = interval.formatYear(date);
    } else if (previousDate == null || !YearMonth.from(date).equals(YearMonth.from(previousDate))) {
      label = interval.formatMonth(date);
    } else {
      label = interval.formatDay(date);
    }
    return new XAxisTick(dataIndex - firstVisibleDataIndex, dataIndex, label);
  }

  private static LocalDate dateAt(List<LocalDate> dates, int dataIndex) {
    if (dataIndex < dates.size()) {
      return dates.get(dataIndex);
    }
    return dates.get(dates.size() - 1).plusDays(dataIndex - dates.size() + 1L);
  }

  record XAxisTick(int slotIndex, int dataIndex, String label) {}
}
