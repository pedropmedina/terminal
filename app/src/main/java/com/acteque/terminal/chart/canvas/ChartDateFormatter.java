package com.acteque.terminal.chart.canvas;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

/** Date presentation used by the chart canvas. */
final class ChartDateFormatter {

  private static final DateTimeFormatter MONTH = DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("d", Locale.US);
  private static final DateTimeFormatter YEAR = DateTimeFormatter.ofPattern("yyyy", Locale.US);
  private static final DateTimeFormatter CROSSHAIR = DateTimeFormatter.ofPattern("EEE MMM dd, yyyy", Locale.US);
  private static final DateTimeFormatter INTRADAY_CROSSHAIR = DateTimeFormatter.ofPattern(
    "MMM d, yyyy h:mm a",
    Locale.US
  );
  private static final ZoneId MARKET_ZONE = ZoneId.of("America/New_York");

  /** Prevents utility-class instantiation. */
  private ChartDateFormatter() {}

  /**
   * @param date the date to format
   * @return its abbreviated month
   */
  static String month(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(MONTH);
  }

  /**
   * @param date the date to format
   * @return its unpadded day of month
   */
  static String day(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(DAY);
  }

  /**
   * @param date the date to format
   * @return its four-digit year
   */
  static String year(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(YEAR);
  }

  /**
   * @param date the date to format
   * @return its calendar crosshair label
   */
  static String crosshair(LocalDate date) {
    return Objects.requireNonNull(date, "date cannot be null").format(CROSSHAIR);
  }

  /**
   * @param timestamp the instant to format
   * @return its New York intraday crosshair label
   */
  static String crosshair(Instant timestamp) {
    return Objects.requireNonNull(timestamp, "timestamp cannot be null").atZone(MARKET_ZONE).format(INTRADAY_CROSSHAIR);
  }
}
