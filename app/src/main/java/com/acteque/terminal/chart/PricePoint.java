package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.IntradayData;
import com.acteque.terminal.marketdata.Ohlcv;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Optional;

/**
 * Drawing-friendly projection of a provider-neutral market-data bar.
 *
 * @param date the calendar label or New York date of a timestamped bar
 * @param timestamp the original instant for an intraday bar, or empty for a calendar bar
 * @param open the opening price for drawing
 * @param high the high price for drawing
 * @param low the low price for drawing
 * @param close the closing price for drawing
 * @param volume the traded volume
 */
public record PricePoint(
  LocalDate date,
  Optional<Instant> timestamp,
  double open,
  double high,
  double low,
  double close,
  long volume
) {
  private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

  /**
   * Creates a drawing point from a calendar bar without an intraday timestamp.
   *
   * @param date the calendar label
   * @param open the opening price
   * @param high the high price
   * @param low the low price
   * @param close the closing price
   * @param volume the traded volume
   */
  public PricePoint(LocalDate date, double open, double high, double low, double close, long volume) {
    this(date, Optional.empty(), open, high, low, close, volume);
  }

  public PricePoint {
    Objects.requireNonNull(date, "date cannot be null");
    Objects.requireNonNull(timestamp, "timestamp cannot be null");
  }

  /**
   * Converts a provider-neutral calendar bar into a drawing point.
   *
   * @param bar the normalized calendar bar
   * @return its drawing point without an intraday timestamp
   */
  public static PricePoint from(CalendarData bar) {
    Objects.requireNonNull(bar, "bar cannot be null");
    Ohlcv prices = bar.prices();
    return new PricePoint(
      bar.date(),
      Optional.empty(),
      prices.open().doubleValue(),
      prices.high().doubleValue(),
      prices.low().doubleValue(),
      prices.close().doubleValue(),
      prices.volume().longValueExact()
    );
  }

  /**
   * Converts a timestamped provider bar without discarding its time.
   *
   * @param bar the normalized intraday bar
   * @return its drawing point with an original timestamp
   */
  public static PricePoint from(IntradayData bar) {
    Objects.requireNonNull(bar, "bar cannot be null");
    Ohlcv prices = bar.prices();
    return new PricePoint(
      bar.timestamp().atZone(MARKET_ZONE).toLocalDate(),
      Optional.of(bar.timestamp()),
      prices.open().doubleValue(),
      prices.high().doubleValue(),
      prices.low().doubleValue(),
      prices.close().doubleValue(),
      prices.volume().longValueExact()
    );
  }

  /**
   * Returns the representative price used by line-based renderers.
   *
   * @return the closing price
   */
  public double price() {
    return close;
  }
}
