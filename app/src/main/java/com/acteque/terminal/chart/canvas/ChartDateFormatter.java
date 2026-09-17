package com.acteque.terminal.chart.canvas;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Date presentation used by the chart canvas. */
final class ChartDateFormatter {

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.US);
  private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy", Locale.US);
  private static final DateTimeFormatter CROSSHAIR = DateTimeFormatter.ofPattern("EEE MMM dd, yyyy", Locale.US);

  private ChartDateFormatter() {}

  static String month(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(MONTH);
  }

  static String day(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(DAY);
  }

  static String year(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(YEAR);
  }

  static String crosshair(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(CROSSHAIR);
  }
}
