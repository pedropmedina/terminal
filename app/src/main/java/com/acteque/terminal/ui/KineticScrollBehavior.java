package com.acteque.terminal.ui;

import java.util.Objects;
import javafx.animation.AnimationTimer;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.input.ScrollEvent;

/** Adds velocity-based vertical scrolling to any node backed by a pixel scroll target. */
final class KineticScrollBehavior implements AutoCloseable {

  @FunctionalInterface
  interface PixelScrollTarget {
    /** Scrolls by the requested pixels and returns the distance actually moved. */
    double scrollBy(double requestedPixels);
  }

  private static final double MAX_FRAME_SECONDS = 0.05;
  private static final long PIXEL_INPUT_IDLE_NANOS = 32_000_000L;
  private static final long GESTURE_BREAK_NANOS = 120_000_000L;
  private static final double NOMINAL_INPUT_FRAME_SECONDS = 1.0 / 60.0;
  private static final double VELOCITY_SAMPLE_WEIGHT = 0.55;
  private static final double RELEASE_VELOCITY_MULTIPLIER = 1.50;
  private static final double MAX_MOMENTUM_PIXELS_PER_SECOND = 3_200.0;
  private static final double MIN_GLIDE_SECONDS = 0.36;
  private static final double GESTURE_TO_GLIDE_MULTIPLIER = 1.05;
  private static final double MAX_GLIDE_SECONDS = 1.20;
  private static final double STOP_VELOCITY_PIXELS_PER_SECOND = 12.0;

  private final Node eventSource;
  private final PixelScrollTarget scrollTarget;
  private final ReadOnlyBooleanWrapper gliding;
  private final AnimationTimer momentumAnimation;
  private final EventHandler<ScrollEvent> scrollHandler = event -> {
    if (event.getTextDeltaYUnits() != ScrollEvent.VerticalTextScrollUnits.PAGES) {
      handleScroll(event);
    }
  };

  private double pixelVelocity;
  private long gestureStartNanos;
  private long previousPixelInputNanos;
  private long lastPixelInputNanos;
  private long momentumStartNanos;
  private long previousMomentumFrameNanos;
  private double releaseVelocity;
  private double momentumDurationSeconds;
  private boolean momentumAnimationRunning;

  KineticScrollBehavior(Node eventSource, PixelScrollTarget scrollTarget, ReadOnlyBooleanWrapper gliding) {
    this.eventSource = Objects.requireNonNull(eventSource, "eventSource");
    this.scrollTarget = Objects.requireNonNull(scrollTarget, "scrollTarget");
    this.gliding = Objects.requireNonNull(gliding, "gliding");
    momentumAnimation = new AnimationTimer() {
      @Override
      public void handle(long now) {
        advanceMomentum(now);
      }
    };
    eventSource.addEventFilter(ScrollEvent.SCROLL, scrollHandler);
  }

  private void advanceMomentum(long now) {
    if (System.nanoTime() - lastPixelInputNanos < PIXEL_INPUT_IDLE_NANOS) {
      return;
    }
    if (momentumStartNanos == 0L) {
      double gestureSeconds = Math.max(
        NOMINAL_INPUT_FRAME_SECONDS,
        (lastPixelInputNanos - gestureStartNanos) / 1_000_000_000.0
      );
      momentumDurationSeconds = Math.min(
        MAX_GLIDE_SECONDS,
        MIN_GLIDE_SECONDS + gestureSeconds * GESTURE_TO_GLIDE_MULTIPLIER
      );
      releaseVelocity = clampMomentumVelocity(pixelVelocity * RELEASE_VELOCITY_MULTIPLIER);
      momentumStartNanos = now;
      previousMomentumFrameNanos = now;
      if (Math.abs(releaseVelocity) < STOP_VELOCITY_PIXELS_PER_SECOND) {
        stopMomentumAnimation();
      } else {
        gliding.set(true);
      }
      return;
    }

    double momentumSeconds = (now - momentumStartNanos) / 1_000_000_000.0;
    if (momentumSeconds >= momentumDurationSeconds) {
      stopMomentumAnimation();
      return;
    }
    double elapsedSeconds = Math.min((now - previousMomentumFrameNanos) / 1_000_000_000.0, MAX_FRAME_SECONDS);
    previousMomentumFrameNanos = now;
    double remainingFraction = 1.0 - momentumSeconds / momentumDurationSeconds;
    double movedPixels = scrollTarget.scrollBy(releaseVelocity * remainingFraction * elapsedSeconds);

    if (movedPixels == 0.0) {
      stopMomentumAnimation();
    }
  }

  private void handleScroll(ScrollEvent event) {
    double requestedPixels = -event.getDeltaY();
    if (requestedPixels == 0.0) {
      return;
    }

    if (event.isInertia()) {
      event.consume();
      return;
    }

    long now = System.nanoTime();
    gliding.set(false);
    updatePixelVelocity(requestedPixels, now);
    previousPixelInputNanos = now;
    lastPixelInputNanos = now;
    momentumStartNanos = 0L;
    previousMomentumFrameNanos = 0L;
    scrollTarget.scrollBy(requestedPixels);
    startMomentumAnimation();
    event.consume();
  }

  private void updatePixelVelocity(double pixels, long now) {
    if (previousPixelInputNanos == 0L || now - previousPixelInputNanos > GESTURE_BREAK_NANOS) {
      gestureStartNanos = now;
      pixelVelocity = clampMomentumVelocity(pixels / NOMINAL_INPUT_FRAME_SECONDS);
      return;
    }

    double elapsedSeconds = (now - previousPixelInputNanos) / 1_000_000_000.0;
    if (elapsedSeconds <= 0.0) {
      return;
    }

    double sampledVelocity = pixels / elapsedSeconds;
    if (pixelVelocity != 0.0 && Math.signum(pixelVelocity) != Math.signum(sampledVelocity)) {
      pixelVelocity = sampledVelocity;
    } else {
      pixelVelocity += (sampledVelocity - pixelVelocity) * VELOCITY_SAMPLE_WEIGHT;
    }
    pixelVelocity = clampMomentumVelocity(pixelVelocity);
  }

  private double clampMomentumVelocity(double velocity) {
    return Math.max(-MAX_MOMENTUM_PIXELS_PER_SECOND, Math.min(MAX_MOMENTUM_PIXELS_PER_SECOND, velocity));
  }

  private void startMomentumAnimation() {
    if (!momentumAnimationRunning) {
      momentumAnimationRunning = true;
      momentumAnimation.start();
    }
  }

  private void stopMomentumAnimation() {
    momentumAnimation.stop();
    momentumAnimationRunning = false;
    pixelVelocity = 0.0;
    gestureStartNanos = 0L;
    previousPixelInputNanos = 0L;
    lastPixelInputNanos = 0L;
    momentumStartNanos = 0L;
    previousMomentumFrameNanos = 0L;
    releaseVelocity = 0.0;
    momentumDurationSeconds = 0.0;
    gliding.set(false);
  }

  @Override
  public void close() {
    stopMomentumAnimation();
    eventSource.removeEventFilter(ScrollEvent.SCROLL, scrollHandler);
  }
}
