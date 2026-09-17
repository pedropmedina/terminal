package com.acteque.terminal.test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;

/** Runs JavaFX assertions on the application thread without an external UI-test dependency. */
public final class FxTestSupport {

  private static final long TIMEOUT_SECONDS = 10;
  private static boolean started;

  private FxTestSupport() {}

  public static void runAndWait(Runnable action) {
    ensureStarted();
    AtomicReference<Throwable> failure = new AtomicReference<>();
    CountDownLatch completed = new CountDownLatch(1);
    Platform.runLater(() -> {
      try {
        action.run();
      } catch (Throwable throwable) {
        failure.set(throwable);
      } finally {
        completed.countDown();
      }
    });
    await(completed);
    if (failure.get() != null) {
      throw new AssertionError("JavaFX test action failed", failure.get());
    }
  }

  private static synchronized void ensureStarted() {
    if (started) {
      return;
    }
    CountDownLatch initialized = new CountDownLatch(1);
    Platform.startup(initialized::countDown);
    await(initialized);
    started = true;
  }

  private static void await(CountDownLatch latch) {
    try {
      if (!latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        throw new AssertionError("Timed out waiting for the JavaFX application thread");
      }
    } catch (InterruptedException failure) {
      Thread.currentThread().interrupt();
      throw new AssertionError("Interrupted while waiting for the JavaFX application thread", failure);
    }
  }
}
