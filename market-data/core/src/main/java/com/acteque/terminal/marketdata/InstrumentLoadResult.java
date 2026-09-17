package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Provider-neutral instrument metadata and price history prepared for display. */
public record InstrumentLoadResult(
  String symbol,
  String displayName,
  List<CalendarData> calendarData,
  Instrument details
) {
  public InstrumentLoadResult(String symbol, String displayName, List<CalendarData> calendarData) {
    this(
      symbol,
      displayName,
      calendarData,
      new Instrument(symbol, Optional.of(displayName), Optional.empty(), Optional.empty())
    );
  }

  public InstrumentLoadResult {
    Objects.requireNonNull(details, "details cannot be null");
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(displayName, "displayName cannot be null");
    calendarData = List.copyOf(calendarData);
  }
}
