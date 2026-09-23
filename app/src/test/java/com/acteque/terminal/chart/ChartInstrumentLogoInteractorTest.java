package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.test.FxTestSupport;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChartInstrumentLogoInteractorTest {

  private static final InstrumentLogo FIRST = logo("FIRST");
  private static final InstrumentLogo SECOND = logo("SECOND");
  private static final byte[] PNG = Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
  );

  @Test
  void decodesLogoAndPublishesItThroughTheChartModelOnTheUiExecutor() {
    FxTestSupport.runAndWait(() -> {
      ChartModel model = new ChartModel();
      List<Runnable> uiQueue = new ArrayList<>();
      ChartInteractor interactor = new ChartInteractor(
        model,
        reference -> CompletableFuture.completedFuture(Optional.of(PNG)),
        uiQueue::add
      );

      interactor.setInstrumentLogo(Optional.of(FIRST));
      assertNull(model.getInstrumentLogoImage());
      uiQueue.removeFirst().run();

      assertNotNull(model.getInstrumentLogoImage());
      assertFalse(model.getInstrumentLogoImage().isError());
      assertTrue(model.getInstrumentLogoImage().getWidth() > 0);
    });
  }

  @Test
  void ignoresOlderRequestsAndQueuedCompletions() {
    FxTestSupport.runAndWait(() -> {
      ChartModel model = new ChartModel();
      List<CompletableFuture<Optional<byte[]>>> requests = new ArrayList<>();
      List<Runnable> uiQueue = new ArrayList<>();
      ChartInteractor interactor = new ChartInteractor(
        model,
        reference -> {
          var request = new CompletableFuture<Optional<byte[]>>();
          requests.add(request);
          return request;
        },
        uiQueue::add
      );

      interactor.setInstrumentLogo(Optional.of(FIRST));
      interactor.setInstrumentLogo(Optional.of(SECOND));
      requests.get(1).complete(Optional.of(PNG));
      interactor.setInstrumentLogo(Optional.of(FIRST));
      uiQueue.removeFirst().run();
      assertNull(model.getInstrumentLogoImage());

      requests.get(0).complete(Optional.of(PNG));
      uiQueue.removeFirst().run();
      assertNull(model.getInstrumentLogoImage());

      requests.get(2).complete(Optional.of(PNG));
      uiQueue.removeFirst().run();
      assertNotNull(model.getInstrumentLogoImage());
    });
  }

  @Test
  void cancellationAndMissingMetadataLeaveTheSymbolFallbackState() {
    FxTestSupport.runAndWait(() -> {
      ChartModel model = new ChartModel();
      var pending = new CompletableFuture<Optional<byte[]>>();
      AtomicInteger cancellations = new AtomicInteger();
      ChartInteractor interactor = new ChartInteractor(
        model,
        new LogoSession() {
          @Override
          public CompletableFuture<Optional<byte[]>> load(InstrumentLogo logo) {
            return pending;
          }

          @Override
          public void cancel() {
            cancellations.incrementAndGet();
          }
        },
        Runnable::run
      );

      interactor.setInstrumentLogo(Optional.of(FIRST));
      interactor.cancelInstrumentLogoLoad();
      pending.complete(Optional.of(PNG));
      assertNull(model.getInstrumentLogoImage());
      assertTrue(cancellations.get() >= 2);

      interactor.setInstrumentLogo(Optional.empty());
      assertNull(model.getInstrumentLogoImage());
    });
  }

  @Test
  void missingInvalidAndFailedDownloadsLeaveTheSymbolFallbackState() {
    FxTestSupport.runAndWait(() -> {
      List<CompletableFuture<Optional<byte[]>>> results = List.of(
        CompletableFuture.completedFuture(Optional.empty()),
        CompletableFuture.completedFuture(Optional.of(new byte[] { 1, 2, 3 })),
        CompletableFuture.failedFuture(new LogoException(LogoException.Code.RATE_LIMITED, "HTTP 429")),
        CompletableFuture.failedFuture(new IllegalStateException("Test download failure"))
      );
      for (var result : results) {
        ChartModel model = new ChartModel();
        ChartInteractor interactor = new ChartInteractor(model, ignored -> result, Runnable::run);
        interactor.setInstrumentLogo(Optional.of(FIRST));
        assertNull(model.getInstrumentLogoImage());
      }
    });
  }

  @Test
  void synchronousRateLimitFailuresLeaveTheSymbolFallbackWithoutEscaping() {
    FxTestSupport.runAndWait(() -> {
      ChartModel model = new ChartModel();
      ChartInteractor interactor = new ChartInteractor(
        model,
        ignored -> {
          throw new LogoException(LogoException.Code.RATE_LIMITED, "HTTP 429");
        },
        Runnable::run
      );

      assertDoesNotThrow(() -> interactor.setInstrumentLogo(Optional.of(FIRST)));
      assertNull(model.getInstrumentLogoImage());
    });
  }

  private static InstrumentLogo logo(String symbol) {
    return new InstrumentLogo(
      URI.create("https://images.example.com/" + symbol + ".png"),
      "Logos by Example",
      URI.create("https://example.com")
    );
  }
}
