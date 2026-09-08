package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.test.FxTestSupport;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.image.Image;
import org.junit.jupiter.api.Test;

class ChartLogoLoaderTest {

  private static final InstrumentLogo FIRST = logo("FIRST");
  private static final InstrumentLogo SECOND = logo("SECOND");
  private static final byte[] PNG = Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
  );

  @Test
  void decodesLogoAndDispatchesItsMatchingAttributionToTheUi() {
    FxTestSupport.runAndWait(() -> {
      List<Runnable> uiQueue = new ArrayList<>();
      List<Image> images = new ArrayList<>();
      ChartLogoLoader loader = new ChartLogoLoader(reference -> {
        assertSame(FIRST, reference);
        return CompletableFuture.completedFuture(Optional.of(PNG));
      }, uiQueue::add);

      loader.load(Optional.of(FIRST), (reference, image) -> {
        assertSame(FIRST, reference);
        images.add(image);
      });
      assertTrue(images.isEmpty());
      uiQueue.removeFirst().run();
      assertEquals(1, images.size());
      assertFalse(images.getFirst().isError());
      assertTrue(images.getFirst().getWidth() > 0);
    });
  }

  @Test
  void ignoresSlowOlderRequestsAndAlreadyQueuedCompletionsEvenWhenReturningToTheSameSymbol() {
    FxTestSupport.runAndWait(() -> {
      List<CompletableFuture<Optional<byte[]>>> requests = new ArrayList<>();
      List<Runnable> uiQueue = new ArrayList<>();
      List<InstrumentLogo> displayed = new ArrayList<>();
      ChartLogoLoader loader = new ChartLogoLoader(reference -> {
        var request = new CompletableFuture<Optional<byte[]>>();
        requests.add(request);
        return request;
      }, uiQueue::add);

      loader.load(Optional.of(FIRST), (reference, image) -> displayed.add(reference));
      loader.load(Optional.of(SECOND), (reference, image) -> displayed.add(reference));
      requests.get(1).complete(Optional.of(PNG));
      loader.load(Optional.of(FIRST), (reference, image) -> displayed.add(reference));
      requests.get(0).complete(Optional.of(PNG));
      requests.get(2).complete(Optional.of(PNG));
      uiQueue.forEach(Runnable::run);

      assertEquals(List.of(FIRST), displayed);
    });
  }

  @Test
  void cancelInvalidatesCallbacksQueuedBeforeNewPriceLoadsOrShutdown() {
    FxTestSupport.runAndWait(() -> {
      List<Runnable> uiQueue = new ArrayList<>();
      AtomicBoolean displayed = new AtomicBoolean();
      ChartLogoLoader loader = new ChartLogoLoader(
        reference -> CompletableFuture.completedFuture(Optional.of(PNG)),
        uiQueue::add
      );
      loader.load(Optional.of(FIRST), (reference, image) -> displayed.set(true));
      loader.cancel();
      uiQueue.forEach(Runnable::run);
      assertFalse(displayed.get());
    });
  }

  @Test
  void missingMetadataDoesNotDownloadAndInvalidatesThePreviousSelection() {
    FxTestSupport.runAndWait(() -> {
      var pending = new CompletableFuture<Optional<byte[]>>();
      List<InstrumentLogo> requests = new ArrayList<>();
      AtomicBoolean displayed = new AtomicBoolean();
      ChartLogoLoader loader = new ChartLogoLoader(reference -> {
        requests.add(reference);
        return pending;
      }, Runnable::run);
      loader.load(Optional.of(FIRST), (reference, image) -> displayed.set(true));
      loader.load(Optional.empty(), (reference, image) -> displayed.set(true));
      pending.complete(Optional.of(PNG));
      assertEquals(List.of(FIRST), requests);
      assertFalse(displayed.get());
    });
  }

  @Test
  void missingInvalidAndFailedDownloadsLeaveTheFallbackAlone() {
    FxTestSupport.runAndWait(() -> {
      List<CompletableFuture<Optional<byte[]>>> results = List.of(
        CompletableFuture.completedFuture(Optional.empty()),
        CompletableFuture.completedFuture(Optional.of(new byte[] { 1, 2, 3 })),
        CompletableFuture.failedFuture(new IllegalStateException("Test download failure"))
      );
      for (var result : results) {
        AtomicBoolean displayed = new AtomicBoolean();
        ChartLogoLoader loader = new ChartLogoLoader(reference -> result, Runnable::run);
        loader.load(Optional.of(FIRST), (reference, image) -> displayed.set(true));
        assertFalse(displayed.get());
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
