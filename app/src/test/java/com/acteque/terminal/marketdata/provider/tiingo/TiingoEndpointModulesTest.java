package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoEodResampleFrequency;
import com.acteque.terminal.marketdata.provider.tiingo.iex.TiingoIexApi;
import com.acteque.terminal.marketdata.provider.tiingo.iex.TiingoIexLastPrice;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import java.math.BigDecimal;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TiingoEndpointModulesTest {

  @Test
  void exposesDailyAndIexAsFirstClassEndpointModules() {
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(200, "[]");
    TiingoMarketDataClient tiingo = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    assertEquals(TiingoDailyApi.class, tiingo.daily.getClass());
    assertEquals(TiingoIexApi.class, tiingo.iex.getClass());
    assertEquals(TiingoTickerCatalogApi.class, tiingo.tickerCatalog.getClass());
  }

  @Test
  void dailyModuleOwnsDailyBarRequests() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(200, "[]");
    };
    TiingoMarketDataClient tiingo = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    tiingo.daily.getBars(
      "aapl",
      LocalDate.parse("2024-01-01"),
      LocalDate.parse("2024-12-31"),
      TiingoEodResampleFrequency.MONTHLY
    );

    assertEquals(
      "https://example.test/tiingo/daily/AAPL/prices?startDate=2024-01-01&endDate=2024-12-31" +
      "&format=json&resampleFreq=monthly",
      requestedUri.get().toString()
    );
  }

  @Test
  void iexModuleGetsAllAndSingleLastPrices() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    String json =
      """
      [{
        "ticker": "AAPL",
        "timestamp": "2024-01-02T20:59:59.000Z",
        "quoteTimestamp": "2024-01-02T20:59:58.000Z",
        "lastSaleTimestamp": "2024-01-02T20:59:57.000Z",
        "last": 185.5,
        "lastSize": 100,
        "tngoLast": 185.5,
        "prevClose": 184.25,
        "open": 184.5,
        "high": 186,
        "low": 183.75,
        "mid": 185.49,
        "volume": 123456,
        "bidPrice": 185.48,
        "bidSize": 200,
        "askPrice": 185.50,
        "askSize": 300
      }]
      """;
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(200, json);
    };
    TiingoMarketDataClient tiingo = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    List<TiingoIexLastPrice> all = tiingo.iex.getLastPrices();
    assertEquals("https://example.test/iex", requestedUri.get().toString());
    assertEquals("AAPL", all.getFirst().ticker());

    TiingoIexLastPrice aapl = tiingo.iex.getLastPrice("aapl");
    assertEquals("https://example.test/iex/AAPL", requestedUri.get().toString());
    assertEquals(new BigDecimal("185.5"), aapl.last().orElseThrow());
    assertEquals(Instant.parse("2024-01-02T20:59:59Z"), aapl.timestamp().orElseThrow());
  }

  @Test
  void iexModuleOwnsHistoricalPriceRequests() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(200, "[]");
    };
    TiingoMarketDataClient tiingo = new TiingoMarketDataClient(
      "test-token",
      URI.create("https://example.test"),
      transport
    );

    tiingo.iex.getPrices(
      "msft",
      LocalDate.parse("2024-01-02"),
      LocalDate.parse("2024-01-03"),
      Duration.ofMinutes(5)
    );

    assertEquals(
      "https://example.test/iex/MSFT/prices?startDate=2024-01-02&endDate=2024-01-03" +
      "&resampleFreq=5min&columns=open,high,low,close,volume&afterHours=false&forceFill=false&format=json",
      requestedUri.get().toString()
    );
  }
}
