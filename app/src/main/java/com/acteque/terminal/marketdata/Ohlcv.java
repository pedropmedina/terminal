package com.acteque.terminal.marketdata;

import java.math.BigDecimal;
import java.util.Objects;

/** Open, high, low, close, and volume values with decimal precision preserved. */
public record Ohlcv(BigDecimal open, BigDecimal high, BigDecimal low, BigDecimal close, BigDecimal volume) {
  public Ohlcv {
    Objects.requireNonNull(open, "open");
    Objects.requireNonNull(high, "high");
    Objects.requireNonNull(low, "low");
    Objects.requireNonNull(close, "close");
    Objects.requireNonNull(volume, "volume");

    if (volume.signum() < 0) {
      throw new IllegalArgumentException("volume must not be negative");
    }
  }
}
