package com.acteque.terminal.chart;

import java.util.Objects;

/** Immutable interval value shared by chart MVCI models. */
public record ChartInterval(int amount, Classification classification) {
  public enum Classification {
    TICKS("t"),
    SECONDS("s"),
    MINUTES("m"),
    HOURS("h"),
    DAYS("d"),
    WEEKS("w"),
    MONTHS("mo");

    private final String suffix;

    Classification(String suffix) {
      this.suffix = suffix;
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

  public ChartInterval {
    if (amount <= 0) {
      throw new IllegalArgumentException("amount must be greater than zero");
    }
    Objects.requireNonNull(classification, "classification cannot be null");
  }

  public static ChartInterval of(int amount, Classification classification) {
    return new ChartInterval(amount, classification);
  }

  public static ChartInterval[] values() {
    return STANDARD_VALUES.clone();
  }

  private static ChartInterval standard(int amount, Classification classification) {
    return new ChartInterval(amount, classification);
  }

  /**
   * Returns the short chart label, using single letters for standard calendar intervals.
   *
   * @return the interval's short display label
   */
  public String name() {
    if (amount == 1) {
      return switch (classification) {
        case DAYS -> "D";
        case WEEKS -> "W";
        case MONTHS -> "M";
        default -> amount + classification.suffix;
      };
    }
    if (amount == 12 && classification == Classification.MONTHS) {
      return "Y";
    }
    return amount + classification.suffix;
  }
}
