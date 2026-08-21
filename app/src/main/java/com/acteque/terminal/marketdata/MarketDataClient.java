package com.acteque.terminal.marketdata;

import java.util.List;

/** Provider-neutral contract for retrieving historical market data. */
public interface MarketDataClient {
  /** Stable, lowercase identifier for the backing provider. */
  String provider();

  /** Returns end-of-day bars ordered from oldest to newest. */
  List<DailyBar> getDailyBars(DailyBarRequest request);

  /** Returns timestamped intraday bars ordered from oldest to newest. */
  List<IntradayBar> getIntradayBars(IntradayBarRequest request);
}
