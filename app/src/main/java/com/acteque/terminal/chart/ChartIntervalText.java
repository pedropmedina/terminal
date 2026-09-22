package com.acteque.terminal.chart;

import java.util.Locale;
import java.util.Objects;

/** Presentation text and search terms for chart intervals. */
public final class ChartIntervalText {

  private ChartIntervalText() {}

  public static String classificationName(ChartInterval.Classification classification) {
    Objects.requireNonNull(classification, "classification cannot be null");
    return switch (classification) {
      case TICKS -> "Ticks";
      case SECONDS -> "Seconds";
      case MINUTES -> "Minutes";
      case HOURS -> "Hours";
      case DAYS -> "Days";
      case WEEKS -> "Weeks";
      case MONTHS -> "Months";
    };
  }

  public static String category(ChartInterval interval) {
    Objects.requireNonNull(interval, "interval cannot be null");
    return switch (interval.classification()) {
      case WEEKS, MONTHS -> "Days";
      default -> classificationName(interval.classification());
    };
  }

  /**
   * Returns a readable name for an interval, including yearly for twelve months.
   *
   * @param interval the chart interval to describe
   * @return the interval's readable name
   */
  public static String displayName(ChartInterval interval) {
    Objects.requireNonNull(interval, "interval cannot be null");
    if (interval.equals(ChartInterval.TWELVE_MONTHS)) {
      return "Yearly";
    }
    if (interval.amount() == 1) {
      return switch (interval.classification()) {
        case DAYS -> "Daily";
        case WEEKS -> "Weekly";
        case MONTHS -> "Monthly";
        default -> "1 " + unit(interval.classification());
      };
    }
    return interval.amount() + " " + unit(interval.classification()) + "s";
  }

  public static String description(ChartInterval interval) {
    return displayName(interval) + " interval";
  }

  public static boolean matches(ChartInterval interval, String normalizedQuery) {
    Objects.requireNonNull(interval, "interval cannot be null");
    Objects.requireNonNull(normalizedQuery, "normalizedQuery cannot be null");
    return (
      normalizedQuery.isEmpty() ||
      interval.name().toLowerCase(Locale.ROOT).equals(normalizedQuery) ||
      category(interval).toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
      description(interval).toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
  }

  private static String unit(ChartInterval.Classification classification) {
    return switch (classification) {
      case TICKS -> "tick";
      case SECONDS -> "second";
      case MINUTES -> "minute";
      case HOURS -> "hour";
      case DAYS -> "day";
      case WEEKS -> "week";
      case MONTHS -> "month";
    };
  }
}
