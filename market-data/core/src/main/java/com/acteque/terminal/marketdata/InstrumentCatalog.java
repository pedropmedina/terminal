package com.acteque.terminal.marketdata;

import java.util.List;

/** Provider-neutral access to available instruments and their metadata. */
public interface InstrumentCatalog {
  /** Returns the instruments supported by this provider. */
  List<Instrument> getInstruments();

  /**
   * Returns metadata for a symbol recognized by this provider.
   *
   * @throws IllegalArgumentException if the symbol is null or blank
   * @throws MarketDataException with code NOT_FOUND if the instrument does not exist;
   *     other provider and transport failures use the corresponding normalized code
   */
  Instrument getInstrument(String symbol);
}
