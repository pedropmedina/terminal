package com.acteque.terminal;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

enum ChartInterval {
  DAILY("1D", DateTimeFormatter.ofPattern("MM/dd"), 56.0);

  private static final DateTimeFormatter CROSSHAIR_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
    "EEE MMM dd, yyyy",
    Locale.US
  );

  private final String displayName;
  private final DateTimeFormatter labelFormatter;
  private final double minimumLabelSpacing;

  ChartInterval(String displayName, DateTimeFormatter labelFormatter, double minimumLabelSpacing) {
    this.displayName = displayName;
    this.labelFormatter = labelFormatter;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  String displayName() {
    return displayName;
  }

  String format(LocalDate date) {
    return date.format(labelFormatter);
  }

  String formatCrosshair(LocalDate date) {
    return date.format(CROSSHAIR_LABEL_FORMATTER);
  }

  double minimumLabelSpacing() {
    return minimumLabelSpacing;
  }
}
