package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.CalendarInterval;
import com.acteque.terminal.marketdata.HistoricalInterval;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class ChartIntervalHistoryMapperTest {

  @Test
  void mapsFixedAndCalendarSelections() {
    assertEquals(
      new HistoricalInterval.Intraday(Duration.ofMinutes(5)),
      ChartIntervalHistoryMapper.map(ChartInterval.FIVE_MINUTES).orElseThrow()
    );
    assertEquals(
      new HistoricalInterval.Intraday(Duration.ofHours(1)),
      ChartIntervalHistoryMapper.map(ChartInterval.ONE_HOUR).orElseThrow()
    );
    assertEquals(
      new HistoricalInterval.Calendar(CalendarInterval.MONTHLY),
      ChartIntervalHistoryMapper.map(ChartInterval.MONTHLY).orElseThrow()
    );
    assertEquals(
      new HistoricalInterval.Calendar(CalendarInterval.YEARLY),
      ChartIntervalHistoryMapper.map(ChartInterval.TWELVE_MONTHS).orElseThrow()
    );
  }

  @Test
  void leavesUnavailableUnitsAndMultiPeriodCalendarSelectionsUnmapped() {
    assertTrue(ChartIntervalHistoryMapper.map(ChartInterval.ONE_SECOND).isEmpty());
    assertTrue(ChartIntervalHistoryMapper.map(ChartInterval.ONE_TICK).isEmpty());
    assertTrue(ChartIntervalHistoryMapper.map(ChartInterval.THREE_MONTHS).isEmpty());
  }
}
