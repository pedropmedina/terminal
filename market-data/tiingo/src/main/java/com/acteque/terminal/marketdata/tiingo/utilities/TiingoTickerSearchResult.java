package com.acteque.terminal.marketdata.tiingo.utilities;

import java.util.Objects;
import java.util.Optional;

/** A match returned by Tiingo's ticker search utility. */
public record TiingoTickerSearchResult(
  String ticker,
  String name,
  String assetType,
  boolean active,
  Optional<String> permaTicker,
  Optional<String> openFigi
) {
  public TiingoTickerSearchResult {
    ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
    name = Objects.requireNonNull(name, "name cannot be null");
    assetType = Objects.requireNonNull(assetType, "assetType cannot be null");
    permaTicker = Objects.requireNonNull(permaTicker, "permaTicker cannot be null");
    openFigi = Objects.requireNonNull(openFigi, "openFigi cannot be null");
  }
}
