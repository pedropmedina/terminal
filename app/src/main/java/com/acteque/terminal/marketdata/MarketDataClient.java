package com.acteque.terminal.marketdata;

/** Provider-neutral entry point for market-data features. */
public interface MarketDataClient {
  /** Stable, lowercase identifier for the backing provider. */
  String provider();

  /** Returns this client's non-null, reusable historical-bar feature. */
  HistoricalBarData historicalBars();

  /** Returns this client's non-null, reusable instrument-discovery feature. */
  InstrumentDiscovery discovery();
}
