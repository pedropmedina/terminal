package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.Ohlcv;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PricePointTest {

  @Test
  void convertsProviderNeutralDecimalsAtTheChartBoundary() {
    LocalDate date = LocalDate.of(2026, 9, 14);
    CalendarData bar = new CalendarData(
      "ACME",
      date,
      new Ohlcv(
        new BigDecimal("101.25"),
        new BigDecimal("104.50"),
        new BigDecimal("99.75"),
        new BigDecimal("103.125"),
        new BigDecimal("2500000")
      ),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );

    PricePoint point = PricePoint.from(bar);

    assertEquals(date, point.date());
    assertEquals(101.25, point.open());
    assertEquals(104.50, point.high());
    assertEquals(99.75, point.low());
    assertEquals(103.125, point.close());
    assertEquals(2_500_000, point.volume());
  }
}
