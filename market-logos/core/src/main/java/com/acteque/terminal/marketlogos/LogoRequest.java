package com.acteque.terminal.marketlogos;

import java.util.Objects;
import java.util.Optional;

/**
 * Instrument logo lookup hints, not a universal instrument identity. Symbols and exchange labels
 * retain their source spelling; providers must document any mapping they perform.
 */
public record LogoRequest(String symbol, Optional<String> exchange) {
  public LogoRequest {
    symbol = Objects.requireNonNull(symbol, "symbol cannot be null").strip();
    if (symbol.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    exchange = Objects.requireNonNull(exchange, "exchange cannot be null")
      .map(String::strip)
      .filter(value -> !value.isEmpty());
  }
}
