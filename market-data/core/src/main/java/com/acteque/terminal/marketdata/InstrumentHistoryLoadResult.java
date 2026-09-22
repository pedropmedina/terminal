package com.acteque.terminal.marketdata;

import java.util.Objects;

/** Instrument metadata and history loaded together for one interval.
 * @param symbol the normalized instrument symbol
 * @param displayName the name shown in the chart
 * @param details provider-neutral instrument metadata
 * @param history the ordered bars at the requested interval
 */
public record InstrumentHistoryLoadResult(
  String symbol,
  String displayName,
  Instrument details,
  HistoricalPage history
) {
  public InstrumentHistoryLoadResult {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(displayName, "displayName cannot be null");
    Objects.requireNonNull(details, "details cannot be null");
    Objects.requireNonNull(history, "history cannot be null");
  }
}
