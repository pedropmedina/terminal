package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.concurrent.CompletionStage;

/** Provider-neutral market-data operations for one displayed-instrument session. */
public interface MarketDataSession extends AutoCloseable {
  CompletionStage<LoadedInstrument> loadInitial();

  CompletionStage<LoadedInstrument> loadInstrument(String symbol);

  CompletionStage<List<DailyBar>> loadEarlier();

  @Override
  void close();
}
