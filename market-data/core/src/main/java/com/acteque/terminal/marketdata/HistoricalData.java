package com.acteque.terminal.marketdata;

import java.time.Duration;
import java.util.List;

/** Provider-neutral access to historical OHLCV. */
public interface HistoricalData {
  /**
   * Reports whether this provider can request bars for a calendar period.
   *
   * @param interval the calendar period to inspect
   * @return true when the provider accepts the period
   */
  default boolean supports(CalendarInterval interval) {
    return interval == CalendarInterval.DAILY;
  }

  /**
   * Reports whether this provider can request bars for a fixed intraday duration.
   *
   * @param interval the fixed duration to inspect
   * @return true when the provider accepts the duration
   */
  default boolean supports(Duration interval) {
    return false;
  }

  /** Returns OHLCV for the requested calendar interval, ordered from oldest to newest. */
  List<CalendarData> getCalendarData(CalendarRequest request);

  /** Returns timestamped intraday OHLCV ordered from oldest to newest. */
  List<IntradayData> getIntradayData(IntradayRequest request);
}
