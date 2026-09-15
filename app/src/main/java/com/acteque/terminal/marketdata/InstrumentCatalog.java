package com.acteque.terminal.marketdata;

import java.util.List;

/** Provider-neutral access to instruments available for selection. */
public interface InstrumentCatalog {
  /** Returns the instruments supported by this provider. */
  List<InstrumentDetails> getSupportedInstruments();
}
