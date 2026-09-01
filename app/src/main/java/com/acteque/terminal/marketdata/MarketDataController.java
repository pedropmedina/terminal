package com.acteque.terminal.marketdata;

import com.acteque.terminal.chart.PricePoint;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns the loaded daily-price history and fetches earlier pages on demand. */
public final class MarketDataController implements AutoCloseable {

  private static final long HISTORY_PAGE_MONTHS = 6;

  private final MarketDataClient client;
  private String symbol;
  private final Clock clock;
  private final ExecutorService executor;
  private final NavigableMap<LocalDate, PricePoint> pointsByDate = new TreeMap<>();

  private boolean earlierHistoryLoadInProgress;
  private boolean allEarlierHistoryLoaded;
  private long instrumentLoadGeneration;

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
    String requestedSymbol;
    long generation;
    synchronized (this) {
      if (earlierHistoryLoadInProgress || allEarlierHistoryLoaded || pointsByDate.isEmpty()) {
        return CompletableFuture.completedFuture(snapshot());
      }

      earlierHistoryLoadInProgress = true;
      oldestAvailableDate = pointsByDate.firstKey();
      requestedSymbol = symbol;
      generation = instrumentLoadGeneration;
    }

    LocalDate startDate = oldestAvailableDate.minusMonths(HISTORY_PAGE_MONTHS);
    LocalDate endDate = oldestAvailableDate.minusDays(1);
    try {
      return CompletableFuture.supplyAsync(() -> loadPage(requestedSymbol, startDate, endDate), executor).handle(
        (page, failure) -> {
          synchronized (this) {
            if (generation != instrumentLoadGeneration) {
              return snapshot();
            }
            earlierHistoryLoadInProgress = false;
            if (failure != null) {
              throw asCompletionException(failure);
            }

            int previousSize = pointsByDate.size();
            addPoints(page);
            if (pointsByDate.size() == previousSize) {
              allEarlierHistoryLoaded = true;
            }
            return snapshot();
          }
        }
      );
    } catch (RuntimeException exception) {
      synchronized (this) {
        earlierHistoryLoadInProgress = false;
      }
      throw exception;
    }
  }

  public CompletionStage<List<PricePoint>> loadInstrument(String symbol) {
    String requestedSymbol = normalizeSymbol(symbol);
    long generation;
    synchronized (this) {
      generation = ++instrumentLoadGeneration;
    }

    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusMonths(HISTORY_PAGE_MONTHS);
    return CompletableFuture.supplyAsync(() -> loadPage(requestedSymbol, startDate, endDate), executor).handle(
      (page, failure) -> {
        synchronized (this) {
          if (generation != instrumentLoadGeneration) {
            throw new CancellationException("A newer instrument was selected");
          }
          earlierHistoryLoadInProgress = false;
          if (failure != null) {
            throw asCompletionException(failure);
          }

          this.symbol = requestedSymbol;
          pointsByDate.clear();
          allEarlierHistoryLoaded = false;
          addPoints(page);
          return snapshot();
        }
      }
    );
  }

  @Override
  public void close() {
    executor.close();
  }

  private List<PricePoint> loadPage(LocalDate startDate, LocalDate endDate) {
    return loadPage(symbol, startDate, endDate);
  }

  private List<PricePoint> loadPage(String symbol, LocalDate startDate, LocalDate endDate) {
    return client
      .getDailyBars(new DailyBarRequest(symbol, startDate, endDate))
      .stream()
      .map(MarketDataController::toPricePoint)
      .toList();
  }

  private static String normalizeSymbol(String symbol) {
    Objects.requireNonNull(symbol, "symbol");
    String normalized = symbol.strip().toUpperCase(Locale.ROOT);
    if (normalized.isEmpty()) {
      throw new IllegalArgumentException("symbol must not be blank");
    }
    return normalized;
  }

  private static CompletionException asCompletionException(Throwable failure) {
    return failure instanceof CompletionException completionException
      ? completionException
      : new CompletionException(failure);
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
