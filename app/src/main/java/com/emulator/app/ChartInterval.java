package com.emulator.app;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

enum ChartInterval {
  DAILY(DateTimeFormatter.ofPattern("MM/dd"), 56.0);

  private final DateTimeFormatter labelFormatter;
  private final double minimumLabelSpacing;

  ChartInterval(DateTimeFormatter labelFormatter, double minimumLabelSpacing) {
    this.labelFormatter = labelFormatter;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  String format(LocalDate date) {
    return date.format(labelFormatter);
  }

  double minimumLabelSpacing() {
    return minimumLabelSpacing;
  }
}
