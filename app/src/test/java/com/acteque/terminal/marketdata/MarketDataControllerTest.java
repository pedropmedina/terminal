package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.MarketDataController.LoadedInstrument;
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

class MarketDataControllerTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void loadsInitialAndEarlierPagesIntoOneOrderedDeduplicatedSnapshot() {
    StubMarketDataClient client = new StubMarketDataClient(
      List.of(bar("2026-02-23", "101"), bar("2026-08-20", "102")),
      List.of(bar("2025-09-02", "99"), bar("2026-02-23", "100"))
    );
    try (
      MarketDataController controller = new MarketDataController(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      LoadedInstrument initial = controller.loadInitial().toCompletableFuture().join();
      List<DailyBar> withEarlierHistory = controller.loadEarlier().toCompletableFuture().join();

      assertEquals(List.of(date("2026-02-23"), date("2026-08-20")), dates(initial.bars()));
      assertEquals(List.of(date("2025-09-02"), date("2026-02-23"), date("2026-08-20")), dates(withEarlierHistory));
      assertEquals(new BigDecimal("100"), withEarlierHistory.get(1).prices().close());
      assertEquals(List.of("IBM"), client.metadataRequests);
      assertEquals(
        List.of(
          new DailyBarRequest("IBM", date("2026-02-21"), date("2026-08-21")),
          new DailyBarRequest("IBM", date("2025-08-23"), date("2026-02-22"))
        ),
        client.requests
      );
    }
  }

  @Test
  void stopsRequestingOnceAnEarlierPageAddsNoData() {
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-02-23", "101")), List.of());
    try (
      MarketDataController controller = new MarketDataController(
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
    StubMarketDataClient client = new StubMarketDataClient(
      List.of(bar("IBM", "2026-08-20", "102")),
      List.of(bar("AAPL", "2026-08-19", "201"), bar("AAPL", "2026-08-20", "202"))
    );
    try (
      MarketDataController controller = new MarketDataController(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      LoadedInstrument initial = controller.loadInitial().toCompletableFuture().join();
      LoadedInstrument selected = controller.loadInstrument(" aapl ").toCompletableFuture().join();

      assertEquals("IBM", initial.symbol());
      assertEquals("IBM name", initial.displayName());
      assertEquals(List.of(date("2026-08-20")), dates(initial.bars()));
      assertEquals(new BigDecimal("102"), initial.bars().getFirst().prices().close());
      assertEquals("AAPL", selected.symbol());
      assertEquals("AAPL name", selected.displayName());
      assertEquals(List.of("IBM", "AAPL"), client.metadataRequests);
      assertEquals(List.of(date("2026-08-19"), date("2026-08-20")), dates(selected.bars()));
      assertEquals(
        List.of(
          new DailyBarRequest("IBM", date("2026-02-21"), date("2026-08-21")),
          new DailyBarRequest("AAPL", date("2026-02-21"), date("2026-08-21"))
        ),
        client.requests
      );
    }
  }

  @Test
  void initialLoadRunsAsynchronouslyOnTheExecutor() throws Exception {
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")));
    CountDownLatch executorStarted = new CountDownLatch(1);
    CountDownLatch releaseExecutor = new CountDownLatch(1);
    var executor = Executors.newSingleThreadExecutor();
    try (MarketDataController controller = new MarketDataController(client, " ibm ", CLOCK, executor)) {
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
        LoadedInstrument loaded = initial.get(5, TimeUnit.SECONDS);
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
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")));
    client.metadata = symbol -> details("provider-symbol", Optional.empty());
    try (MarketDataController controller = controller(client)) {
      LoadedInstrument loaded = controller.loadInitial().toCompletableFuture().join();
      assertEquals("IBM", loaded.symbol());
      assertEquals("IBM", loaded.displayName());
      assertEquals(new BigDecimal("102"), loaded.bars().getFirst().prices().close());
      assertEquals(List.of("IBM"), client.metadataRequests);
    }
  }

  @Test
  void fallsBackToSymbolOnMetadataFailureWithoutLosingHistory() {
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")), List.of());
    client.metadata = symbol -> {
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Metadata service unavailable");
    };
    try (MarketDataController controller = controller(client)) {
      LoadedInstrument loaded = controller.loadInitial().toCompletableFuture().join();
      assertEquals("IBM", loaded.displayName());
      assertEquals(new BigDecimal("102"), loaded.bars().getFirst().prices().close());
      assertEquals(loaded.bars(), controller.loadEarlier().toCompletableFuture().join());
      assertEquals(List.of("IBM"), client.metadataRequests);
    }
  }

  @Test
  void doesNotHideMetadataProgrammingErrors() {
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-08-20", "102")));
    IllegalStateException failure = new IllegalStateException("Broken metadata implementation");
    client.metadata = symbol -> {
      throw failure;
    };
    try (MarketDataController controller = controller(client)) {
      CompletionException thrown = assertThrows(CompletionException.class, () ->
        controller.loadInitial().toCompletableFuture().join()
      );
      assertSame(failure, thrown.getCause());
      assertTrue(controller.loadEarlier().toCompletableFuture().join().isEmpty());
    }
  }

  @Test
  void cancelsStaleInitialLoadWithoutMixingMetadataOrHistory() throws Exception {
    StubMarketDataClient client = new StubMarketDataClient(
      List.of(bar("2026-08-20", "102")),
      List.of(bar("AAPL", "2026-08-19", "201")),
      List.of(bar("AAPL", "2026-02-18", "199"))
    );
    CountDownLatch metadataStarted = new CountDownLatch(1);
    CountDownLatch releaseMetadata = new CountDownLatch(1);
    client.metadata = symbol -> {
      if (symbol.equals("IBM")) {
        metadataStarted.countDown();
        await(releaseMetadata);
      }
      return details(symbol, Optional.of(symbol + " name"));
    };
    try (MarketDataController controller = controller(client)) {
      try {
        var stale = controller.loadInitial().toCompletableFuture();
        assertTrue(metadataStarted.await(5, TimeUnit.SECONDS));
        LoadedInstrument selected = controller.loadInstrument("AAPL").toCompletableFuture().get(5, TimeUnit.SECONDS);
        releaseMetadata.countDown();
        CompletionException failure = assertThrows(CompletionException.class, stale::join);
        assertInstanceOf(CancellationException.class, failure.getCause());
        assertEquals("AAPL", selected.symbol());
        assertEquals("AAPL name", selected.displayName());
        assertEquals(List.of(date("2026-08-19")), dates(selected.bars()));
        assertEquals(new BigDecimal("201"), selected.bars().getFirst().prices().close());
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

  @Test
  void loadedInstrumentDefensivelyCopiesItsBars() {
    List<DailyBar> bars = new ArrayList<>();
    LoadedInstrument loaded = new LoadedInstrument("IBM", "IBM name", bars);
    bars.add(bar("2026-08-20", "1"));
    assertTrue(loaded.bars().isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> loaded.bars().add(bars.getFirst()));
  }

  private static MarketDataController controller(StubMarketDataClient client) {
    return new MarketDataController(client, "IBM", CLOCK, Executors.newVirtualThreadPerTaskExecutor());
  }

  private static InstrumentDetails details(String symbol, Optional<String> name) {
    return new InstrumentDetails(symbol, name, Optional.empty(), Optional.empty());
  }

  private static void await(CountDownLatch latch) {
    try {
      assertTrue(latch.await(5, TimeUnit.SECONDS), "Timed out waiting for test coordination");
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }

  private static List<LocalDate> dates(List<DailyBar> bars) {
    return bars.stream().map(DailyBar::date).toList();
  }

  private static LocalDate date(String value) {
    return LocalDate.parse(value);
  }

  private static DailyBar bar(String date, String close) {
    return bar("IBM", date, close);
  }

  private static DailyBar bar(String symbol, String date, String close) {
    BigDecimal price = new BigDecimal(close);
    Ohlcv prices = new Ohlcv(price, price, price, price, new BigDecimal("1000"));
    return new DailyBar(symbol, LocalDate.parse(date), prices, Optional.empty(), Optional.empty(), Optional.empty());
  }

  private static final class StubMarketDataClient implements MarketDataClient, HistoricalBarData {

    private final List<List<DailyBar>> responses;
    private final List<DailyBarRequest> requests = new CopyOnWriteArrayList<>();
    private final List<String> metadataRequests = new CopyOnWriteArrayList<>();
    private InstrumentDiscovery metadata = symbol -> details(symbol, Optional.of(symbol + " name"));
    private int responseIndex;

    @SafeVarargs
    private StubMarketDataClient(List<DailyBar>... responses) {
      this.responses = List.of(responses);
    }

    @Override
    public String provider() {
      return "stub";
    }

    @Override
    public HistoricalBarData historicalBars() {
      return this;
    }

    @Override
    public InstrumentDiscovery discovery() {
      return symbol -> {
        metadataRequests.add(symbol);
        return metadata.getInstrument(symbol);
      };
    }

    @Override
    public synchronized List<DailyBar> getDailyBars(DailyBarRequest request) {
      requests.add(request);
      return responses.get(responseIndex++);
    }

    @Override
    public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
      throw new UnsupportedOperationException();
    }
  }
}
