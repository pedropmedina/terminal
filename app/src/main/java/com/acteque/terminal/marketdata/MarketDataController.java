package com.acteque.terminal.marketdata;

import com.acteque.terminal.chart.PricePoint;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns the loaded daily-price history and fetches earlier pages on demand. */
public final class MarketDataController implements AutoCloseable {

  private static final long HISTORY_PAGE_MONTHS = 6;

  private final MarketDataClient client;
  private final String symbol;
  private final Clock clock;
  private final ExecutorService executor;
  private final NavigableMap<LocalDate, PricePoint> pointsByDate = new TreeMap<>();

  private boolean earlierHistoryLoadInProgress;
  private boolean allEarlierHistoryLoaded;

  public MarketDataController(MarketDataClient client, String symbol) {
    this(client, symbol, Clock.systemDefaultZone(), Executors.newVirtualThreadPerTaskExecutor());
  }

  MarketDataController(MarketDataClient client, String symbol, Clock clock, ExecutorService executor) {
    this.client = Objects.requireNonNull(client, "client");
    this.symbol = Objects.requireNonNull(symbol, "symbol");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  public List<PricePoint> loadInitial() {
    LocalDate endDate = LocalDate.now(clock);
    List<PricePoint> initialPoints = loadPage(endDate.minusMonths(HISTORY_PAGE_MONTHS), endDate);
    synchronized (this) {
      addPoints(initialPoints);
      return snapshot();
    }
  }

  public CompletionStage<List<PricePoint>> loadEarlier() {
    LocalDate oldestAvailableDate;
    synchronized (this) {
      if (earlierHistoryLoadInProgress || allEarlierHistoryLoaded || pointsByDate.isEmpty()) {
        return CompletableFuture.completedFuture(snapshot());
      }

      earlierHistoryLoadInProgress = true;
      oldestAvailableDate = pointsByDate.firstKey();
    }

    LocalDate startDate = oldestAvailableDate.minusMonths(HISTORY_PAGE_MONTHS);
    LocalDate endDate = oldestAvailableDate.minusDays(1);
    try {
      return CompletableFuture.supplyAsync(() -> loadPage(startDate, endDate), executor).handle((page, failure) -> {
        synchronized (this) {
          earlierHistoryLoadInProgress = false;
          if (failure != null) {
            throw failure instanceof CompletionException completionException
              ? completionException
              : new CompletionException(failure);
          }

          int previousSize = pointsByDate.size();
          addPoints(page);
          if (pointsByDate.size() == previousSize) {
            allEarlierHistoryLoaded = true;
          }
          return snapshot();
        }
      });
    } catch (RuntimeException exception) {
      synchronized (this) {
        earlierHistoryLoadInProgress = false;
      }
      throw exception;
    }
  }

  @Override
  public void close() {
    executor.close();
  }

  private List<PricePoint> loadPage(LocalDate startDate, LocalDate endDate) {
    return client
      .getDailyBars(new DailyBarRequest(symbol, startDate, endDate))
      .stream()
      .map(MarketDataController::toPricePoint)
      .toList();
  }

  private void addPoints(List<PricePoint> points) {
    for (PricePoint point : points) {
      pointsByDate.put(point.date(), point);
    }
  }

  private List<PricePoint> snapshot() {
    return List.copyOf(pointsByDate.values());
  }

  private static PricePoint toPricePoint(DailyBar bar) {
    Ohlcv prices = bar.prices();
    return new PricePoint(
      bar.date(),
      prices.open().doubleValue(),
      prices.high().doubleValue(),
      prices.low().doubleValue(),
      prices.close().doubleValue(),
      prices.volume().longValueExact()
    );
  }
}
