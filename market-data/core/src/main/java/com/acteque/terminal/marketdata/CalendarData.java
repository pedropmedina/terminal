package com.acteque.terminal.marketdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * Normalized OHLCV for a calendar period. The date is the provider's period label,
 * which need not be a trading date. The request specifies the interval.
 * Adjustments and corporate-action values are optional provider-reported data.
 */
public record CalendarData(
  String symbol,
  LocalDate date,
  Ohlcv prices,
  Optional<Ohlcv> adjustedPrices,
  Optional<BigDecimal> cashDividend,
  Optional<BigDecimal> splitFactor
) {
  public CalendarData {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(date, "date cannot be null");
    Objects.requireNonNull(prices, "prices cannot be null");
    Objects.requireNonNull(adjustedPrices, "adjustedPrices cannot be null");
    Objects.requireNonNull(cashDividend, "cashDividend cannot be null");
    Objects.requireNonNull(splitFactor, "splitFactor cannot be null");
  }
}
