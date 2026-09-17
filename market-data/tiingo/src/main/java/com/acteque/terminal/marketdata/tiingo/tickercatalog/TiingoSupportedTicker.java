package com.acteque.terminal.marketdata.tiingo.tickercatalog;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/** A row in Tiingo's daily supported-ticker catalog. */
public record TiingoSupportedTicker(
  String ticker,
  String exchange,
  String assetType,
  String priceCurrency,
  Optional<LocalDate> startDate,
  Optional<LocalDate> endDate
) {
  public TiingoSupportedTicker {
    ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
    exchange = Objects.requireNonNull(exchange, "exchange cannot be null");
    assetType = Objects.requireNonNull(assetType, "assetType cannot be null");
    priceCurrency = Objects.requireNonNull(priceCurrency, "priceCurrency cannot be null");
    startDate = Objects.requireNonNull(startDate, "startDate cannot be null");
    endDate = Objects.requireNonNull(endDate, "endDate cannot be null");
  }
}
