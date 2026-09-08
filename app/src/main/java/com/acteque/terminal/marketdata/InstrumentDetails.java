package com.acteque.terminal.marketdata;

import java.util.Objects;
import java.util.Optional;

/**
 * Instrument metadata using a provider-scoped symbol, not a cross-provider identifier.
 * Exchange is a provider-reported label, not necessarily an ISO market identifier code.
 * Unavailable descriptive fields are empty.
 */
public record InstrumentDetails(
  String symbol,
  Optional<String> name,
  Optional<String> exchange,
  Optional<String> description,
  Optional<InstrumentLogo> logo
) {
  public InstrumentDetails(
    String symbol,
    Optional<String> name,
    Optional<String> exchange,
    Optional<String> description
  ) {
    this(symbol, name, exchange, description, Optional.empty());
  }

  public InstrumentDetails withLogo(Optional<InstrumentLogo> logo) {
    return new InstrumentDetails(symbol, name, exchange, description, logo);
  }

  public InstrumentDetails {
    Objects.requireNonNull(logo, "logo");
    symbol = Objects.requireNonNull(symbol, "symbol").strip();
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    name = Objects.requireNonNull(name, "name")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
    exchange = Objects.requireNonNull(exchange, "exchange")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
    description = Objects.requireNonNull(description, "description")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
  }
}
