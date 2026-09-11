package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ChartIntervalTest {

  @Test
  void formatsLongIntervalNamesForEveryClassification() {
    String[] singularNames = { "1 tick", "1 second", "1 minute", "1 hour", "Daily", "Weekly", "Monthly" };
    String[] pluralNames = { "7 ticks", "7 seconds", "7 minutes", "7 hours", "7 days", "7 weeks", "7 months" };
    for (ChartInterval.Classification classification : ChartInterval.Classification.values()) {
      assertEquals(singularNames[classification.ordinal()], ChartInterval.of(1, classification).displayName());
      assertEquals(pluralNames[classification.ordinal()], ChartInterval.of(7, classification).displayName());
    }
    assertEquals("Daily", ChartInterval.DAILY.displayName());
    assertEquals("Weekly", ChartInterval.WEEKLY.displayName());
    assertEquals("Monthly", ChartInterval.MONTHLY.displayName());
    assertEquals("5 minutes", ChartInterval.FIVE_MINUTES.displayName());
    assertEquals("3 months", ChartInterval.THREE_MONTHS.displayName());
    assertEquals("1D", ChartInterval.DAILY.name());
    assertEquals("5M", ChartInterval.FIVE_MINUTES.name());
  }
}
