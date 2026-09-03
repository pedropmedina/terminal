package com.acteque.terminal.chart;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ChartInterval {

  public enum Classification {
    TICKS("Ticks", "Ticks", "T", "tick"),
    SECONDS("Seconds", "Seconds", "S", "second"),
    MINUTES("Minutes", "Minutes", "M", "minute"),
    HOURS("Hours", "Hours", "H", "hour"),
    DAYS("Days", "Days", "D", "day"),
    WEEKS("Weeks", "Days", "W", "week"),
    MONTHS("Months", "Days", "Mo", "month");

    private final String label;
    private final String category;
    private final String suffix;
    private final String unit;

    Classification(String label, String category, String suffix, String unit) {
      this.label = label;
      this.category = category;
      this.suffix = suffix;
      this.unit = unit;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  public static final ChartInterval ONE_TICK = standard("1T", "Ticks", "1 tick interval");
  public static final ChartInterval TEN_TICKS = standard("10T", "Ticks", "10 tick interval");
  public static final ChartInterval ONE_HUNDRED_TICKS = standard("100T", "Ticks", "100 tick interval");
  public static final ChartInterval ONE_THOUSAND_TICKS = standard("1000T", "Ticks", "1000 tick interval");
  public static final ChartInterval ONE_SECOND = standard("1S", "Seconds", "1 second interval");
  public static final ChartInterval FIVE_SECONDS = standard("5S", "Seconds", "5 second interval");
  public static final ChartInterval TEN_SECONDS = standard("10S", "Seconds", "10 second interval");
  public static final ChartInterval FIFTEEN_SECONDS = standard("15S", "Seconds", "15 second interval");
  public static final ChartInterval THIRTY_SECONDS = standard("30S", "Seconds", "30 second interval");
  public static final ChartInterval FORTY_FIVE_SECONDS = standard("45S", "Seconds", "45 second interval");
  public static final ChartInterval ONE_MINUTE = standard("1M", "Minutes", "1 minute interval");
  public static final ChartInterval TWO_MINUTES = standard("2M", "Minutes", "2 minute interval");
  public static final ChartInterval FIVE_MINUTES = standard("5M", "Minutes", "5 minute interval");
  public static final ChartInterval TEN_MINUTES = standard("10M", "Minutes", "10 minute interval");
  public static final ChartInterval FIFTEEN_MINUTES = standard("15M", "Minutes", "15 minute interval");
  public static final ChartInterval THIRTY_MINUTES = standard("30M", "Minutes", "30 minute interval");
  public static final ChartInterval FORTY_FIVE_MINUTES = standard("45M", "Minutes", "45 minute interval");
  public static final ChartInterval ONE_HOUR = standard("1H", "Hours", "1 hour interval");
  public static final ChartInterval TWO_HOURS = standard("2H", "Hours", "2 hour interval");
  public static final ChartInterval THREE_HOURS = standard("3H", "Hours", "3 hour interval");
  public static final ChartInterval FOUR_HOURS = standard("4H", "Hours", "4 hour interval");
  public static final ChartInterval DAILY = standard("1D", "Days", "1 day interval");
  public static final ChartInterval WEEKLY = standard("1W", "Days", "1 week interval");
  public static final ChartInterval MONTHLY = standard("1Mo", "Days", "1 month interval");
  public static final ChartInterval THREE_MONTHS = standard("3Mo", "Days", "3 month interval");
  public static final ChartInterval SIX_MONTHS = standard("6Mo", "Days", "6 month interval");
  public static final ChartInterval TWELVE_MONTHS = standard("12Mo", "Days", "12 month interval");

  private static final ChartInterval[] STANDARD_VALUES = {
    ONE_TICK,
    TEN_TICKS,
    ONE_HUNDRED_TICKS,
    ONE_THOUSAND_TICKS,
    ONE_SECOND,
    FIVE_SECONDS,
    TEN_SECONDS,
    FIFTEEN_SECONDS,
    THIRTY_SECONDS,
    FORTY_FIVE_SECONDS,
    ONE_MINUTE,
    TWO_MINUTES,
    FIVE_MINUTES,
    TEN_MINUTES,
    FIFTEEN_MINUTES,
    THIRTY_MINUTES,
    FORTY_FIVE_MINUTES,
    ONE_HOUR,
    TWO_HOURS,
    THREE_HOURS,
    FOUR_HOURS,
    DAILY,
    WEEKLY,
    MONTHLY,
    THREE_MONTHS,
    SIX_MONTHS,
    TWELVE_MONTHS,
  };

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

  private ChartInterval(String displayName, String category, String description, double minimumLabelSpacing) {
    this.displayName = displayName;
    this.category = category;
    this.description = description;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  public static ChartInterval of(int amount, Classification classification) {
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    Classification selectedClassification = java.util.Objects.requireNonNull(classification, "classification");
    String pluralizedUnit = amount == 1 ? selectedClassification.unit : selectedClassification.unit + "s";
    return new ChartInterval(
      amount + selectedClassification.suffix,
      selectedClassification.category,
      amount + " " + pluralizedUnit + " interval",
      56.0
    );
  }

  public static ChartInterval[] values() {
    return STANDARD_VALUES.clone();
  }

  private static ChartInterval standard(String displayName, String category, String description) {
    return new ChartInterval(displayName, category, description, 56.0);
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
