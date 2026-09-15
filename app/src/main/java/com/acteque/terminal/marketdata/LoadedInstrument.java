package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Provider-neutral instrument metadata and price history prepared for display. */
public record LoadedInstrument(String symbol, String displayName, List<DailyBar> bars, InstrumentDetails details) {
  public LoadedInstrument(String symbol, String displayName, List<DailyBar> bars) {
    this(
      symbol,
      displayName,
      bars,
      new InstrumentDetails(symbol, Optional.of(displayName), Optional.empty(), Optional.empty())
    );
  }

  public LoadedInstrument {
    Objects.requireNonNull(details, "details");
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(displayName, "displayName");
    bars = List.copyOf(bars);
  }
}
