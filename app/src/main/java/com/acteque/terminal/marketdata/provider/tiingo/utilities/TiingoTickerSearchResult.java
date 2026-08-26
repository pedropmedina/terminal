package com.acteque.terminal.marketdata.provider.tiingo.utilities;

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
    ticker = Objects.requireNonNull(ticker, "ticker");
    name = Objects.requireNonNull(name, "name");
    assetType = Objects.requireNonNull(assetType, "assetType");
    permaTicker = Objects.requireNonNull(permaTicker, "permaTicker");
    openFigi = Objects.requireNonNull(openFigi, "openFigi");
  }
}
