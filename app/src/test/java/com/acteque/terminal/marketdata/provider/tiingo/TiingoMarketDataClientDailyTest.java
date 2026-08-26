package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.MarketDataException;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoEodResampleFrequency;
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
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    List<DailyBar> bars = client.getDailyBars(
      new DailyBarRequest("aapl", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03"))
    );

    assertEquals("tiingo", client.provider());
    assertEquals(
      "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-02&endDate=2024-01-03&format=json",
      requestedUri.get().toString()
    );
    assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
    assertEquals("application/json", requestedHeaders.get().get("Accept"));

    assertEquals(2, bars.size());
    DailyBar first = bars.getFirst();
    assertEquals("AAPL", first.symbol());
    assertEquals(LocalDate.parse("2024-01-02"), first.date());
    assertEquals(new BigDecimal("99"), first.prices().open());
    assertEquals(new BigDecimal("98"), first.prices().close());
    assertEquals(new BigDecimal("1500"), first.prices().volume());
    assertEquals(new BigDecimal("49.5"), first.adjustedPrices().orElseThrow().open());
    assertEquals(new BigDecimal("0.25"), first.cashDividend().orElseThrow());
    assertEquals(new BigDecimal("1"), first.splitFactor().orElseThrow());
  }

  @Test
  void requestsAResampledEndOfDayFrequency() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(200, "[]");
    };
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    client.daily.getBars(
      new DailyBarRequest("aapl", LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31")),
      TiingoEodResampleFrequency.MONTHLY
    );

    assertEquals(
      "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-01&endDate=2024-12-31" +
      "&format=json&resampleFreq=monthly",
      requestedUri.get().toString()
    );
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
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    MarketDataException exception = assertThrows(MarketDataException.class, () ->
      client.getDailyBars(new DailyBarRequest("AAPL", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03")))
    );

    assertEquals(MarketDataException.Code.INVALID_RESPONSE, exception.code());
  }

  private static void assertStatusMapsTo(int status, MarketDataException.Code expectedCode) {
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(status, "provider error");
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    MarketDataException exception = assertThrows(MarketDataException.class, () ->
      client.getDailyBars(new DailyBarRequest("AAPL", LocalDate.parse("2024-01-02"), LocalDate.parse("2024-01-03")))
    );

    assertEquals(expectedCode, exception.code());
  }
}
