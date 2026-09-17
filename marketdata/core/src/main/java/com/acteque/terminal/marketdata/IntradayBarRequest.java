package com.acteque.terminal.marketdata;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** A provider-neutral request for timestamped intraday bars in an inclusive date range. */
public record IntradayBarRequest(
  String symbol,
  LocalDate startDate,
  LocalDate endDate,
  Duration interval,
  boolean includeAfterHours,
  boolean forceFill
) {
  public IntradayBarRequest(String symbol, LocalDate startDate, LocalDate endDate, Duration interval) {
    this(symbol, startDate, endDate, interval, false, false);
  }

  public IntradayBarRequest {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(startDate, "startDate");
    Objects.requireNonNull(endDate, "endDate");
    Objects.requireNonNull(interval, "interval");

    symbol = symbol.strip().toUpperCase(Locale.ROOT);
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not be before startDate");
    }
    if (interval.isNegative() || interval.isZero()) {
      throw new IllegalArgumentException("interval must be positive");
    }
    if (interval.compareTo(Duration.ofDays(1)) >= 0) {
      throw new IllegalArgumentException("intraday interval must be shorter than one day");
    }
    if (interval.toSecondsPart() != 0 || interval.toNanosPart() != 0) {
      throw new IllegalArgumentException("interval must be a whole number of minutes");
    }
  }
}
