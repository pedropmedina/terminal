package com.acteque.terminal.chartworkspace;

import java.util.Collection;
import java.util.Objects;
import java.util.random.RandomGenerator;
import javafx.scene.paint.Color;

/** Generates visually separated chart-identifier colors within a fixed saturation and brightness range. */
final class ChartIdentifierColorGenerator {

  private static final int CANDIDATE_COUNT = 24;
  private static final double SATURATION = 0.72;
  private static final double BRIGHTNESS = 0.85;
  private static final double FULL_HUE_RANGE = 360.0;

  private final RandomGenerator random;

  /**
   * Creates a generator backed by the supplied random source.
   *
   * @param random the source used to sample candidate hues
   */
  ChartIdentifierColorGenerator(RandomGenerator random) {
    this.random = Objects.requireNonNull(random, "random cannot be null");
  }

  /**
   * Selects the sampled hue with the greatest minimum distance from active colors.
   *
   * @param activeColors the colors already assigned within the workspace
   * @return a new identifier color
   */
  Color next(Collection<Color> activeColors) {
    Objects.requireNonNull(activeColors, "activeColors cannot be null");

    double selectedHue = random.nextDouble(FULL_HUE_RANGE);
    double selectedDistance = minimumHueDistance(selectedHue, activeColors);
    for (int candidate = 1; candidate < CANDIDATE_COUNT; candidate++) {
      double hue = random.nextDouble(FULL_HUE_RANGE);
      double distance = minimumHueDistance(hue, activeColors);
      if (distance > selectedDistance) {
        selectedHue = hue;
        selectedDistance = distance;
      }
    }
    return Color.hsb(selectedHue, SATURATION, BRIGHTNESS);
  }

  /**
   * Computes the shortest circular hue distance from a candidate to existing colors.
   *
   * @param hue the candidate hue in degrees
   * @param colors the existing identifier colors
   * @return the minimum circular distance in degrees
   */
  private static double minimumHueDistance(double hue, Collection<Color> colors) {
    double distance = FULL_HUE_RANGE;
    for (Color color : colors) {
      double difference = Math.abs(hue - color.getHue());
      distance = Math.min(distance, Math.min(difference, FULL_HUE_RANGE - difference));
    }
    return distance;
  }
}
