package com.acteque.terminal.marketdata;

import java.util.List;
import java.util.concurrent.CompletionStage;

/** Provider-neutral market-data operations for one displayed-instrument session. */
public interface MarketDataSession extends AutoCloseable {
  /**
   * Reports whether the session's provider accepts an interval.
   *
   * @param interval the historical interval to inspect
   * @return true when the interval can be requested
   */
  default boolean supports(HistoricalInterval interval) {
    return interval instanceof HistoricalInterval.Calendar calendar && calendar.value() == CalendarInterval.DAILY;
  }

  /**
   * Loads the initial instrument and bars at the selected interval.
   *
   * @param interval the requested historical interval
   * @return an asynchronous instrument and history result
   */
  default CompletionStage<InstrumentHistoryLoadResult> loadInitial(HistoricalInterval interval) {
    if (!(interval instanceof HistoricalInterval.Calendar calendar) || calendar.value() != CalendarInterval.DAILY) {
      throw new UnsupportedOperationException("Interval-specific history is unavailable");
    }
    return loadInitial().thenApply(result ->
      new InstrumentHistoryLoadResult(
        result.symbol(),
        result.displayName(),
        result.details(),
        HistoricalPage.calendar(CalendarInterval.DAILY, result.calendarData())
      )
    );
  }

  /**
   * Loads an instrument and its bars at the selected interval.
   *
   * @param symbol the instrument symbol
   * @param interval the requested historical interval
   * @return an asynchronous instrument and history result
   */
  default CompletionStage<InstrumentHistoryLoadResult> loadInstrumentHistory(
    String symbol,
    HistoricalInterval interval
  ) {
    if (!(interval instanceof HistoricalInterval.Calendar calendar) || calendar.value() != CalendarInterval.DAILY) {
      throw new UnsupportedOperationException("Interval-specific history is unavailable");
    }
    return loadInstrument(symbol).thenApply(result ->
      new InstrumentHistoryLoadResult(
        result.symbol(),
        result.displayName(),
        result.details(),
        HistoricalPage.calendar(CalendarInterval.DAILY, result.calendarData())
      )
    );
  }

  /**
   * Loads earlier bars for the active interval.
   *
   * @return an asynchronous ordered history snapshot
   */
  default CompletionStage<HistoricalPage> loadEarlierHistory() {
    return loadEarlier().thenApply(bars -> HistoricalPage.calendar(CalendarInterval.DAILY, bars));
  }

  CompletionStage<InstrumentLoadResult> loadInitial();

  CompletionStage<InstrumentLoadResult> loadInstrument(String symbol);

  CompletionStage<List<CalendarData>> loadEarlier();

  @Override
  void close();
}
