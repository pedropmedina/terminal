package com.acteque.terminal.marketdata.tiingo.eod;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** Descriptive metadata returned by Tiingo's end-of-day metadata endpoint. */
public record TiingoTickerMetadata(
  String ticker,
  String name,
  String exchangeCode,
  Optional<String> description,
  Optional<LocalDate> startDate,
  Optional<LocalDate> endDate
) {
  public TiingoTickerMetadata {
    ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
    name = Objects.requireNonNull(name, "name cannot be null");
    exchangeCode = Objects.requireNonNull(exchangeCode, "exchangeCode cannot be null");
    description = Objects.requireNonNull(description, "description cannot be null");
    startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
    endDate = Objects.requireNonNull(endDate, "endDate cannot be null");
  }
}
