package com.acteque.terminal.marketdata;

/** Provider-neutral access to instrument metadata. */
public interface InstrumentDiscovery {
  /**
   * Returns metadata for a symbol recognized by this provider.
   *
   * @throws IllegalArgumentException if the symbol is null or blank
   * @throws MarketDataException with code NOT_FOUND if the instrument does not exist;
   *     other provider and transport failures use the corresponding normalized code
   */
  InstrumentDetails getInstrument(String symbol);
}
