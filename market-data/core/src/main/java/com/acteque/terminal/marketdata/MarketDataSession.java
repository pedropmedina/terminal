package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.concurrent.CompletionStage;

/** Provider-neutral market-data operations for one displayed-instrument session. */
public interface MarketDataSession extends AutoCloseable {
  CompletionStage<InstrumentLoadResult> loadInitial();

  CompletionStage<InstrumentLoadResult> loadInstrument(String symbol);

  CompletionStage<List<CalendarData>> loadEarlier();

  @Override
  void close();
}
