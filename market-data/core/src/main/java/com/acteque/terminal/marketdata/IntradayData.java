package com.acteque.terminal.marketdata;

import java.time.Instant;
import java.util.Objects;

/** Normalized timestamped intraday OHLCV. */
public record IntradayData(String symbol, Instant timestamp, Ohlcv prices) {
  public IntradayData {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(timestamp, "timestamp cannot be null");
    Objects.requireNonNull(prices, "prices cannot be null");
  }
}
