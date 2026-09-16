package com.acteque.terminal.marketlogos;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.DefaultMarketDataSession;
import com.acteque.terminal.marketdata.HistoricalBarData;
import com.acteque.terminal.marketdata.InstrumentDetails;
import com.acteque.terminal.marketdata.InstrumentDiscovery;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.MarketDataClient;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class DefaultLogoSessionTest {

  private static final InstrumentLogo LOGO = new InstrumentLogo(
    URI.create("https://images.example.com/a.png"),
    "Logos",
    URI.create("https://example.com")
  );
  private static final InstrumentDetails DETAILS = new InstrumentDetails(
    "IBM",
    Optional.of("Name"),
    Optional.of("Market"),
    Optional.empty()
  );

  @Test
  void resolvesThroughTheInjectedProviderWithoutDownloading() {
    LogoRequest request = new LogoRequest("Provider:Ab.C", Optional.of("Market"));
    InstrumentLogos provider = new InstrumentLogos() {
      public Optional<InstrumentLogo> findLogo(LogoRequest actual) {
        assertSame(request, actual);
        return Optional.of(LOGO);
      }

      public Optional<byte[]> load(InstrumentLogo logo) {
        fail("Resolving a reference must not download it");
        return Optional.empty();
      }
    };
    try (LogoSession session = new DefaultLogoSession(provider)) {
      assertSame(LOGO, session.findLogo(request).orElseThrow());
    }
    try (LogoSession session = new DefaultLogoSession(InstrumentLogos.NONE)) {
      assertTrue(session.findLogo(request).isEmpty());
      assertTrue(session.load(LOGO).toCompletableFuture().join().isEmpty());
    }
  }

  @Test
  void closingOneSessionDoesNotCancelAnotherSessionUsingTheSameProvider() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    byte[] bytes = { 1, 2, 3 };
    InstrumentLogos provider = logos(logo -> {
      started.countDown();
      await(release);
      return Optional.of(bytes);
    });
    try (LogoSession first = new DefaultLogoSession(provider); LogoSession second = new DefaultLogoSession(provider)) {
      try {
        var pending = second.load(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        first.close();
        assertFalse(pending.isDone());
        release.countDown();
        assertArrayEquals(bytes, pending.get(5, TimeUnit.SECONDS).orElseThrow());
        assertArrayEquals(bytes, second.load(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).orElseThrow());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void logoWorkRunsOnInjectedExecutorAndPropagatesBytesAndMissingImages() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    byte[] bytes = { 1, 2, 3 };
    InstrumentLogos provider = logos(logo -> {
      assertSame(LOGO, logo);
      return loads.getAndIncrement() == 0 ? Optional.of(bytes) : Optional.empty();
    });
    executor.submit(() -> {
      await(release);
    });
    try (DefaultLogoSession controller = new DefaultLogoSession(provider, executor)) {
      try {
        var pending = controller.load(LOGO).toCompletableFuture();
        assertFalse(pending.isDone());
        assertEquals(0, loads.get());
        release.countDown();
        assertArrayEquals(bytes, pending.get(5, TimeUnit.SECONDS).orElseThrow());
        assertTrue(controller.load(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void pendingAndFailedLogoDoesNotBlockOrInvalidateInstrumentAndHistoryLoads() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    LogoException failure = new LogoException(LogoException.Code.RATE_LIMITED, "slow down");
    InstrumentLogos provider = logos(logo -> {
      started.countDown();
      await(release);
      throw failure;
    });
    try (
      DefaultLogoSession controller = new DefaultLogoSession(provider);
      DefaultMarketDataSession marketData = new DefaultMarketDataSession(client(symbol -> DETAILS), "IBM")
    ) {
      try {
        var pending = controller.load(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertEquals("IBM", marketData.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        assertEquals("AAPL", marketData.loadInstrument("AAPL").toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        assertTrue(marketData.loadEarlier().toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
        assertFalse(pending.isDone());
        release.countDown();
        assertSame(failure, assertThrows(CompletionException.class, pending::join).getCause());
        assertEquals("AAPL", marketData.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void replacingLogoInterruptsThePreviousTaskAndCancelsItsStage() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    byte[] replacementBytes = { 4, 5, 6 };
    InstrumentLogos provider = logos(logo -> {
      if (loads.getAndIncrement() == 0) {
        return waitForLogoBody(started, interrupted, release);
      }
      return Optional.of(replacementBytes);
    });
    try (DefaultLogoSession controller = new DefaultLogoSession(provider)) {
      try {
        var previous = controller.load(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        var replacement = controller.load(LOGO).toCompletableFuture();
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        assertTrue(previous.isCancelled());
        assertThrows(CancellationException.class, previous::join);
        assertArrayEquals(replacementBytes, replacement.get(5, TimeUnit.SECONDS).orElseThrow());
        assertEquals(1, release.getCount(), "Replacement must not depend on releasing the old body");
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void explicitCancellationInterruptsRunningLogoAndAllowsAnotherLoad() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    InstrumentLogos provider = logos(logo -> {
      return loads.getAndIncrement() == 0 ? waitForLogoBody(started, interrupted, release) : Optional.empty();
    });
    try (DefaultLogoSession controller = new DefaultLogoSession(provider)) {
      try {
        controller.cancel();
        var pending = controller.load(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        controller.cancel();
        assertTrue(pending.isCancelled());
        assertThrows(CancellationException.class, pending::join);
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        controller.cancel();
        assertTrue(controller.load(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void cancellationBeforeExecutionPreventsTheTransportCall() throws Exception {
    CountDownLatch executorStarted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    var executor = Executors.newSingleThreadExecutor();
    InstrumentLogos provider = logos(logo -> {
      loads.incrementAndGet();
      return Optional.empty();
    });
    executor.submit(() -> {
      executorStarted.countDown();
      await(release);
    });
    try (DefaultLogoSession controller = new DefaultLogoSession(provider, executor)) {
      try {
        assertTrue(executorStarted.await(5, TimeUnit.SECONDS));
        var pending = controller.load(LOGO).toCompletableFuture();
        controller.cancel();
        assertTrue(pending.isCancelled());
        release.countDown();
        executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
        assertEquals(0, loads.get());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void shutdownInterruptsLogoBodyAndDoesNotWaitForItsRelease() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch interrupted = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    InstrumentLogos provider = logos(logo -> waitForLogoBody(started, interrupted, release));
    try (
      DefaultLogoSession controller = new DefaultLogoSession(provider);
      var closer = Executors.newSingleThreadExecutor()
    ) {
      try {
        var pending = controller.load(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        closer.submit(controller::close).get(2, TimeUnit.SECONDS);
        assertTrue(pending.isCancelled());
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        assertEquals(1, release.getCount(), "Shutdown must finish while the body is still blocked");
        assertThrows(RejectedExecutionException.class, () -> controller.load(LOGO));
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void programmingFailuresRemainExceptionalRatherThanBecomingMissingLogos() {
    for (Throwable failure : new Throwable[] {
      new IllegalStateException("broken implementation"),
      new AssertionError("broken invariant"),
    }) {
      InstrumentLogos provider = logos(logo -> {
        if (failure instanceof Error error) {
          throw error;
        }
        throw (RuntimeException) failure;
      });
      try (DefaultLogoSession controller = new DefaultLogoSession(provider)) {
        var pending = controller.load(LOGO).toCompletableFuture();
        assertSame(failure, assertThrows(CompletionException.class, pending::join).getCause());
      }
    }
  }

  private static Optional<byte[]> waitForLogoBody(
    CountDownLatch started,
    CountDownLatch interrupted,
    CountDownLatch release
  ) {
    started.countDown();
    try {
      release.await();
      return Optional.empty();
    } catch (InterruptedException exception) {
      interrupted.countDown();
      Thread.currentThread().interrupt();
      throw new LogoException(LogoException.Code.NETWORK, "Logo body interrupted", exception);
    }
  }

  private static InstrumentLogos logos(java.util.function.Function<InstrumentLogo, Optional<byte[]>> load) {
    return new InstrumentLogos() {
      public Optional<InstrumentLogo> findLogo(LogoRequest request) {
        return Optional.of(LOGO);
      }

      public Optional<byte[]> load(InstrumentLogo logo) {
        return load.apply(logo);
      }
    };
  }

  static MarketDataClient client(InstrumentDiscovery discovery) {
    return new MarketDataClient() {
      private final HistoricalBarData history = new HistoricalBarData() {
        public List<DailyBar> getDailyBars(DailyBarRequest request) {
          return List.of();
        }

        public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
          return List.of();
        }
      };

      public String provider() {
        return "some-provider";
      }

      public Optional<HistoricalBarData> historicalBars() {
        return Optional.of(history);
      }

      public Optional<InstrumentDiscovery> discovery() {
        return Optional.of(discovery);
      }
    };
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(5, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for test release");
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new AssertionError(exception);
    }
  }
}
