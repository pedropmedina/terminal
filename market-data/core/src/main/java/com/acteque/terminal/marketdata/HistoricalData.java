package com.acteque.terminal.marketdata;

import java.util.List;

/** Provider-neutral access to historical OHLCV. */
public interface HistoricalData {
  /** Returns OHLCV for the requested calendar interval, ordered from oldest to newest. */
  List<CalendarData> getCalendarData(CalendarRequest request);

  /** Returns timestamped intraday OHLCV ordered from oldest to newest. */
  List<IntradayData> getIntradayData(IntradayRequest request);
}
