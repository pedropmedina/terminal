package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Clock;
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

class MarketDataControllerLogoTest {

  private static final InstrumentLogo LOGO = LogoMarketDataClientTest.LOGO;
  private static final InstrumentDetails DETAILS = LogoMarketDataClientTest.DETAILS;

  @Test
  void retainsActualDetailsAndDoesNotDownloadDuringChartLoad() throws Exception {
    AtomicInteger loads = new AtomicInteger();
    InstrumentLogos logos = logos(logo -> {
      loads.incrementAndGet();
      return Optional.empty();
    });
    MarketDataClient client = new LogoMarketDataClient(LogoMarketDataClientTest.client(symbol -> DETAILS), logos);
    try (MarketDataController controller = new MarketDataController(client, "IBM")) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals("IBM", loaded.symbol());
      assertEquals("Name", loaded.displayName());
      assertEquals(DETAILS.withLogo(Optional.of(LOGO)), loaded.details());
      assertEquals(0, loads.get());
    }
  }

  @Test
  void metadataFailureProducesFallbackDetailsAndLegacyConstructorStillWorks() throws Exception {
    MarketDataClient client = LogoMarketDataClientTest.client(symbol -> {
      throw new MarketDataException(MarketDataException.Code.NETWORK, "offline");
    });
    try (MarketDataController controller = new MarketDataController(client, "IBM")) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals(
        new InstrumentDetails("IBM", Optional.empty(), Optional.empty(), Optional.empty()),
        loaded.details()
      );
      assertEquals("IBM", loaded.displayName());
      assertTrue(controller.loadLogo(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
    }
    var legacy = new MarketDataController.LoadedInstrument("IBM", "IBM name", List.of());
    assertEquals(Optional.of("IBM name"), legacy.details().name());
    assertTrue(legacy.details().logo().isEmpty());
  }

  @Test
  void logoWorkRunsOnInjectedExecutorAndPropagatesBytesAndMissingImages() throws Exception {
    var executor = Executors.newSingleThreadExecutor();
    CountDownLatch release = new CountDownLatch(1);
    AtomicInteger loads = new AtomicInteger();
    byte[] bytes = { 1, 2, 3 };
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> {
        assertSame(LOGO, logo);
        return loads.getAndIncrement() == 0 ? Optional.of(bytes) : Optional.empty();
      })
    );
    executor.submit(() -> {
      await(release);
    });
    try (MarketDataController controller = new MarketDataController(client, "IBM", Clock.systemUTC(), executor)) {
      try {
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
        assertFalse(pending.isDone());
        assertEquals(0, loads.get());
        release.countDown();
        assertArrayEquals(bytes, pending.get(5, TimeUnit.SECONDS).orElseThrow());
        assertTrue(controller.loadLogo(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
      } finally {
        release.countDown();
      }
    }
  }

  @Test
  void pendingAndFailedLogoDoesNotBlockOrInvalidateInstrumentAndHistoryLoads() throws Exception {
    CountDownLatch started = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    MarketDataException failure = new MarketDataException(MarketDataException.Code.RATE_LIMITED, "slow down");
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> {
        started.countDown();
        await(release);
        throw failure;
      })
    );
    try (MarketDataController controller = new MarketDataController(client, "IBM")) {
      try {
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        assertEquals("IBM", controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        assertEquals("AAPL", controller.loadInstrument("AAPL").toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        assertTrue(controller.loadEarlier().toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
        assertFalse(pending.isDone());
        release.countDown();
        assertSame(failure, assertThrows(CompletionException.class, pending::join).getCause());
        assertEquals("AAPL", controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
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
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> {
        if (loads.getAndIncrement() == 0) {
          return waitForLogoBody(started, interrupted, release);
        }
        return Optional.of(replacementBytes);
      })
    );
    try (MarketDataController controller = new MarketDataController(client, "IBM")) {
      try {
        var previous = controller.loadLogo(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        var replacement = controller.loadLogo(LOGO).toCompletableFuture();
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
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> {
        return loads.getAndIncrement() == 0 ? waitForLogoBody(started, interrupted, release) : Optional.empty();
      })
    );
    try (MarketDataController controller = new MarketDataController(client, "IBM")) {
      try {
        controller.cancelLogoLoad();
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        controller.cancelLogoLoad();
        assertTrue(pending.isCancelled());
        assertThrows(CancellationException.class, pending::join);
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        controller.cancelLogoLoad();
        assertTrue(controller.loadLogo(LOGO).toCompletableFuture().get(5, TimeUnit.SECONDS).isEmpty());
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
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> {
        loads.incrementAndGet();
        return Optional.empty();
      })
    );
    executor.submit(() -> {
      executorStarted.countDown();
      await(release);
    });
    try (MarketDataController controller = new MarketDataController(client, "IBM", Clock.systemUTC(), executor)) {
      try {
        assertTrue(executorStarted.await(5, TimeUnit.SECONDS));
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
        controller.cancelLogoLoad();
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
    MarketDataClient client = new LogoMarketDataClient(
      LogoMarketDataClientTest.client(symbol -> DETAILS),
      logos(logo -> waitForLogoBody(started, interrupted, release))
    );
    try (
      MarketDataController controller = new MarketDataController(client, "IBM");
      var closer = Executors.newSingleThreadExecutor()
    ) {
      try {
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
        assertTrue(started.await(5, TimeUnit.SECONDS));
        closer.submit(controller::close).get(2, TimeUnit.SECONDS);
        assertTrue(pending.isCancelled());
        assertTrue(interrupted.await(5, TimeUnit.SECONDS));
        assertEquals(1, release.getCount(), "Shutdown must finish while the body is still blocked");
        assertThrows(RejectedExecutionException.class, () -> controller.loadLogo(LOGO));
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
      MarketDataClient client = new LogoMarketDataClient(
        LogoMarketDataClientTest.client(symbol -> DETAILS),
        logos(logo -> {
          if (failure instanceof Error error) {
            throw error;
          }
          throw (RuntimeException) failure;
        })
      );
      try (MarketDataController controller = new MarketDataController(client, "IBM")) {
        var pending = controller.loadLogo(LOGO).toCompletableFuture();
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
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Logo body interrupted", exception);
    }
  }

  private static InstrumentLogos logos(java.util.function.Function<InstrumentLogo, Optional<byte[]>> load) {
    return new InstrumentLogos() {
      public Optional<InstrumentLogo> findLogo(InstrumentDetails details) {
        return Optional.of(LOGO);
      }

      public Optional<byte[]> load(InstrumentLogo logo) {
        return load.apply(logo);
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
