package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.MarketDataException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TiingoMarketDataClientDailyTest {

  private static final String HEADER =
    "date,close,high,low,open,volume,adjClose,adjHigh,adjLow,adjOpen,adjVolume,divCash,splitFactor";

  @Test
  void mapsTiingoDataToTheProviderNeutralContract() {
    String csv =
      HEADER +
      "\n2024-01-03T00:00:00.000Z,102,103,99,100,2000,51,51.5,49.5,50,4000,0,2" +
      "\n2024-01-02T00:00:00.000Z,98,101,97,99,1500,49,50.5,48.5,49.5,3000,0.25,1";
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    AtomicReference<Map<String, String>> requestedHeaders = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      requestedHeaders.set(headers);
      return new TiingoHttpTransport.Response(200, csv);
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
      "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-02&endDate=2024-01-03&format=csv",
      requestedUri.get().toString()
    );
    assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
    assertEquals("text/csv", requestedHeaders.get().get("Accept"));

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
      return new TiingoHttpTransport.Response(200, "");
    };
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    client.getDailyBars(
      new DailyBarRequest("aapl", LocalDate.parse("2024-01-01"), LocalDate.parse("2024-12-31")),
      TiingoEodResampleFrequency.MONTHLY
    );

    assertEquals(
      "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-01&endDate=2024-12-31" +
      "&format=csv&resampleFreq=monthly",
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
  void rejectsResponsesMissingRequiredColumns() {
    TiingoHttpTransport transport = (uri, headers) ->
      new TiingoHttpTransport.Response(200, "date,close\n2024-01-02,10");
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
