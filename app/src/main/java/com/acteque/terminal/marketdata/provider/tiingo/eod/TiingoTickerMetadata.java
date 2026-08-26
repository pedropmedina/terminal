package com.acteque.terminal.marketdata.provider.tiingo.eod;

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
    ticker = Objects.requireNonNull(ticker, "ticker");
    name = Objects.requireNonNull(name, "name");
    exchangeCode = Objects.requireNonNull(exchangeCode, "exchangeCode");
    description = Objects.requireNonNull(description, "description");
    startDate = Objects.requireNonNull(startDate, "startDate");
    endDate = Objects.requireNonNull(endDate, "endDate");
  }
}
