package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Reusable conformance tests for historical providers. Implement fixtures through an injected
 * transport: two reversed AAPL daily bars for Jan 2/3 2024 with exact close 10.12345678901234567890,
 * an empty response, and a transport failure. Never call live endpoints.
 */
public abstract class HistoricalMarketDataContract {

  protected enum Fixture {
    REVERSED_BARS,
    EMPTY,
    NETWORK_FAILURE,
  }

  protected abstract MarketDataClient createClient(Fixture fixture);

  private static CalendarRequest request() {
    return new CalendarRequest(" aapl ", LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3));
  }

  @Test
  void returnsOrderedNormalizedBarsWithDecimalPrecision() {
    try (MarketDataClient client = createClient(Fixture.REVERSED_BARS)) {
      HistoricalData historical = client.historical().orElseThrow();
      assertSame(historical, client.historical().orElseThrow());
      List<CalendarData> bars = historical.getCalendarData(request());
      assertEquals(
        List.of(LocalDate.of(2024, 1, 2), LocalDate.of(2024, 1, 3)),
        bars.stream().map(CalendarData::date).toList()
      );
      for (CalendarData bar : bars) {
        assertEquals("AAPL", bar.symbol());
        assertEquals(0, new BigDecimal("10.12345678901234567890").compareTo(bar.prices().close()));
      }
    }
  }

  @Test
  void distinguishesEmptyHistoryFromTransportFailure() {
    try (MarketDataClient client = createClient(Fixture.EMPTY)) {
      assertTrue(client.historical().orElseThrow().getCalendarData(request()).isEmpty());
    }
    try (MarketDataClient client = createClient(Fixture.NETWORK_FAILURE)) {
      assertEquals(
        MarketDataException.Code.NETWORK,
        assertThrows(MarketDataException.class, () ->
          client.historical().orElseThrow().getCalendarData(request())
        ).code()
      );
    }
  }
}
