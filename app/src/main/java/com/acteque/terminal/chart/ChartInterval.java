package com.acteque.terminal.chart;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum ChartInterval {
  DAILY("1D", 56.0);

  private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("d", Locale.US);
  private static final DateTimeFormatter YEAR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy", Locale.US);
  private static final DateTimeFormatter CROSSHAIR_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
    "EEE MMM dd, yyyy",
    Locale.US
  );

  private final String displayName;
  private final double minimumLabelSpacing;

  ChartInterval(String displayName, double minimumLabelSpacing) {
    this.displayName = displayName;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  String displayName() {
    return displayName;
  }

  String formatMonth(LocalDate date) {
    return date.format(MONTH_LABEL_FORMATTER);
  }

  String formatDay(LocalDate date) {
    return date.format(DAY_LABEL_FORMATTER);
  }

  String formatYear(LocalDate date) {
    return date.format(YEAR_LABEL_FORMATTER);
  }

  String formatCrosshair(LocalDate date) {
    return date.format(CROSSHAIR_LABEL_FORMATTER);
  }

  double minimumLabelSpacing() {
    return minimumLabelSpacing;
  }
}
