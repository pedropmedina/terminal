package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.MarketDataException;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TiingoMarketDataClientIntradayTest {

  @Test
  void requestsAndMapsTimestampedIexBars() {
    String csv =
      "date,close,high,low,open,volume" +
      "\n2024-01-02T14:35:00.000Z,102,103,99,100,2000" +
      "\n2024-01-02T14:30:00.000Z,98,101,97,99,1500";
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
      transport,
      TiingoIntradayFeed.IEX
    );

    List<IntradayBar> bars = client.getIntradayBars(
      new IntradayBarRequest(
        "aapl",
        LocalDate.parse("2024-01-02"),
        LocalDate.parse("2024-01-03"),
        Duration.ofMinutes(5),
        true,
        true
      )
    );

    assertEquals("tiingo", client.provider());
    assertEquals(
      "https://example.test/iex/AAPL/prices?startDate=2024-01-02&endDate=2024-01-03" +
      "&resampleFreq=5min&columns=open,high,low,close,volume&afterHours=true&forceFill=true&format=csv",
      requestedUri.get().toString()
    );
    assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
    assertEquals("text/csv", requestedHeaders.get().get("Accept"));

    assertEquals(2, bars.size());
    IntradayBar first = bars.getFirst();
    assertEquals("AAPL", first.symbol());
    assertEquals(Instant.parse("2024-01-02T14:30:00Z"), first.timestamp());
    assertEquals(new BigDecimal("99"), first.prices().open());
    assertEquals(new BigDecimal("98"), first.prices().close());
    assertEquals(new BigDecimal("1500"), first.prices().volume());
  }

  @Test
  void supportsTheConsolidatedFeedAndHourlyIntervals() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(200, "");
    };
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport,
      TiingoIntradayFeed.CONSOLIDATED
    );

    client.getIntradayBars(
      new IntradayBarRequest(
        "MSFT",
        LocalDate.parse("2024-01-02"),
        LocalDate.parse("2024-01-02"),
        Duration.ofHours(1)
      )
    );

    assertEquals(
      "https://example.test/tiingo/equity/intraday/MSFT/prices?startDate=2024-01-02&endDate=2024-01-02" +
      "&resampleFreq=1hour&columns=open,high,low,close,volume&afterHours=false&forceFill=false&format=csv",
      requestedUri.get().toString()
    );
  }

  @Test
  void rejectsResponsesMissingRequiredColumns() {
    TiingoHttpTransport transport = (uri, headers) ->
      new TiingoHttpTransport.Response(200, "date,close\n2024-01-02T14:30:00Z,10");
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport,
      TiingoIntradayFeed.IEX
    );

    MarketDataException exception = assertThrows(MarketDataException.class, () ->
      client.getIntradayBars(
        new IntradayBarRequest(
          "AAPL",
          LocalDate.parse("2024-01-02"),
          LocalDate.parse("2024-01-03"),
          Duration.ofMinutes(5)
        )
      )
    );

    assertEquals(MarketDataException.Code.INVALID_RESPONSE, exception.code());
  }

  @Test
  void mapsProviderStatusCodesToGlobalErrorCodes() {
    assertStatusMapsTo(401, MarketDataException.Code.AUTHENTICATION);
    assertStatusMapsTo(404, MarketDataException.Code.NOT_FOUND);
    assertStatusMapsTo(429, MarketDataException.Code.RATE_LIMITED);
    assertStatusMapsTo(500, MarketDataException.Code.PROVIDER_ERROR);
  }

  private static void assertStatusMapsTo(int status, MarketDataException.Code expectedCode) {
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(status, "provider error");
    TiingoMarketDataClient client = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport,
      TiingoIntradayFeed.IEX
    );

    MarketDataException exception = assertThrows(MarketDataException.class, () ->
      client.getIntradayBars(
        new IntradayBarRequest(
          "AAPL",
          LocalDate.parse("2024-01-02"),
          LocalDate.parse("2024-01-03"),
          Duration.ofMinutes(5)
        )
      )
    );

    assertEquals(expectedCode, exception.code());
  }
}
