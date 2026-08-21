package com.acteque.terminal.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Normalized daily data returned by every historical market-data provider. */
public record DailyBar(
  String symbol,
  LocalDate date,
  Ohlcv prices,
  Optional<Ohlcv> adjustedPrices,
  Optional<BigDecimal> cashDividend,
  Optional<BigDecimal> splitFactor
) {
  public DailyBar {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(prices, "prices");
    Objects.requireNonNull(adjustedPrices, "adjustedPrices");
    Objects.requireNonNull(cashDividend, "cashDividend");
    Objects.requireNonNull(splitFactor, "splitFactor");
  }
}
