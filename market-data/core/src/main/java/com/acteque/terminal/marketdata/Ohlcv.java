package com.acteque.terminal.marketdata;

import java.math.BigDecimal;
import java.util.Objects;

/** Open, high, low, close, and volume values with decimal precision preserved. */
public record Ohlcv(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
  public Ohlcv {
    Objects.requireNonNull(open, "open cannot be null");
    Objects.requireNonNull(high, "high cannot be null");
    Objects.requireNonNull(low, "low cannot be null");
    Objects.requireNonNull(close, "close cannot be null");
    Objects.requireNonNull(volume, "volume cannot be null");

    if (volume.signum() < 0) {
      throw new IllegalArgumentException("volume must not be negative");
    }
  }
}
