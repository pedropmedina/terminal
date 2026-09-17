package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.Ohlcv;
import java.time.LocalDate;
import java.util.Objects;

/** Drawing-friendly projection of a provider-neutral market-data bar. */
public record PricePoint(LocalDate date, double open, double high, double low, double close, long volume) {
  public static PricePoint from(CalendarData bar) {
    Objects.requireNonNull(bar, "bar cannot be null");
    Ohlcv prices = bar.prices();
    return new PricePoint(
      bar.date(),
      prices.open().doubleValue(),
      prices.high().doubleValue(),
      prices.low().doubleValue(),
      prices.close().doubleValue(),
      prices.volume().longValueExact()
    );
  }

  public double price() {
    return close;
  }
}
