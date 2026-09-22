package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.HistoricalInterval;
import com.acteque.terminal.marketdata.HistoricalPage;
import com.acteque.terminal.marketdata.InstrumentHistoryLoadResult;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Coordinates chart state and market-data workflows without depending on JavaFX controls or layout. */
final class ChartInteractor implements AutoCloseable {

  private final ChartModel model;
  private final MarketDataSession marketData;
  private final Executor uiExecutor;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Runnable instrumentLoadStartedHandler = () -> {};
  private Consumer<InstrumentLoadResult> instrumentLoadedHandler = ignored -> {};
  private Consumer<List<CalendarData>> earlierHistoryLoadedHandler = ignored -> {};
  private BiConsumer<String, Throwable> instrumentLoadFailedHandler = (symbol, failure) -> {};
  private Consumer<Throwable> earlierHistoryLoadFailedHandler = ignored -> {};
  private Consumer<InstrumentHistoryLoadResult> historyLoadedHandler = ignored -> {};
  private Consumer<HistoricalPage> earlierSelectedHistoryLoadedHandler = ignored -> {};
  private long instrumentLoadGeneration;
  private String requestedSymbol;
  private ChartInterval requestedInterval;
  private boolean historyLoadInProgress;
  private boolean closed;

  ChartInteractor(ChartModel model) {
    this(model, null, Runnable::run);
  }

  ChartInteractor(ChartModel model, MarketDataSession marketData, Executor uiExecutor) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.marketData = marketData;
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
  }

  void initialize(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
    requestedInterval = interval;
    requestedSymbol = model.getSymbol();
  }

  void openInstrumentSearch() {
    if (!model.isModalOpen()) {
      model.setInstrumentSearchOpen(true);
    }
  }

  void closeInstrumentSearch() {
    model.setInstrumentSearchOpen(false);
  }

  void openIntervalSelection() {
    if (!model.isModalOpen()) {
      model.setIntervalSelectionOpen(true);
    }
  }

  void closeIntervalSelection() {
    model.setIntervalSelectionOpen(false);
  }

  void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void selectInterval(ChartInterval interval) {
    ChartInterval selectedInterval = Objects.requireNonNull(interval, "interval cannot be null");
    model.setInterval(selectedInterval);
    closeIntervalSelection();
    intervalSelectedHandler.accept(selectedInterval);
  }

  void onInstrumentLoadStarted(Runnable callback) {
    instrumentLoadStartedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onInstrumentLoaded(Consumer<InstrumentLoadResult> callback) {
    instrumentLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onEarlierHistoryLoaded(Consumer<List<CalendarData>> callback) {
    earlierHistoryLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onInstrumentLoadFailed(BiConsumer<String, Throwable> callback) {
    instrumentLoadFailedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onEarlierHistoryLoadFailed(Consumer<Throwable> callback) {
    earlierHistoryLoadFailedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onHistoryLoaded(Consumer<InstrumentHistoryLoadResult> callback) {
    historyLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  void onEarlierSelectedHistoryLoaded(Consumer<HistoricalPage> callback) {
    earlierSelectedHistoryLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  boolean supports(ChartInterval interval) {
    return ChartIntervalHistoryMapper.map(interval)
      .filter(candidate -> marketData != null && marketData.supports(candidate))
      .isPresent();
  }

  void loadInitialHistory(String symbol) {
    requestedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    HistoricalInterval interval = selectedHistoryInterval();
    displayHistoryLoad(symbol, requestedInterval, requireMarketData().loadInitial(interval));
  }

  void selectInstrumentHistory(String symbol) {
    requestedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    HistoricalInterval interval = selectedHistoryInterval();
    displayHistoryLoad(symbol, requestedInterval, requireMarketData().loadInstrumentHistory(symbol, interval));
  }

  void requestInterval(ChartInterval interval) {
    ChartInterval requested = Objects.requireNonNull(interval, "interval cannot be null");
    if (marketData == null) {
      selectInterval(requested);
      return;
    }
    HistoricalInterval historical = ChartIntervalHistoryMapper.map(requested)
      .filter(marketData::supports)
      .orElseThrow(() -> new IllegalArgumentException("Unsupported chart interval: " + requested));
    closeIntervalSelection();
    if (requested.equals(model.getInterval()) && !historyLoadInProgress) {
      return;
    }
    requestedInterval = requested;
    String symbol = Objects.requireNonNull(requestedSymbol, "requestedSymbol cannot be null");
    displayHistoryLoad(symbol, requested, marketData.loadInstrumentHistory(symbol, historical));
  }

  void loadEarlierSelectedHistory() {
    long generation = instrumentLoadGeneration;
    requireMarketData()
      .loadEarlierHistory()
      .whenComplete((page, failure) ->
        uiExecutor.execute(() -> {
          if (closed || generation != instrumentLoadGeneration) {
            return;
          }
          if (failure != null) {
            earlierHistoryLoadFailedHandler.accept(unwrap(failure));
          } else {
            earlierSelectedHistoryLoadedHandler.accept(page);
          }
        })
      );
  }

  private HistoricalInterval selectedHistoryInterval() {
    return ChartIntervalHistoryMapper.map(requestedInterval)
      .filter(candidate -> requireMarketData().supports(candidate))
      .orElseThrow(() -> new IllegalArgumentException("Unsupported chart interval: " + requestedInterval));
  }

  private void displayHistoryLoad(
    String symbol,
    ChartInterval requestedInterval,
    CompletionStage<InstrumentHistoryLoadResult> load
  ) {
    long generation = ++instrumentLoadGeneration;
    historyLoadInProgress = true;
    model.setLoadError(null);
    instrumentLoadStartedHandler.run();
    load.whenComplete((result, failure) ->
      uiExecutor.execute(() -> {
        if (closed || generation != instrumentLoadGeneration) {
          return;
        }
        historyLoadInProgress = false;
        if (failure != null) {
          Throwable cause = unwrap(failure);
          requestedSymbol = model.getSymbol();
          this.requestedInterval = model.getInterval();
          if (!(cause instanceof CancellationException)) {
            model.setLoadError(
              "Unable to load " + symbol + " at " + requestedInterval.name() + ": " + cause.getMessage()
            );
            instrumentLoadFailedHandler.accept(symbol, cause);
          }
          return;
        }
        requestedSymbol = result.symbol();
        this.requestedInterval = requestedInterval;
        model.setInterval(requestedInterval);
        model.setLoadError(null);
        intervalSelectedHandler.accept(requestedInterval);
        historyLoadedHandler.accept(result);
      })
    );
  }

  void loadInitialInstrument(String symbol) {
    displayInstrumentLoad(Objects.requireNonNull(symbol, "symbol cannot be null"), requireMarketData().loadInitial());
  }

  void selectInstrument(String symbol) {
    String selectedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    displayInstrumentLoad(selectedSymbol, requireMarketData().loadInstrument(selectedSymbol));
  }

  void loadEarlierHistory() {
    long generation = instrumentLoadGeneration;
    requireMarketData()
      .loadEarlier()
      .whenComplete((bars, failure) ->
        uiExecutor.execute(() -> {
          if (closed || generation != instrumentLoadGeneration) {
            return;
          }
          if (failure != null) {
            earlierHistoryLoadFailedHandler.accept(unwrap(failure));
          } else {
            earlierHistoryLoadedHandler.accept(bars);
          }
        })
      );
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    ++instrumentLoadGeneration;
    if (marketData != null) {
      marketData.close();
    }
  }

  private void displayInstrumentLoad(String symbol, CompletionStage<InstrumentLoadResult> load) {
    long generation = ++instrumentLoadGeneration;
    instrumentLoadStartedHandler.run();
    load.whenComplete((instrument, failure) ->
      uiExecutor.execute(() -> {
        if (closed || generation != instrumentLoadGeneration) {
          return;
        }
        if (failure != null) {
          Throwable cause = unwrap(failure);
          if (!(cause instanceof CancellationException)) {
            instrumentLoadFailedHandler.accept(symbol, cause);
          }
          return;
        }
        instrumentLoadedHandler.accept(instrument);
      })
    );
  }

  private MarketDataSession requireMarketData() {
    if (marketData == null) {
      throw new IllegalStateException("This chart has no market-data session");
    }
    return marketData;
  }

  private static Throwable unwrap(Throwable failure) {
    return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
  }
}
