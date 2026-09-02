package com.acteque.terminal.chart;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public enum ChartInterval {
  ONE_TICK("1T", "Ticks", "1 tick interval", 56.0),
  TEN_TICKS("10T", "Ticks", "10 tick interval", 56.0),
  ONE_HUNDRED_TICKS("100T", "Ticks", "100 tick interval", 56.0),
  ONE_THOUSAND_TICKS("1000T", "Ticks", "1000 tick interval", 56.0),
  ONE_SECOND("1S", "Seconds", "1 second interval", 56.0),
  FIVE_SECONDS("5S", "Seconds", "5 second interval", 56.0),
  TEN_SECONDS("10S", "Seconds", "10 second interval", 56.0),
  FIFTEEN_SECONDS("15S", "Seconds", "15 second interval", 56.0),
  THIRTY_SECONDS("30S", "Seconds", "30 second interval", 56.0),
  FORTY_FIVE_SECONDS("45S", "Seconds", "45 second interval", 56.0),
  ONE_MINUTE("1M", "Minutes", "1 minute interval", 56.0),
  TWO_MINUTES("2M", "Minutes", "2 minute interval", 56.0),
  FIVE_MINUTES("5M", "Minutes", "5 minute interval", 56.0),
  TEN_MINUTES("10M", "Minutes", "10 minute interval", 56.0),
  FIFTEEN_MINUTES("15M", "Minutes", "15 minute interval", 56.0),
  THIRTY_MINUTES("30M", "Minutes", "30 minute interval", 56.0),
  FORTY_FIVE_MINUTES("45M", "Minutes", "45 minute interval", 56.0),
  ONE_HOUR("1H", "Hours", "1 hour interval", 56.0),
  TWO_HOURS("2H", "Hours", "2 hour interval", 56.0),
  THREE_HOURS("3H", "Hours", "3 hour interval", 56.0),
  FOUR_HOURS("4H", "Hours", "4 hour interval", 56.0),
  DAILY("1D", "Days", "1 day interval", 56.0),
  WEEKLY("1W", "Days", "1 week interval", 56.0),
  MONTHLY("1Mo", "Days", "1 month interval", 56.0),
  THREE_MONTHS("3Mo", "Days", "3 month interval", 56.0),
  SIX_MONTHS("6Mo", "Days", "6 month interval", 56.0),
  TWELVE_MONTHS("12Mo", "Days", "12 month interval", 56.0);

  private static final DateTimeFormatter MONTH_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MMM", Locale.US);
  private static final DateTimeFormatter DAY_LABEL_FORMATTER = DateTimeFormatter.ofPattern("d", Locale.US);
  private static final DateTimeFormatter YEAR_LABEL_FORMATTER = DateTimeFormatter.ofPattern("yyyy", Locale.US);
  private static final DateTimeFormatter CROSSHAIR_LABEL_FORMATTER = DateTimeFormatter.ofPattern(
    "EEE MMM dd, yyyy",
    Locale.US
  );

  private final String displayName;
  private final String category;
  private final String description;
  private final double minimumLabelSpacing;

  ChartInterval(String displayName, String category, String description, double minimumLabelSpacing) {
    this.displayName = displayName;
    this.category = category;
    this.description = description;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  String displayName() {
    return displayName;
  }

  String category() {
    return category;
  }

  String description() {
    return description;
  }

  boolean matches(String normalizedQuery) {
    return (
      normalizedQuery.isEmpty() ||
      displayName.toLowerCase(Locale.ROOT).equals(normalizedQuery) ||
      category.toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
      description.toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
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
