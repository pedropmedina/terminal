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

  public static final ChartInterval ONE_TICK = standard(1, Classification.TICKS);
  public static final ChartInterval TEN_TICKS = standard(10, Classification.TICKS);
  public static final ChartInterval ONE_HUNDRED_TICKS = standard(100, Classification.TICKS);
  public static final ChartInterval ONE_THOUSAND_TICKS = standard(1000, Classification.TICKS);
  public static final ChartInterval ONE_SECOND = standard(1, Classification.SECONDS);
  public static final ChartInterval FIVE_SECONDS = standard(5, Classification.SECONDS);
  public static final ChartInterval TEN_SECONDS = standard(10, Classification.SECONDS);
  public static final ChartInterval FIFTEEN_SECONDS = standard(15, Classification.SECONDS);
  public static final ChartInterval THIRTY_SECONDS = standard(30, Classification.SECONDS);
  public static final ChartInterval FORTY_FIVE_SECONDS = standard(45, Classification.SECONDS);
  public static final ChartInterval ONE_MINUTE = standard(1, Classification.MINUTES);
  public static final ChartInterval TWO_MINUTES = standard(2, Classification.MINUTES);
  public static final ChartInterval FIVE_MINUTES = standard(5, Classification.MINUTES);
  public static final ChartInterval TEN_MINUTES = standard(10, Classification.MINUTES);
  public static final ChartInterval FIFTEEN_MINUTES = standard(15, Classification.MINUTES);
  public static final ChartInterval THIRTY_MINUTES = standard(30, Classification.MINUTES);
  public static final ChartInterval FORTY_FIVE_MINUTES = standard(45, Classification.MINUTES);
  public static final ChartInterval ONE_HOUR = standard(1, Classification.HOURS);
  public static final ChartInterval TWO_HOURS = standard(2, Classification.HOURS);
  public static final ChartInterval THREE_HOURS = standard(3, Classification.HOURS);
  public static final ChartInterval FOUR_HOURS = standard(4, Classification.HOURS);
  public static final ChartInterval DAILY = standard(1, Classification.DAYS);
  public static final ChartInterval WEEKLY = standard(1, Classification.WEEKS);
  public static final ChartInterval MONTHLY = standard(1, Classification.MONTHS);
  public static final ChartInterval THREE_MONTHS = standard(3, Classification.MONTHS);
  public static final ChartInterval SIX_MONTHS = standard(6, Classification.MONTHS);
  public static final ChartInterval TWELVE_MONTHS = standard(12, Classification.MONTHS);

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

  private final int amount;
  private final Classification classification;
  private final String name;
  private final String category;
  private final String description;
  private final double minimumLabelSpacing;

  private ChartInterval(int amount, Classification classification, String description, double minimumLabelSpacing) {
    this.amount = amount;
    this.classification = classification;
    this.name = amount + classification.suffix;
    this.category = classification.category;
    this.description = description;
    this.minimumLabelSpacing = minimumLabelSpacing;
  }

  public static ChartInterval of(int amount, Classification classification) {
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    Classification selectedClassification = java.util.Objects.requireNonNull(classification, "classification");
    String pluralizedUnit = amount == 1 ? selectedClassification.unit : selectedClassification.unit + "s";
    return new ChartInterval(amount, selectedClassification, amount + " " + pluralizedUnit + " interval", 56.0);
  }

  public static ChartInterval[] values() {
    return STANDARD_VALUES.clone();
  }

  private static ChartInterval standard(int amount, Classification classification) {
    return new ChartInterval(amount, classification, amount + " " + classification.unit + " interval", 56.0);
  }

  public String name() {
    return name;
  }

  public String displayName() {
    if (amount == 1) {
      return switch (classification) {
        case DAYS -> "Daily";
        case WEEKS -> "Weekly";
        case MONTHS -> "Monthly";
        default -> "1 " + classification.unit;
      };
    }
    return amount + " " + classification.unit + "s";
  }

  public String category() {
    return category;
  }

  public String description() {
    return description;
  }

  public boolean matches(String normalizedQuery) {
    return (
      normalizedQuery.isEmpty() ||
      name.toLowerCase(Locale.ROOT).equals(normalizedQuery) ||
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
