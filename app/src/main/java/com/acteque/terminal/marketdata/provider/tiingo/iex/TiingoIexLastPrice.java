package com.acteque.terminal.marketdata.provider.tiingo.iex;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Top-of-book and last-sale snapshot returned by Tiingo's IEX API. */
public record TiingoIexLastPrice(
  String ticker,
  Optional<Instant> timestamp,
  Optional<Instant> quoteTimestamp,
  Optional<Instant> lastSaleTimestamp,
  Optional<BigDecimal> last,
  Optional<BigDecimal> lastSize,
  Optional<BigDecimal> tngoLast,
  Optional<BigDecimal> prevClose,
  Optional<BigDecimal> open,
  Optional<BigDecimal> high,
  Optional<BigDecimal> low,
  Optional<BigDecimal> mid,
  Optional<BigDecimal> volume,
  Optional<BigDecimal> bidPrice,
  Optional<BigDecimal> bidSize,
  Optional<BigDecimal> askPrice,
  Optional<BigDecimal> askSize
) {
  public TiingoIexLastPrice {
    ticker = Objects.requireNonNull(ticker, "ticker");
    timestamp = Objects.requireNonNull(timestamp, "timestamp");
    quoteTimestamp = Objects.requireNonNull(quoteTimestamp, "quoteTimestamp");
    lastSaleTimestamp = Objects.requireNonNull(lastSaleTimestamp, "lastSaleTimestamp");
    last = Objects.requireNonNull(last, "last");
    lastSize = Objects.requireNonNull(lastSize, "lastSize");
    tngoLast = Objects.requireNonNull(tngoLast, "tngoLast");
    prevClose = Objects.requireNonNull(prevClose, "prevClose");
    open = Objects.requireNonNull(open, "open");
    high = Objects.requireNonNull(high, "high");
    low = Objects.requireNonNull(low, "low");
    mid = Objects.requireNonNull(mid, "mid");
    volume = Objects.requireNonNull(volume, "volume");
    bidPrice = Objects.requireNonNull(bidPrice, "bidPrice");
    bidSize = Objects.requireNonNull(bidSize, "bidSize");
    askPrice = Objects.requireNonNull(askPrice, "askPrice");
    askSize = Objects.requireNonNull(askSize, "askSize");
  }
}
