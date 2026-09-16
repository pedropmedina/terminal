package com.acteque.terminal.marketdata;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Owns one instrument session and its executor, but never closes the shared provider. */
public final class DefaultMarketDataSession implements MarketDataSession {

  private static final System.Logger LOGGER = System.getLogger(DefaultMarketDataSession.class.getName());
  private static final long HISTORY_PAGE_MONTHS = 6;

  private final HistoricalBarData history;
  private final Optional<InstrumentDiscovery> discovery;
  private String symbol;
  private final Clock clock;
  private final ExecutorService executor;
  private final NavigableMap<LocalDate, DailyBar> barsByDate = new TreeMap<>();

  private boolean earlierHistoryLoadInProgress;
  private boolean allEarlierHistoryLoaded;
  private long instrumentLoadGeneration;

  public DefaultMarketDataSession(MarketDataClient client, String symbol) {
    this(client, symbol, Clock.systemDefaultZone(), Executors.newVirtualThreadPerTaskExecutor());
  }

  DefaultMarketDataSession(MarketDataClient client, String symbol, Clock clock, ExecutorService executor) {
    Objects.requireNonNull(client, "client");
    this.history = client
      .historicalBars()
      .orElseThrow(() ->
        new IllegalArgumentException("Provider " + client.provider() + " does not support historical bars")
      );
    this.discovery = client.discovery();
    this.symbol = Objects.requireNonNull(symbol, "symbol");
    this.clock = Objects.requireNonNull(clock, "clock");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  @Override
  public synchronized CompletionStage<LoadedInstrument> loadInitial() {
    return loadInstrument(symbol);
  }

  @Override
  public CompletionStage<List<DailyBar>> loadEarlier() {
    LocalDate oldestAvailableDate;
    String requestedSymbol;
    long generation;
    synchronized (this) {
      if (earlierHistoryLoadInProgress || allEarlierHistoryLoaded || barsByDate.isEmpty()) {
        return CompletableFuture.completedFuture(snapshot());
      }

      earlierHistoryLoadInProgress = true;
      oldestAvailableDate = barsByDate.firstKey();
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

            int previousSize = barsByDate.size();
            addBars(page);
            if (barsByDate.size() == previousSize) {
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

  @Override
  public CompletionStage<LoadedInstrument> loadInstrument(String symbol) {
    String requestedSymbol = normalizeSymbol(symbol);
    long generation;
    synchronized (this) {
      generation = ++instrumentLoadGeneration;
    }

    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusMonths(HISTORY_PAGE_MONTHS);
    return CompletableFuture.supplyAsync(() -> {
      List<DailyBar> page = loadPage(requestedSymbol, startDate, endDate);
      InstrumentDetails details;
      try {
        details = discovery
          .map(feature -> feature.getInstrument(requestedSymbol))
          .orElseGet(() ->
            new InstrumentDetails(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty())
          );
      } catch (MarketDataException exception) {
        LOGGER.log(
          System.Logger.Level.WARNING,
          "Could not load metadata for " +
            requestedSymbol +
            " (" +
            exception.code() +
            "); using the symbol as the display name. Check provider availability, credentials, and symbol support.",
          exception
        );
        details = new InstrumentDetails(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty());
      }
      return new LoadedInstrument(requestedSymbol, details.name().orElse(requestedSymbol), page, details);
    }, executor).handle((loaded, failure) -> {
      synchronized (this) {
        if (generation != instrumentLoadGeneration) {
          throw new CancellationException("A newer instrument was selected");
        }
        earlierHistoryLoadInProgress = false;
        if (failure != null) {
          throw asCompletionException(failure);
        }

        this.symbol = requestedSymbol;
        barsByDate.clear();
        allEarlierHistoryLoaded = false;
        addBars(loaded.bars());
        return new LoadedInstrument(requestedSymbol, loaded.displayName(), snapshot(), loaded.details());
      }
    });
  }

  @Override
  public void close() {
    executor.close();
  }

  private List<DailyBar> loadPage(String symbol, LocalDate startDate, LocalDate endDate) {
    return history.getDailyBars(new DailyBarRequest(symbol, startDate, endDate));
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

  private void addBars(List<DailyBar> bars) {
    for (DailyBar bar : bars) {
      barsByDate.put(bar.date(), bar);
    }
  }

  private List<DailyBar> snapshot() {
    return List.copyOf(barsByDate.values());
  }
}
