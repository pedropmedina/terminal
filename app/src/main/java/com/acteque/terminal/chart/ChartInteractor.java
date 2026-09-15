package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.LoadedInstrument;
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
  private Consumer<LoadedInstrument> instrumentLoadedHandler = ignored -> {};
  private Consumer<List<DailyBar>> earlierHistoryLoadedHandler = ignored -> {};
  private BiConsumer<String, Throwable> instrumentLoadFailedHandler = (symbol, failure) -> {};
  private Consumer<Throwable> earlierHistoryLoadFailedHandler = ignored -> {};
  private long instrumentLoadGeneration;
  private boolean closed;

  ChartInteractor(ChartModel model) {
    this(model, null, Runnable::run);
  }

  ChartInteractor(ChartModel model, MarketDataSession marketData, Executor uiExecutor) {
    this.model = Objects.requireNonNull(model, "model");
    this.marketData = marketData;
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
  }

  void initialize(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval"));
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
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback");
  }

  void selectInterval(ChartInterval interval) {
    ChartInterval selectedInterval = Objects.requireNonNull(interval, "interval");
    model.setInterval(selectedInterval);
    closeIntervalSelection();
    intervalSelectedHandler.accept(selectedInterval);
  }

  void onInstrumentLoadStarted(Runnable callback) {
    instrumentLoadStartedHandler = Objects.requireNonNull(callback, "callback");
  }

  void onInstrumentLoaded(Consumer<LoadedInstrument> callback) {
    instrumentLoadedHandler = Objects.requireNonNull(callback, "callback");
  }

  void onEarlierHistoryLoaded(Consumer<List<DailyBar>> callback) {
    earlierHistoryLoadedHandler = Objects.requireNonNull(callback, "callback");
  }

  void onInstrumentLoadFailed(BiConsumer<String, Throwable> callback) {
    instrumentLoadFailedHandler = Objects.requireNonNull(callback, "callback");
  }

  void onEarlierHistoryLoadFailed(Consumer<Throwable> callback) {
    earlierHistoryLoadFailedHandler = Objects.requireNonNull(callback, "callback");
  }

  void loadInitialInstrument(String symbol) {
    displayInstrumentLoad(Objects.requireNonNull(symbol, "symbol"), requireMarketData().loadInitial());
  }

  void selectInstrument(String symbol) {
    String selectedSymbol = Objects.requireNonNull(symbol, "symbol");
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

  private void displayInstrumentLoad(String symbol, CompletionStage<LoadedInstrument> load) {
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
