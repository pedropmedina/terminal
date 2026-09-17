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
public final class MarketDataSessionDefault implements MarketDataSession {

  private static final System.Logger LOGGER = System.getLogger(MarketDataSessionDefault.class.getName());
  private static final long HISTORY_PAGE_MONTHS = 6;

  private final HistoricalData historical;
  private final Optional<InstrumentCatalog> catalog;
  private String symbol;
  private final Clock clock;
  private final ExecutorService executor;
  private final NavigableMap<LocalDate, CalendarData> calendarDataByDate = new TreeMap<>();

  private boolean earlierHistoryLoadInProgress;
  private boolean allEarlierHistoryLoaded;
  private long instrumentLoadGeneration;

  public MarketDataSessionDefault(MarketDataClient client, String symbol) {
    this(client, symbol, Clock.systemDefaultZone(), Executors.newVirtualThreadPerTaskExecutor());
  }

  MarketDataSessionDefault(MarketDataClient client, String symbol, Clock clock, ExecutorService executor) {
    Objects.requireNonNull(client, "client cannot be null");
    this.historical = client
      .historical()
      .orElseThrow(() ->
        new IllegalArgumentException("Provider " + client.provider() + " does not support historical data")
      );
    this.catalog = client.catalog();
    this.symbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    this.clock = Objects.requireNonNull(clock, "clock cannot be null");
    this.executor = Objects.requireNonNull(executor, "executor cannot be null");
  }

  @Override
  public synchronized CompletionStage<InstrumentLoadResult> loadInitial() {
    return loadInstrument(symbol);
  }

  @Override
  public CompletionStage<List<CalendarData>> loadEarlier() {
    LocalDate oldestAvailableDate;
    String requestedSymbol;
    long generation;
    synchronized (this) {
      if (earlierHistoryLoadInProgress || allEarlierHistoryLoaded || calendarDataByDate.isEmpty()) {
        return CompletableFuture.completedFuture(snapshot());
      }

      earlierHistoryLoadInProgress = true;
      oldestAvailableDate = calendarDataByDate.firstKey();
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

            int previousSize = calendarDataByDate.size();
            mergeCalendarData(page);
            if (calendarDataByDate.size() == previousSize) {
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
  public CompletionStage<InstrumentLoadResult> loadInstrument(String symbol) {
    String requestedSymbol = normalizeSymbol(symbol);
    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = endDate.minusMonths(HISTORY_PAGE_MONTHS);

    long generation;
    synchronized (this) {
      generation = ++instrumentLoadGeneration;
    }

    return CompletableFuture.supplyAsync(() -> {
      List<CalendarData> page = loadPage(requestedSymbol, startDate, endDate);

      Instrument instrument;
      try {
        instrument = catalog
          .map(feature -> feature.getInstrument(requestedSymbol))
          .orElseGet(() -> new Instrument(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty()));
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
        instrument = new Instrument(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty());
      }

      return new InstrumentLoadResult(requestedSymbol, instrument.name().orElse(requestedSymbol), page, instrument);
    }, executor).handle((result, failure) -> {
      synchronized (this) {
        if (generation != instrumentLoadGeneration) {
          throw new CancellationException("A newer instrument was selected");
        }
        earlierHistoryLoadInProgress = false;
        if (failure != null) {
          throw asCompletionException(failure);
        }

        this.symbol = requestedSymbol;
        calendarDataByDate.clear();
        allEarlierHistoryLoaded = false;
        mergeCalendarData(result.calendarData());
        return new InstrumentLoadResult(requestedSymbol, result.displayName(), snapshot(), result.details());
      }
    });
  }

  @Override
  public void close() {
    executor.close();
  }

  private List<CalendarData> loadPage(String symbol, LocalDate startDate, LocalDate endDate) {
    return historical.getCalendarData(new CalendarRequest(symbol, startDate, endDate));
  }

  private static String normalizeSymbol(String symbol) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
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

  private void mergeCalendarData(List<CalendarData> calendarData) {
    for (CalendarData entry : calendarData) {
      calendarDataByDate.put(entry.date(), entry);
    }
  }

  private List<CalendarData> snapshot() {
    return List.copyOf(calendarDataByDate.values());
  }
}
