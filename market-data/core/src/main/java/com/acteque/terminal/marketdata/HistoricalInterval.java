package com.acteque.terminal.marketdata;

import java.time.Duration;
import java.util.Objects;

/** A calendar period or fixed intraday duration requested from historical data. */
public sealed interface HistoricalInterval permits HistoricalInterval.Calendar, HistoricalInterval.Intraday {
  /** A calendar period whose bars carry dates.
   * @param value the requested calendar period
   */
  record Calendar(CalendarInterval value) implements HistoricalInterval {
    public Calendar {
      Objects.requireNonNull(value, "value cannot be null");
    }
  }

  /** A fixed duration whose bars carry timestamps.
   * @param value the requested elapsed duration
   */
  record Intraday(Duration value) implements HistoricalInterval {
    public Intraday {
      Objects.requireNonNull(value, "value cannot be null");
    }
  }
}
