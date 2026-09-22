package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.Objects;

/** An ordered history snapshot for exactly one requested interval.
 * @param interval the interval shared by all returned bars
 * @param calendarData calendar bars, empty for intraday pages
 * @param intradayData timestamped bars, empty for calendar pages
 */
public record HistoricalPage(
  HistoricalInterval interval,
  List<CalendarData> calendarData,
  List<IntradayData> intradayData
) {
  public HistoricalPage {
    Objects.requireNonNull(interval, "interval cannot be null");
    calendarData = List.copyOf(calendarData);
    intradayData = List.copyOf(intradayData);
    if (interval instanceof HistoricalInterval.Calendar && !intradayData.isEmpty()) {
      throw new IllegalArgumentException("calendar page cannot contain intraday bars");
    }
    if (interval instanceof HistoricalInterval.Intraday && !calendarData.isEmpty()) {
      throw new IllegalArgumentException("intraday page cannot contain calendar bars");
    }
  }

  /**
   * Creates a calendar snapshot.
   * @param interval the requested calendar period
   * @param bars the ordered calendar bars
   * @return the calendar history page
   */
  public static HistoricalPage calendar(CalendarInterval interval, List<CalendarData> bars) {
    return new HistoricalPage(new HistoricalInterval.Calendar(interval), bars, List.of());
  }

  /**
   * Creates an intraday snapshot.
   * @param interval the requested fixed duration
   * @param bars the ordered timestamped bars
   * @return the intraday history page
   */
  public static HistoricalPage intraday(java.time.Duration interval, List<IntradayData> bars) {
    return new HistoricalPage(new HistoricalInterval.Intraday(interval), List.of(), bars);
  }
}
