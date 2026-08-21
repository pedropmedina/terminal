package com.acteque.terminal.marketdata.provider.tiingo;

/** Tiingo equity feeds that expose historical intraday bars. */
public enum TiingoIntradayFeed {
  /** Production-oriented IEX feed recommended by Tiingo. */
  IEX("/iex/"),

  /** Consolidated multi-venue equity feed, currently marked beta by Tiingo. */
  CONSOLIDATED("/tiingo/equity/intraday/");

  private final String pathPrefix;

  TiingoIntradayFeed(String pathPrefix) {
    this.pathPrefix = pathPrefix;
  }

  String pathPrefix() {
    return pathPrefix;
  }
}
