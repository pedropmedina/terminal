package com.acteque.terminal.marketdata;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** A provider-neutral request for a symbol's inclusive daily date range. */
public record DailyBarRequest(String symbol, LocalDate startDate, LocalDate endDate) {
  public DailyBarRequest {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(startDate, "startDate");
    Objects.requireNonNull(endDate, "endDate");

    symbol = symbol.strip().toUpperCase(Locale.ROOT);
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not be before startDate");
    }
  }
}
