package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.HistoricalInterval;
import com.acteque.terminal.marketdata.HistoricalPage;
import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentHistoryLoadResult;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChartInteractorTest {

  @Test
  void appliesOnlyTheLatestSuccessfulIntervalLoad() {
    ChartModel model = new ChartModel();
    model.setSymbol("IBM");
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(model, marketData, uiQueue::add)) {
      interactor.initialize(ChartInterval.DAILY);
      interactor.requestInterval(ChartInterval.FIVE_MINUTES);
      interactor.requestInterval(ChartInterval.ONE_HOUR);
      assertEquals(ChartInterval.DAILY, model.getInterval());

      marketData.historyLoads.getFirst().complete(history("IBM", Duration.ofMinutes(5)));
      marketData.historyLoads.getLast().complete(history("IBM", Duration.ofHours(1)));
      uiQueue.removeFirst().run();
      assertEquals(ChartInterval.DAILY, model.getInterval());
      uiQueue.removeFirst().run();
      assertEquals(ChartInterval.ONE_HOUR, model.getInterval());
    }
  }

  @Test
  void failedIntervalLoadKeepsTheDisplayedIntervalAndReportsAnError() {
    ChartModel model = new ChartModel();
    model.setSymbol("IBM");
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(model, marketData, uiQueue::add)) {
      interactor.initialize(ChartInterval.DAILY);
      interactor.requestInterval(ChartInterval.ONE_HOUR);
      marketData.historyLoads.getFirst().completeExceptionally(new IllegalStateException("Provider rejected 1h"));
      uiQueue.removeFirst().run();

      assertEquals(ChartInterval.DAILY, model.getInterval());
      assertTrue(model.loadErrorProperty().get().contains("Provider rejected 1h"));
    }
  }

  @Test
  void intervalSelectionUsesTheMostRecentlyRequestedSymbol() {
    ChartModel model = new ChartModel();
    model.setSymbol("IBM");
    StubMarketDataSession marketData = new StubMarketDataSession();
    try (ChartInteractor interactor = new ChartInteractor(model, marketData, Runnable::run)) {
      interactor.initialize(ChartInterval.DAILY);
      interactor.selectInstrumentHistory("AAPL");
      interactor.requestInterval(ChartInterval.ONE_HOUR);

      assertEquals(List.of("AAPL", "AAPL"), marketData.requestedSymbols);
      assertEquals(new HistoricalInterval.Intraday(Duration.ofHours(1)), marketData.requestedIntervals.getLast());
    }
  }

  @Test
  void selectingDisplayedIntervalAgainSupersedesAPendingSelection() {
    ChartModel model = new ChartModel();
    model.setSymbol("IBM");
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(model, marketData, uiQueue::add)) {
      interactor.initialize(ChartInterval.DAILY);
      interactor.requestInterval(ChartInterval.ONE_HOUR);
      interactor.requestInterval(ChartInterval.DAILY);
      assertEquals(2, marketData.historyLoads.size());

      marketData.historyLoads.getFirst().complete(history("IBM", Duration.ofHours(1)));
      marketData.historyLoads
        .getLast()
        .complete(
          new InstrumentHistoryLoadResult(
            "IBM",
            "IBM",
            new Instrument("IBM", Optional.of("IBM"), Optional.empty(), Optional.empty()),
            HistoricalPage.calendar(com.acteque.terminal.marketdata.CalendarInterval.DAILY, List.of())
          )
        );
      uiQueue.removeFirst().run();
      uiQueue.removeFirst().run();
      assertEquals(ChartInterval.DAILY, model.getInterval());
    }
  }

  private static InstrumentHistoryLoadResult history(String symbol, Duration interval) {
    return new InstrumentHistoryLoadResult(
      symbol,
      symbol,
      new Instrument(symbol, Optional.of(symbol), Optional.empty(), Optional.empty()),
      HistoricalPage.intraday(interval, List.of())
    );
  }

  @Test
  void initializesTheCurrentInterval() {
    ChartModel model = new ChartModel();
    try (ChartInteractor interactor = new ChartInteractor(model)) {
      interactor.initialize(ChartInterval.DAILY);

      assertEquals(ChartInterval.DAILY, model.getInterval());
    }
  }

  @Test
  void keepsChartModalsMutuallyExclusive() {
    ChartModel model = new ChartModel();
    try (ChartInteractor interactor = new ChartInteractor(model)) {
      interactor.openInstrumentSearch();
      interactor.openIntervalSelection();

      assertTrue(model.isInstrumentSearchOpen());
      assertFalse(model.isIntervalSelectionOpen());
      assertTrue(model.isModalOpen());

      interactor.closeInstrumentSearch();
      interactor.openIntervalSelection();

      assertFalse(model.isInstrumentSearchOpen());
      assertTrue(model.isIntervalSelectionOpen());
    }
  }

  @Test
  void selectingAnIntervalUpdatesStateAndClosesTheDialog() {
    ChartModel model = new ChartModel();
    try (ChartInteractor interactor = new ChartInteractor(model)) {
      interactor.initialize(ChartInterval.DAILY);
      interactor.openIntervalSelection();
      AtomicReference<ChartInterval> selectedInterval = new AtomicReference<>();
      interactor.onIntervalSelected(selectedInterval::set);

      interactor.selectInterval(ChartInterval.ONE_HOUR);

      assertEquals(ChartInterval.ONE_HOUR, model.getInterval());
      assertEquals(ChartInterval.ONE_HOUR, selectedInterval.get());
      assertFalse(model.isIntervalSelectionOpen());
      assertFalse(model.isModalOpen());
    }
  }

  @Test
  void publishesInstrumentLoadsOnTheUiExecutor() {
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(new ChartModel(), marketData, uiQueue::add)) {
      AtomicReference<InstrumentLoadResult> displayed = new AtomicReference<>();
      interactor.onInstrumentLoaded(displayed::set);

      interactor.loadInitialInstrument("IBM");
      InstrumentLoadResult loaded = new InstrumentLoadResult("IBM", "IBM", List.of());
      marketData.initial.complete(loaded);

      assertNull(displayed.get());
      uiQueue.removeFirst().run();
      assertEquals(loaded, displayed.get());
    }
  }

  @Test
  void ignoresACompletedInstrumentLoadAfterANewerSelection() {
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(new ChartModel(), marketData, uiQueue::add)) {
      AtomicReference<InstrumentLoadResult> displayed = new AtomicReference<>();
      interactor.onInstrumentLoaded(displayed::set);

      interactor.selectInstrument("IBM");
      CompletableFuture<InstrumentLoadResult> ibmLoad = marketData.instrumentLoads.getFirst();
      ibmLoad.complete(new InstrumentLoadResult("IBM", "IBM", List.of()));
      interactor.selectInstrument("AAPL");
      CompletableFuture<InstrumentLoadResult> appleLoad = marketData.instrumentLoads.getLast();
      InstrumentLoadResult apple = new InstrumentLoadResult("AAPL", "Apple", List.of());
      appleLoad.complete(apple);

      uiQueue.removeFirst().run();
      assertNull(displayed.get());
      uiQueue.removeFirst().run();
      assertEquals(apple, displayed.get());
    }
  }

  @Test
  void changingInstrumentInvalidatesAnEarlierHistoryCompletion() {
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(new ChartModel(), marketData, uiQueue::add)) {
      AtomicReference<List<CalendarData>> displayed = new AtomicReference<>();
      interactor.onEarlierHistoryLoaded(displayed::set);

      interactor.loadEarlierHistory();
      marketData.earlier.complete(List.of());
      interactor.selectInstrument("AAPL");

      uiQueue.removeFirst().run();
      assertNull(displayed.get());
    }
  }

  @Test
  void closingInvalidatesUiWorkAndClosesTheMarketDataSession() {
    StubMarketDataSession marketData = new StubMarketDataSession();
    List<Runnable> uiQueue = new ArrayList<>();
    try (ChartInteractor interactor = new ChartInteractor(new ChartModel(), marketData, uiQueue::add)) {
      AtomicReference<InstrumentLoadResult> displayed = new AtomicReference<>();
      interactor.onInstrumentLoaded(displayed::set);

      interactor.loadInitialInstrument("IBM");
      marketData.initial.complete(new InstrumentLoadResult("IBM", "IBM", List.of()));
      interactor.close();
      uiQueue.removeFirst().run();

      assertNull(displayed.get());
      assertTrue(marketData.closed);
    }
  }

  private static final class StubMarketDataSession implements MarketDataSession {

    private final CompletableFuture<InstrumentLoadResult> initial = new CompletableFuture<>();
    private final List<CompletableFuture<InstrumentLoadResult>> instrumentLoads = new ArrayList<>();
    private final CompletableFuture<List<CalendarData>> earlier = new CompletableFuture<>();
    private final List<CompletableFuture<InstrumentHistoryLoadResult>> historyLoads = new ArrayList<>();
    private final List<String> requestedSymbols = new ArrayList<>();
    private final List<HistoricalInterval> requestedIntervals = new ArrayList<>();
    private boolean closed;

    @Override
    public boolean supports(HistoricalInterval interval) {
      return true;
    }

    @Override
    public CompletableFuture<InstrumentHistoryLoadResult> loadInstrumentHistory(
      String symbol,
      HistoricalInterval interval
    ) {
      requestedSymbols.add(symbol);
      requestedIntervals.add(interval);
      CompletableFuture<InstrumentHistoryLoadResult> load = new CompletableFuture<>();
      historyLoads.add(load);
      return load;
    }

    @Override
    public CompletableFuture<InstrumentLoadResult> loadInitial() {
      return initial;
    }

    @Override
    public CompletableFuture<InstrumentLoadResult> loadInstrument(String symbol) {
      CompletableFuture<InstrumentLoadResult> load = new CompletableFuture<>();
      instrumentLoads.add(load);
      return load;
    }

    @Override
    public CompletableFuture<List<CalendarData>> loadEarlier() {
      return earlier;
    }

    @Override
    public void close() {
      closed = true;
    }
  }
}
