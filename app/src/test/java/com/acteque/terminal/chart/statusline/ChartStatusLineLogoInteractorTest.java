package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketlogos.InstrumentLogo;
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

class ChartStatusLineLogoInteractorTest {

  private static final InstrumentLogo FIRST = logo("FIRST");
  private static final InstrumentLogo SECOND = logo("SECOND");
  private static final byte[] PNG = Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
  );

  @Test
  void decodesLogoAndPublishesItThroughTheModelOnTheUiExecutor() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      List<Runnable> uiQueue = new ArrayList<>();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(
        model,
        reference -> CompletableFuture.completedFuture(Optional.of(PNG)),
        uiQueue::add
      );

      interactor.setInstrument("First", Optional.of(FIRST));
      assertNull(model.getLogoState());
      uiQueue.removeFirst().run();

      assertSame(FIRST, model.getLogoState().logo());
      assertFalse(model.getLogoState().image().isError());
      assertTrue(model.getLogoState().image().getWidth() > 0);
    });
  }

  @Test
  void ignoresOlderRequestsAndQueuedCompletions() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      List<CompletableFuture<Optional<byte[]>>> requests = new ArrayList<>();
      List<Runnable> uiQueue = new ArrayList<>();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(
        model,
        reference -> {
          var request = new CompletableFuture<Optional<byte[]>>();
          requests.add(request);
          return request;
        },
        uiQueue::add
      );

      interactor.setInstrument("First", Optional.of(FIRST));
      interactor.setInstrument("Second", Optional.of(SECOND));
      requests.get(1).complete(Optional.of(PNG));
      interactor.setInstrument("First again", Optional.of(FIRST));
      requests.get(0).complete(Optional.of(PNG));
      requests.get(2).complete(Optional.of(PNG));
      uiQueue.forEach(Runnable::run);

      assertSame(FIRST, model.getLogoState().logo());
    });
  }

  @Test
  void cancellationAndMissingMetadataLeaveTheFallbackState() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      var pending = new CompletableFuture<Optional<byte[]>>();
      AtomicInteger cancellations = new AtomicInteger();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(
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

      interactor.setInstrument("First", Optional.of(FIRST));
      interactor.cancelLogoLoad();
      pending.complete(Optional.of(PNG));
      assertNull(model.getLogoState());
      assertTrue(cancellations.get() >= 2);

      interactor.setInstrument("No logo", Optional.empty());
      assertNull(model.getLogoState());
    });
  }

  @Test
  void missingInvalidAndFailedDownloadsLeaveTheFallbackState() {
    FxTestSupport.runAndWait(() -> {
      List<CompletableFuture<Optional<byte[]>>> results = List.of(
        CompletableFuture.completedFuture(Optional.empty()),
        CompletableFuture.completedFuture(Optional.of(new byte[] { 1, 2, 3 })),
        CompletableFuture.failedFuture(new IllegalStateException("Test download failure"))
      );
      for (var result : results) {
        ChartStatusLineModel model = new ChartStatusLineModel();
        ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model, ignored -> result, Runnable::run);
        interactor.setInstrument("First", Optional.of(FIRST));
        assertNull(model.getLogoState());
      }
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
