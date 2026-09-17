package com.acteque.terminal.marketdata;

import java.time.Instant;
import java.util.Objects;

/** A timestamped intraday OHLCV bar. */
public record IntradayBar(String symbol, Instant timestamp, Ohlcv prices) {
  public IntradayBar {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(timestamp, "timestamp");
    Objects.requireNonNull(prices, "prices");
  }
}
