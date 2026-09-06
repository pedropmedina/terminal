package com.acteque.terminal.marketdata;

import java.util.List;

/** Provider-neutral access to historical OHLCV bars. */
public interface HistoricalBarData {
  /** Returns end-of-day bars ordered from oldest to newest. */
  List<DailyBar> getDailyBars(DailyBarRequest request);

  /** Returns timestamped intraday bars ordered from oldest to newest. */
  List<IntradayBar> getIntradayBars(IntradayBarRequest request);
}
