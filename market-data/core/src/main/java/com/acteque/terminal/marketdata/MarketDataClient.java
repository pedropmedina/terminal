package com.acteque.terminal.marketdata;

import java.util.Optional;

/** Provider-neutral capabilities. Instances may be shared by independent sessions. */
public interface MarketDataClient extends AutoCloseable {
  /** Stable, lowercase identifier for the backing provider. */
  String provider();

  /** Empty means unsupported, not supported with no results. Present capabilities are reusable. */
  default Optional<HistoricalData> historical() {
    return Optional.empty();
  }

  default Optional<InstrumentCatalog> catalog() {
    return Optional.empty();
  }

  /** Called by the provider owner after all sessions close. Must be idempotent. */
  @Override
  default void close() {}
}
