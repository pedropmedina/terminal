package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MarketDataSessionDefaultTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void loadsInitialAndEarlierPagesIntoOneOrderedDeduplicatedSnapshot() {
    try (
      StubMarketDataClient client = new StubMarketDataClient(
        List.of(bar("2026-02-23", "101"), bar("2026-08-20", "102")),
        List.of(bar("2025-09-02", "99"), bar("2026-02-23", "100"))
      );
      MarketDataSessionDefault controller = new MarketDataSessionDefault(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      InstrumentLoadResult initial = controller.loadInitial().toCompletableFuture().join();
      List<CalendarData> withEarlierHistory = controller.loadEarlier().toCompletableFuture().join();

      assertEquals(List.of(date("2026-02-23"), date("2026-08-20")), dates(initial.calendarData()));
      assertEquals(List.of(date("2025-09-02"), date("2026-02-23"), date("2026-08-20")), dates(withEarlierHistory));
      assertEquals(new BigDecimal("100"), withEarlierHistory.get(1).prices().close());
      assertEquals(List.of("IBM"), client.metadataRequests);
      assertEquals(
        List.of(
          new CalendarRequest("IBM", date("2026-02-21"), date("2026-08-21")),
          new CalendarRequest("IBM", date("2025-08-23"), date("2026-02-22"))
        ),
        client.requests
      );
    }
  }

  @Test
  void stopsRequestingOnceAnEarlierPageAddsNoData() {
    try (
      StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-02-23", "101")), List.of());
      MarketDataSessionDefault controller = new MarketDataSessionDefault(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      controller.loadInitial().toCompletableFuture().join();
      controller.loadEarlier().toCompletableFuture().join();
      controller.loadEarlier().toCompletableFuture().join();

      assertEquals(2, client.requests.size());
    }
  }

  @Test
  void loadsASelectedInstrumentAndReplacesThePreviousHistory() {
    try (
      StubMarketDataClient client = new StubMarketDataClient(
        List.of(bar("IBM", "2026-08-20", "102")),
        List.of(bar("AAPL", "2026-08-19", "201"), bar("AAPL", "2026-08-20", "202"))
      );
      MarketDataSessionDefault controller = new MarketDataSessionDefault(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      InstrumentLoadResult initial = controller.loadInitial().toCompletableFuture().join();
      InstrumentLoadResult selected = controller.loadInstrument(" aapl ").toCompletableFuture().join();

      assertEquals("IBM", initial.symbol());
      assertEquals("IBM name", initial.displayName());
      assertEquals(List.of(date("2026-08-20")), dates(initial.calendarData()));
      assertEquals(new BigDecimal("102"), initial.calendarData().getFirst().prices().close());
      assertEquals("AAPL", selected.symbol());
      assertEquals("AAPL name", selected.displayName());
      assertEquals(List.of("IBM", "AAPL"), client.metadataRequests);
      assertEquals(List.of(date("2026-08-19"), date("2026-08-20")), dates(selected.calendarData()));
      assertEquals(
        List.of(
          new CalendarRequest("IBM", date("2026-02-21"), date("2026-08-21")),
          new CalendarRequest("AAPL", date("2026-02-21"), date("2026-08-21"))
        ),
        client.requests
      );
    }
  }

  @Test
  void initialLoadRunsAsynchronouslyOnTheExecutor() throws Exception {
    CountDownLatch executorStarted = new CountDownLatch(1);
    CountDownLatch releaseExecutor = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try (
      StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")));
      MarketDataSessionDefault controller = new MarketDataSessionDefault(client, " ibm ", CLOCK, executor)
    ) {
      executor.submit(() -> {
        executorStarted.countDown();
        await(releaseExecutor);
      });
      try {
        assertTrue(executorStarted.await(5, TimeUnit.SECONDS));
        var initial = controller.loadInitial().toCompletableFuture();
        assertFalse(initial.isDone());
        assertTrue(client.requests.isEmpty());
        assertTrue(client.metadataRequests.isEmpty());
        releaseExecutor.countDown();
        InstrumentLoadResult loaded = initial.get(5, TimeUnit.SECONDS);
        assertEquals("IBM", loaded.symbol());
        assertEquals("IBM name", loaded.displayName());
        assertEquals(List.of("IBM"), client.metadataRequests);
      } finally {
        releaseExecutor.countDown();
      }
    }
  }

  @Test
  void fallsBackToRequestedSymbolWhenNameIsMissing() {
    try (StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")))) {
      client.metadata = symbol -> details("provider-symbol", Optional.empty());
      try (MarketDataSessionDefault controller = controller(client)) {
        InstrumentLoadResult loaded = controller.loadInitial().toCompletableFuture().join();
        assertEquals("IBM", loaded.symbol());
        assertEquals("IBM", loaded.displayName());
        assertEquals(new BigDecimal("102"), loaded.calendarData().getFirst().prices().close());
        assertEquals(List.of("IBM"), client.metadataRequests);
      }
    }
  }

  @Test
  void fallsBackToSymbolOnMetadataFailureWithoutLosingHistory() {
    try (StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")), List.of())) {
      client.metadata = symbol -> {
        throw new MarketDataException(MarketDataException.Code.NETWORK, "Metadata service unavailable");
      };
      try (MarketDataSessionDefault controller = controller(client)) {
        InstrumentLoadResult loaded = controller.loadInitial().toCompletableFuture().join();
        assertEquals("IBM", loaded.displayName());
        assertEquals(new BigDecimal("102"), loaded.calendarData().getFirst().prices().close());
        assertEquals(loaded.calendarData(), controller.loadEarlier().toCompletableFuture().join());
        assertEquals(List.of("IBM"), client.metadataRequests);
      }
    }
  }

  @Test
  void doesNotHideMetadataProgrammingErrors() {
    IllegalStateException failure = new IllegalStateException("Broken metadata implementation");
    try (StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")))) {
      client.metadata = symbol -> {
        throw failure;
      };
      try (MarketDataSessionDefault controller = controller(client)) {
        CompletionException thrown = assertThrows(CompletionException.class, () ->
          controller.loadInitial().toCompletableFuture().join()
        );
        assertSame(failure, thrown.getCause());
        assertTrue(controller.loadEarlier().toCompletableFuture().join().isEmpty());
      }
    }
  }

  @Test
  void cancelsStaleInitialLoadWithoutMixingMetadataOrHistory() throws Exception {
    CountDownLatch metadataStarted = new CountDownLatch(1);
    CountDownLatch releaseMetadata = new CountDownLatch(1);
    try (
      StubMarketDataClient client = new StubMarketDataClient(
        List.of(bar("2026-08-20", "102")),
        List.of(bar("AAPL", "2026-08-19", "201")),
        List.of(bar("AAPL", "2026-02-18", "199"))
      )
    ) {
      client.metadata = symbol -> {
        if (symbol.equals("IBM")) {
          metadataStarted.countDown();
          await(releaseMetadata);
        }
        return details(symbol, Optional.of(symbol + " name"));
      };
      try (MarketDataSessionDefault controller = controller(client)) {
        try {
          var stale = controller.loadInitial().toCompletableFuture();
          assertTrue(metadataStarted.await(5, TimeUnit.SECONDS));
          InstrumentLoadResult selected = controller
            .loadInstrument("AAPL")
            .toCompletableFuture()
            .get(5, TimeUnit.SECONDS);
          releaseMetadata.countDown();
          CompletionException failure = assertThrows(CompletionException.class, stale::join);
          assertInstanceOf(CancellationException.class, failure.getCause());
          assertEquals("AAPL", selected.symbol());
          assertEquals("AAPL name", selected.displayName());
          assertEquals(List.of(date("2026-08-19")), dates(selected.calendarData()));
          assertEquals(new BigDecimal("201"), selected.calendarData().getFirst().prices().close());
          assertEquals(
            List.of(date("2026-02-18"), date("2026-08-19")),
            dates(controller.loadEarlier().toCompletableFuture().get(5, TimeUnit.SECONDS))
          );
          assertEquals("AAPL", client.requests.getLast().symbol());
          assertEquals(List.of("IBM", "AAPL"), client.metadataRequests);
        } finally {
          releaseMetadata.countDown();
        }
      }
    }
  }

  @Test
  void loadedInstrumentDefensivelyCopiesItsBars() {
    List<CalendarData> bars = new ArrayList<>();
    InstrumentLoadResult loaded = new InstrumentLoadResult("IBM", "IBM name", bars);
    bars.add(bar("2026-08-20", "1"));
    assertTrue(loaded.calendarData().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> loaded.calendarData().add(bars.getFirst()));
  }

  private static MarketDataSessionDefault controller(StubMarketDataClient client) {
    return new MarketDataSessionDefault(client, "IBM", CLOCK, Executors.newVirtualThreadPerTaskExecutor());
  }

  private static Instrument details(String symbol, Optional<String> name) {
    return new Instrument(symbol, name, Optional.empty(), Optional.empty());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for test coordination");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  private static List<LocalDate> dates(List<CalendarData> bars) {
    return bars.stream().map(CalendarData::date).toList();
  }

  private static LocalDate date(String value) {
    return LocalDate.parse(value);
  }

  private static CalendarData bar(String date, String close) {
    return bar("IBM", date, close);
  }

  private static CalendarData bar(String symbol, String date, String close) {
    BigDecimal price = new BigDecimal(close);
    Ohlcv prices = new Ohlcv(price, price, price, price, new BigDecimal("1000"));
    return new CalendarData(
      symbol,
      LocalDate.parse(date),
      prices,
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );
  }

  private static final class StubMarketDataClient implements MarketDataClient, HistoricalData, InstrumentCatalog {

    private final List<List<CalendarData>> responses;
    private final List<CalendarRequest> requests = new CopyOnWriteArrayList<>();
    private final List<String> metadataRequests = new CopyOnWriteArrayList<>();
    private java.util.function.Function<String, Instrument> metadata = symbol ->
      details(symbol, Optional.of(symbol + " name"));
    private int responseIndex;

    @SafeVarargs
    private StubMarketDataClient(List<CalendarData>... responses) {
      this.responses = List.of(responses);
    }

    @Override
    public String provider() {
      return "stub";
    }

    @Override
    public Optional<HistoricalData> historical() {
      return Optional.of(this);
    }

    @Override
    public Optional<InstrumentCatalog> catalog() {
      return Optional.of(this);
    }

    @Override
    public List<Instrument> getInstruments() {
      throw new AssertionError("Session should only request metadata");
    }

    @Override
    public Instrument getInstrument(String symbol) {
      metadataRequests.add(symbol);
      return metadata.apply(symbol);
    }

    @Override
    public synchronized List<CalendarData> getCalendarData(CalendarRequest request) {
      requests.add(request);
      return responses.get(responseIndex++);
    }

    @Override
    public List<IntradayData> getIntradayData(IntradayRequest request) {
      throw new UnsupportedOperationException();
    }
  }
}
