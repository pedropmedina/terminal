package com.acteque.terminal.marketdata.provider.tiingo.tickercatalog;

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
    ticker = Objects.requireNonNull(ticker, "ticker");
    exchange = Objects.requireNonNull(exchange, "exchange");
    assetType = Objects.requireNonNull(assetType, "assetType");
    priceCurrency = Objects.requireNonNull(priceCurrency, "priceCurrency");
    startDate = Objects.requireNonNull(startDate, "startDate");
    endDate = Objects.requireNonNull(endDate, "endDate");
  }
}
