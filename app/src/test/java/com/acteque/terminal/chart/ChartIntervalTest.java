package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartIntervalTest {

  @Test
  void formatsLongIntervalNamesForEveryClassification() {
    String[] singularNames = { "1 tick", "1 second", "1 minute", "1 hour", "Daily", "Weekly", "Monthly" };
    String[] pluralNames = { "7 ticks", "7 seconds", "7 minutes", "7 hours", "7 days", "7 weeks", "7 months" };
    for (ChartInterval.Classification classification : ChartInterval.Classification.values()) {
      assertEquals(
        singularNames[classification.ordinal()],
        ChartIntervalText.displayName(ChartInterval.of(1, classification))
      );
      assertEquals(
        pluralNames[classification.ordinal()],
        ChartIntervalText.displayName(ChartInterval.of(7, classification))
      );
    }
    assertEquals("Daily", ChartIntervalText.displayName(ChartInterval.DAILY));
    assertEquals("Weekly", ChartIntervalText.displayName(ChartInterval.WEEKLY));
    assertEquals("Monthly", ChartIntervalText.displayName(ChartInterval.MONTHLY));
    assertEquals("Yearly", ChartIntervalText.displayName(ChartInterval.TWELVE_MONTHS));
    assertEquals("5 minutes", ChartIntervalText.displayName(ChartInterval.FIVE_MINUTES));
    assertEquals("3 months", ChartIntervalText.displayName(ChartInterval.THREE_MONTHS));
    assertEquals("D", ChartInterval.DAILY.name());
    assertEquals("W", ChartInterval.WEEKLY.name());
    assertEquals("M", ChartInterval.MONTHLY.name());
    assertEquals("Y", ChartInterval.TWELVE_MONTHS.name());
    assertEquals("1m", ChartInterval.ONE_MINUTE.name());
    assertEquals("5m", ChartInterval.FIVE_MINUTES.name());
    assertEquals("3mo", ChartInterval.THREE_MONTHS.name());
    assertEquals("2d", ChartInterval.of(2, ChartInterval.Classification.DAYS).name());
    assertEquals(ChartInterval.of(5, ChartInterval.Classification.MINUTES), ChartInterval.FIVE_MINUTES);
  }
}
