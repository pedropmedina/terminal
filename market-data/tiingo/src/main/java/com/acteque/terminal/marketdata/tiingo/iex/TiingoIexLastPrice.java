package com.acteque.terminal.marketdata.tiingo.iex;

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
    ticker = Objects.requireNonNull(ticker, "ticker cannot be null");
    timestamp = Objects.requireNonNull(timestamp, "timestamp cannot be null");
    quoteTimestamp = Objects.requireNonNull(quoteTimestamp, "quoteTimestamp cannot be null");
    lastSaleTimestamp = Objects.requireNonNull(lastSaleTimestamp, "lastSaleTimestamp cannot be null");
    last = Objects.requireNonNull(last, "last cannot be null");
    lastSize = Objects.requireNonNull(lastSize, "lastSize cannot be null");
    tngoLast = Objects.requireNonNull(tngoLast, "tngoLast cannot be null");
    prevClose = Objects.requireNonNull(prevClose, "prevClose cannot be null");
    open = Objects.requireNonNull(open, "open cannot be null");
    high = Objects.requireNonNull(high, "high cannot be null");
    low = Objects.requireNonNull(low, "low cannot be null");
    mid = Objects.requireNonNull(mid, "mid cannot be null");
    volume = Objects.requireNonNull(volume, "volume cannot be null");
    bidPrice = Objects.requireNonNull(bidPrice, "bidPrice cannot be null");
    bidSize = Objects.requireNonNull(bidSize, "bidSize cannot be null");
    askPrice = Objects.requireNonNull(askPrice, "askPrice cannot be null");
    askSize = Objects.requireNonNull(askSize, "askSize cannot be null");
  }
}
