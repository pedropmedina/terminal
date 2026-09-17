package com.acteque.terminal.marketdata;

import java.util.Objects;
import java.util.Optional;

/**
 * Instrument metadata using a provider-scoped symbol, not a cross-provider identifier.
 * Exchange is a provider-reported label, not necessarily an ISO market identifier code.
 * Unavailable descriptive fields are empty.
 */
public record Instrument(
  String symbol,
  Optional<String> name,
  Optional<String> exchange,
  Optional<String> description
) {
  public Instrument {
    symbol = Objects.requireNonNull(symbol, "symbol cannot be null").strip();
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    name = Objects.requireNonNull(name, "name cannot be null")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
    exchange = Objects.requireNonNull(exchange, "exchange cannot be null")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
    description = Objects.requireNonNull(description, "description cannot be null")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
  }
}
