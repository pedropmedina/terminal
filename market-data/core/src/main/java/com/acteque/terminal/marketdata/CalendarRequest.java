package com.acteque.terminal.marketdata;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** A provider-neutral request for calendar OHLCV in an inclusive date range. */
public record CalendarRequest(String symbol, LocalDate startDate, LocalDate endDate, CalendarInterval interval) {
  public CalendarRequest(String symbol, LocalDate startDate, LocalDate endDate) {
    this(symbol, startDate, endDate, CalendarInterval.DAILY);
  }

  public CalendarRequest {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(startDate, "startDate cannot be null");
    Objects.requireNonNull(endDate, "endDate cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");

    symbol = symbol.strip().toUpperCase(Locale.ROOT);
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not be before startDate");
    }
  }
}
