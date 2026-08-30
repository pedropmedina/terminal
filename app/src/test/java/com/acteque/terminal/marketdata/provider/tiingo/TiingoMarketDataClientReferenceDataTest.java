package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.MarketDataException;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoTickerMetadata;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoSupportedTicker;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.marketdata.provider.tiingo.utilities.TiingoTickerSearchResult;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

class TiingoMarketDataClientReferenceDataTest {

  private static final URI BASE_URI = URI.create("https://example.test");
  private static final URI CATALOG_URI = URI.create("https://media.example.test/supported_tickers.zip");

  @Test
  void downloadsAndParsesSupportedTickersOnlyOncePerDay() throws IOException {
    byte[] archive = zip(
      "ticker,exchange,assetType,priceCurrency,startDate,endDate\n" +
        "AAPL,NASDAQ,Stock,USD,1980-12-12,2026-08-25\n" +
        "RESERVED,NASDAQ,Stock,USD,,"
    );
    AtomicInteger requests = new AtomicInteger();
    AtomicReference<Map<String, String>> requestedHeaders = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      assertEquals(CATALOG_URI, uri);
      requests.incrementAndGet();
      requestedHeaders.set(headers);
      return new TiingoHttpTransport.Response(200, archive);
    };
    MutableClock clock = new MutableClock(Instant.parse("2026-08-26T12:00:00Z"));
    TiingoTickerCatalogApi tickerCatalog = tickerCatalog(transport, clock);

    List<TiingoSupportedTicker> first = tickerCatalog.getSupportedTickers();
    List<TiingoSupportedTicker> second = tickerCatalog.getSupportedTickers();

    assertEquals(1, requests.get());
    assertEquals(first, second);
    assertEquals("application/zip", requestedHeaders.get().get("Accept"));
    assertFalse(requestedHeaders.get().containsKey("Authorization"));
    assertEquals(2, first.size());
    assertEquals("AAPL", first.getFirst().ticker());
    assertEquals("Stock", first.getFirst().assetType());
    assertEquals(LocalDate.parse("1980-12-12"), first.getFirst().startDate().orElseThrow());
    assertTrue(first.getLast().startDate().isEmpty());
    assertTrue(first.getLast().endDate().isEmpty());

    clock.setInstant(Instant.parse("2026-08-27T12:00:00Z"));
    tickerCatalog.getSupportedTickers();

    assertEquals(2, requests.get());
  }

  @Test
  void requestsTickerMetadata() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    AtomicReference<Map<String, String>> requestedHeaders = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      requestedHeaders.set(headers);
      return new TiingoHttpTransport.Response(
        200,
        """
        {
          "ticker": "BRK-A",
          "name": "Berkshire Hathaway Inc",
          "exchangeCode": "NYSE",
          "description": "A holding company.",
          "startDate": "1980-03-17",
          "endDate": "2026-08-25"
        }
        """
      );
    };
    TiingoDailyApi daily = daily(transport, Clock.systemUTC());

    TiingoTickerMetadata metadata = daily.getTicker("brk-a");

    assertEquals("https://example.test/tiingo/daily/BRK-A", requestedUri.get().toString());
    assertEquals("application/json", requestedHeaders.get().get("Accept"));
    assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
    assertEquals("Berkshire Hathaway Inc", metadata.name());
    assertEquals("A holding company.", metadata.description().orElseThrow());
    assertEquals(LocalDate.parse("1980-03-17"), metadata.startDate().orElseThrow());
  }

  @Test
  void searchesTickersAndNames() {
    AtomicReference<URI> requestedUri = new AtomicReference<>();
    TiingoHttpTransport transport = (uri, headers) -> {
      requestedUri.set(uri);
      return new TiingoHttpTransport.Response(
        200,
        """
        [{
          "ticker": "AAPL",
          "name": "Apple Inc",
          "assetType": "Stock",
          "isActive": true,
          "permaTicker": "US000000000001",
          "openFIGI": null
        }]
        """
      );
    };
    TiingoMarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

    List<TiingoTickerSearchResult> results = client.searchTickers("apple inc");

    assertEquals("https://example.test/tiingo/utilities/search?query=apple%20inc", requestedUri.get().toString());
    assertEquals(1, results.size());
    assertEquals("AAPL", results.getFirst().ticker());
    assertTrue(results.getFirst().active());
    assertEquals("US000000000001", results.getFirst().permaTicker().orElseThrow());
    assertTrue(results.getFirst().openFigi().isEmpty());
  }

  @Test
  void rejectsAnArchiveWithoutTheExpectedCsv() throws IOException {
    byte[] archive = zipEntry("readme.txt", "not ticker data");
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(200, archive);
    TiingoTickerCatalogApi tickerCatalog = tickerCatalog(transport, Clock.systemUTC());

    MarketDataException exception = assertThrows(MarketDataException.class, tickerCatalog::getSupportedTickers);

    assertEquals(MarketDataException.Code.INVALID_RESPONSE, exception.code());
  }

  private static TiingoTickerCatalogApi tickerCatalog(TiingoHttpTransport transport, Clock clock) {
    TiingoRequestExecutor requests = new TiingoRequestExecutor("test-token", transport);
    return new TiingoTickerCatalogApi(CATALOG_URI, requests, clock);
  }

  private static TiingoDailyApi daily(TiingoHttpTransport transport, Clock clock) {
    TiingoRequestExecutor requests = new TiingoRequestExecutor("test-token", transport);
    return new TiingoDailyApi(BASE_URI, requests);
  }

  private static byte[] zip(String csv) throws IOException {
    return zipEntry("supported_tickers.csv", csv);
  }

  private static byte[] zipEntry(String name, String contents) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
      zip.putNextEntry(new ZipEntry(name));
      zip.write(contents.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      zip.closeEntry();
    }
    return bytes.toByteArray();
  }

  private static final class MutableClock extends Clock {

    private Instant instant;

    private MutableClock(Instant instant) {
      this.instant = instant;
    }

    void setInstant(Instant instant) {
      this.instant = instant;
    }

    @Override
    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
      if (ZoneOffset.UTC.equals(zone)) {
        return this;
      }
      return Clock.fixed(instant, zone);
    }

    @Override
    public Instant instant() {
      return instant;
    }
  }
}
