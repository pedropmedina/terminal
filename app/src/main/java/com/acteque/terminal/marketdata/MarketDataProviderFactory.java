package com.acteque.terminal.marketdata;

import java.util.function.Function;

/** Construction port implemented by each provider integration. */
public interface MarketDataProviderFactory {
  /** Stable lowercase identifier, matching the clients this factory creates. */
  String provider();

  /**
   * Creates a fresh client. The lookup supplies provider-specific configuration by key;
   * missing keys return null. Implementations validate required values without logging secrets.
   * The returned client is owned by the caller and must not be cached by the factory.
   */
  MarketDataClient create(Function<String, String> configuration);
}
