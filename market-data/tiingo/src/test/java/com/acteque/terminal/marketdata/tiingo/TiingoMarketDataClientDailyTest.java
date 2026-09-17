package com.acteque.terminal.marketdata.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.CalendarInterval;
import com.acteque.terminal.marketdata.CalendarRequest;
import com.acteque.terminal.marketdata.MarketDataException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TiingoMarketDataClientDailyTest {

  @Test
  void mapsTiingoDataToTheProviderNeutralContract() {
    String json =
      "[" +
      "{\"date\":\"2024-01-03T00:00:00.000Z\",\"close\":102,\"high\":103,\"low\":99," +
      "\"open\":100,\"volume\":2000,\"adjClose\":51,\"adjHigh\":51.5,\"adjLow\":49.5," +
      "\"adjOpen\":50,\"adjVolume\":4000,\"divCash\":0,\"splitFactor\":2}," +
      "{\"date\":\"2024-01-02T00:00:00.000Z\",\"close\":98,\"high\":101,\"low\":97," +
      "\"open\":99,\"volume\":1500,\"adjClose\":49,\"adjHigh\":50.5,\"adjLow\":48.5," +
      "\"adjOpen\":49.5,\"adjVolume\":3000,\"divCash\":0.25,\"splitFactor\":1}" +
      "]";
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    AtomicReference<Map<String, String>> requestedHeaders = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      requestedHeaders.set(headers);
      return new TiingoHttpTransport.Response(200, json);
    };
    try (
      TiingoMarketDataClient client = new TiingoMarketDataClient(
        "test-token",
        URI.create("https://example.test"),
        transport
      )
    ) {
      List<CalendarData> calendarData = client
        .historical()
        .orElseThrow()
        .getCalendarData(new CalendarRequest("aapl", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03")));

      assertEquals("tiingo", client.provider());
      assertEquals(
        "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-02&endDate=2024-01-03&format=json",
        requestedUri.get().toString()
      );
      assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
      assertEquals("application/json", requestedHeaders.get().get("Accept"));

      assertEquals(2, calendarData.size());
      CalendarData first = calendarData.getFirst();
      assertEquals("AAPL", first.symbol());
      assertEquals(LocalDate.parse("2024-01-02"), first.date());
      assertEquals(new BigDecimal("99"), first.prices().open());
      assertEquals(new BigDecimal("98"), first.prices().close());
      assertEquals(new BigDecimal("1500"), first.prices().volume());
      assertEquals(new BigDecimal("49.5"), first.adjustedPrices().orElseThrow().open());
      assertEquals(new BigDecimal("0.25"), first.cashDividend().orElseThrow());
      assertEquals(new BigDecimal("1"), first.splitFactor().orElseThrow());
    }
  }

  @Test
  void requestsCalendarIntervalsThroughTheProviderNeutralContract() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(
        200,
        """
        [{"date":"2024-12-31T00:00:00.000Z","open":10.125,"high":15,"low":9,"close":14,
          "volume":123456,"adjOpen":10.125,"adjHigh":15,"adjLow":9,"adjClose":14,"adjVolume":123456}]
        """
      );
    };
    try (
      TiingoMarketDataClient client = new TiingoMarketDataClient(
        "test-token",
        URI.create("https://example.test"),
        transport
      )
    ) {
      Map<CalendarInterval, String> frequencies = Map.of(
        CalendarInterval.DAILY,
        "",
        CalendarInterval.WEEKLY,
        "&resampleFreq=weekly",
        CalendarInterval.MONTHLY,
        "&resampleFreq=monthly",
        CalendarInterval.YEARLY,
        "&resampleFreq=annually"
      );
      for (CalendarInterval interval : CalendarInterval.values()) {
        List<CalendarData> data = client
          .historical()
          .orElseThrow()
          .getCalendarData(
            new CalendarRequest("aapl", LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31"), interval)
          );
        assertEquals(
          "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-01&endDate=2024-12-31&format=json" +
            frequencies.get(interval),
          requestedUri.get().toString()
        );
        assertEquals(1, data.size());
        assertEquals("AAPL", data.getFirst().symbol());
        assertEquals(LocalDate.of(2024, 12, 31), data.getFirst().date());
        assertEquals(new BigDecimal("10.125"), data.getFirst().prices().open());
        assertEquals(new BigDecimal("123456"), data.getFirst().prices().volume());
        assertEquals(data.getFirst().prices(), data.getFirst().adjustedPrices().orElseThrow());
        assertEquals(java.util.Optional.empty(), data.getFirst().cashDividend());
        assertEquals(java.util.Optional.empty(), data.getFirst().splitFactor());
      }
    }
  }

  @Test
  void mapsProviderStatusCodesToGlobalErrorCodes() {
    assertStatusMapsTo(401, MarketDataException.Code.AUTHENTICATION);
    assertStatusMapsTo(404, MarketDataException.Code.NOT_FOUND);
    assertStatusMapsTo(429, MarketDataException.Code.RATE_LIMITED);
    assertStatusMapsTo(500, MarketDataException.Code.PROVIDER_ERROR);
  }

  @Test
  void rejectsResponsesMissingRequiredFields() {
    TiingoHttpTransport transport = (uri, headers) ->
      new TiingoHttpTransport.Response(200, "[{\"date\":\"2024-01-02\",\"close\":10}]");
    try (
      TiingoMarketDataClient client = new TiingoMarketDataClient(
        "test-token",
        URI.create("https://example.test"),
        transport
      )
    ) {
      MarketDataException exception = assertThrows(MarketDataException.class, () ->
        client
          .historical()
          .orElseThrow()
          .getCalendarData(new CalendarRequest("AAPL", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03")))
      );

      assertEquals(MarketDataException.Code.INVALID_RESPONSE, exception.code());
    }
  }

  private static void assertStatusMapsTo(int status, MarketDataException.Code expectedCode) {
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(status, "provider error");
    try (
      TiingoMarketDataClient client = new TiingoMarketDataClient(
        "test-token",
        URI.create("https://example.test"),
        transport
      )
    ) {
      MarketDataException exception = assertThrows(MarketDataException.class, () ->
        client
          .historical()
          .orElseThrow()
          .getCalendarData(new CalendarRequest("AAPL", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03")))
      );

      assertEquals(expectedCode, exception.code());
    }
  }
}
