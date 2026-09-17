package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChartInteractorTest {

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
    private boolean closed;

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
