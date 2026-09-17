package com.acteque.terminal;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.MarketDataException;
import java.util.List;
import java.util.function.Supplier;

public record StubInstrumentCatalog(Supplier<List<Instrument>> instruments) implements InstrumentCatalog {
  @Override
  public List<Instrument> getInstruments() {
    return instruments.get();
  }

  @Override
  public Instrument getInstrument(String symbol) {
    return getInstruments()
      .stream()
      .filter(instrument -> instrument.symbol().equals(symbol))
      .findFirst()
      .orElseThrow(() -> new MarketDataException(MarketDataException.Code.NOT_FOUND, "Unknown symbol"));
  }
}
