package com.acteque.terminal.marketdata;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
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
  private static final long INTRADAY_TARGET_BARS = 500;

  private final HistoricalData historical;
  private final Optional<InstrumentCatalog> catalog;
  private String symbol;
  private final Clock clock;
  private final ExecutorService executor;
  private final NavigableMap<LocalDate, CalendarData> calendarDataByDate = new TreeMap<>();
  private final NavigableMap<Instant, IntradayData> intradayDataByTime = new TreeMap<>();
  private HistoricalInterval activeInterval = new HistoricalInterval.Calendar(CalendarInterval.DAILY);

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
  public boolean supports(HistoricalInterval interval) {
    Objects.requireNonNull(interval, "interval cannot be null");
    return switch (interval) {
      case HistoricalInterval.Calendar calendar -> historical.supports(calendar.value());
      case HistoricalInterval.Intraday intraday -> historical.supports(intraday.value());
    };
  }

  @Override
  public CompletionStage<InstrumentHistoryLoadResult> loadInitial(HistoricalInterval interval) {
    return loadInstrumentHistory(symbol, interval);
  }

  @Override
  public CompletionStage<InstrumentHistoryLoadResult> loadInstrumentHistory(
    String symbol,
    HistoricalInterval interval
  ) {
    String requestedSymbol = normalizeSymbol(symbol);

    HistoricalInterval requestedInterval = Objects.requireNonNull(interval, "interval cannot be null");
    if (!supports(requestedInterval)) {
      throw new IllegalArgumentException("Unsupported historical interval: " + requestedInterval);
    }

    LocalDate endDate = LocalDate.now(clock);
    LocalDate startDate = pageStart(endDate, requestedInterval);

    long generation;
    synchronized (this) {
      generation = ++instrumentLoadGeneration;
    }

    return CompletableFuture.supplyAsync(() -> {
      HistoricalPage page = loadPage(requestedSymbol, startDate, endDate, requestedInterval);
      Instrument instrument = loadMetadata(requestedSymbol);
      return new InstrumentHistoryLoadResult(
        requestedSymbol,
        instrument.name().orElse(requestedSymbol),
        instrument,
        page
      );
    }, executor).handle((result, failure) -> {
      synchronized (this) {
        if (generation != instrumentLoadGeneration) {
          throw new CancellationException("A newer instrument or interval was selected");
        }

        earlierHistoryLoadInProgress = false;

        if (failure != null) {
          throw asCompletionException(failure);
        }

        this.symbol = requestedSymbol;
        activeInterval = requestedInterval;
        calendarDataByDate.clear();
        intradayDataByTime.clear();
        allEarlierHistoryLoaded = false;

        mergeHistory(result.history());

        return new InstrumentHistoryLoadResult(
          requestedSymbol,
          result.displayName(),
          result.details(),
          historySnapshot()
        );
      }
    });
  }

  @Override
  public CompletionStage<HistoricalPage> loadEarlierHistory() {
    LocalDate oldestDate;
    String requestedSymbol;
    HistoricalInterval requestedInterval;
    long generation;
    synchronized (this) {
      if (earlierHistoryLoadInProgress || allEarlierHistoryLoaded || historyEmpty()) {
        return CompletableFuture.completedFuture(historySnapshot());
      }

      earlierHistoryLoadInProgress = true;
      requestedInterval = activeInterval;
      oldestDate =
        requestedInterval instanceof HistoricalInterval.Calendar
          ? calendarDataByDate.firstKey()
          : intradayDataByTime.firstKey().atZone(clock.getZone()).toLocalDate();
      requestedSymbol = symbol;
      generation = instrumentLoadGeneration;
    }

    LocalDate endDate = oldestDate.minusDays(1);
    LocalDate startDate =
      requestedInterval instanceof HistoricalInterval.Calendar
        ? oldestDate.minusMonths(HISTORY_PAGE_MONTHS)
        : pageStart(endDate, requestedInterval);
    try {
      return CompletableFuture.supplyAsync(
        () -> loadPage(requestedSymbol, startDate, endDate, requestedInterval),
        executor
      ).handle((page, failure) -> {
        synchronized (this) {
          if (generation != instrumentLoadGeneration) {
            return historySnapshot();
          }

          earlierHistoryLoadInProgress = false;

          if (failure != null) {
            throw asCompletionException(failure);
          }

          int previousSize = historySize();

          mergeHistory(page);

          if (historySize() == previousSize) {
            allEarlierHistoryLoaded = true;
          }

          return historySnapshot();
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
        activeInterval = new HistoricalInterval.Calendar(CalendarInterval.DAILY);
        calendarDataByDate.clear();
        intradayDataByTime.clear();
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

  private HistoricalPage loadPage(String symbol, LocalDate startDate, LocalDate endDate, HistoricalInterval interval) {
    return switch (interval) {
      case HistoricalInterval.Calendar calendar -> HistoricalPage.calendar(
        calendar.value(),
        historical.getCalendarData(new CalendarRequest(symbol, startDate, endDate, calendar.value()))
      );
      case HistoricalInterval.Intraday intraday -> HistoricalPage.intraday(
        intraday.value(),
        historical.getIntradayData(new IntradayRequest(symbol, startDate, endDate, intraday.value()))
      );
    };
  }

  private LocalDate pageStart(LocalDate endDate, HistoricalInterval interval) {
    if (interval instanceof HistoricalInterval.Calendar) {
      return endDate.minusMonths(HISTORY_PAGE_MONTHS);
    }
    Duration duration = ((HistoricalInterval.Intraday) interval).value();
    long tradingDays = Math.ceilDiv(INTRADAY_TARGET_BARS * duration.toMinutes(), 390);
    long calendarDays = Math.clamp(Math.ceilDiv(tradingDays * 7, 5), 7, 365);
    return endDate.minusDays(calendarDays - 1);
  }

  private Instrument loadMetadata(String requestedSymbol) {
    try {
      return catalog
        .map(feature -> feature.getInstrument(requestedSymbol))
        .orElseGet(() -> new Instrument(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty()));
    } catch (MarketDataException exception) {
      LOGGER.log(System.Logger.Level.WARNING, "Could not load metadata for " + requestedSymbol, exception);
      return new Instrument(requestedSymbol, Optional.empty(), Optional.empty(), Optional.empty());
    }
  }

  private void mergeHistory(HistoricalPage page) {
    page.calendarData().forEach(bar -> calendarDataByDate.put(bar.date(), bar));
    page.intradayData().forEach(bar -> intradayDataByTime.put(bar.timestamp(), bar));
  }

  private HistoricalPage historySnapshot() {
    return new HistoricalPage(
      activeInterval,
      List.copyOf(calendarDataByDate.values()),
      List.copyOf(intradayDataByTime.values())
    );
  }

  private boolean historyEmpty() {
    return calendarDataByDate.isEmpty() && intradayDataByTime.isEmpty();
  }

  private int historySize() {
    return calendarDataByDate.size() + intradayDataByTime.size();
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
