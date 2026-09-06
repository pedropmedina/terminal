package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.acteque.terminal.marketdata.InstrumentDetails;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.MarketDataException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class TiingoMarketDataClientDiscoveryTest {

  private static final URI BASE_URI = URI.create("https://example.test");

  @Test
  void mapsMetadataThroughTheProviderNeutralContractAndNormalizesTheRequest() {
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
          "name": "  Berkshire Hathaway Inc  ",
          "exchangeCode": " NYSE ",
          "description": "  A holding company.  "
        }
        """
      );
    };
    MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

    InstrumentDetails details = client.discovery().getInstrument("  brk-a  ");

    assertEquals("https://example.test/tiingo/daily/BRK-A", requestedUri.get().toString());
    assertEquals("application/json", requestedHeaders.get().get("Accept"));
    assertEquals("Token test-token", requestedHeaders.get().get("Authorization"));
    assertEquals("BRK-A", details.symbol());
    assertEquals(Optional.of("Berkshire Hathaway Inc"), details.name());
    assertEquals(Optional.of("NYSE"), details.exchange());
    assertEquals(Optional.of("A holding company."), details.description());
  }

  @Test
  void representsMissingNullAndBlankDescriptionsAsEmpty() {
    for (String description : List.of("", ",\"description\":null", ",\"description\":\"   \"")) {
      TiingoHttpTransport transport = (uri, headers) ->
        new TiingoHttpTransport.Response(
          200,
          "{\"ticker\":\"AAPL\",\"name\":\"Apple Inc\",\"exchangeCode\":\"NASDAQ\"" + description + "}"
        );
      MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

      InstrumentDetails details = client.discovery().getInstrument("AAPL");

      assertEquals("AAPL", details.symbol());
      assertEquals(Optional.of("Apple Inc"), details.name());
      assertEquals(Optional.of("NASDAQ"), details.exchange());
      assertEquals(Optional.empty(), details.description());
    }
  }

  @Test
  void rejectsBlankAndNullSymbolsBeforeMakingARequest() {
    AtomicInteger requests = new AtomicInteger();
    TiingoHttpTransport transport = (uri, headers) -> {
      requests.incrementAndGet();
      return new TiingoHttpTransport.Response(200, "{}");
    };
    MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

    for (String symbol : new String[] { null, "", " ", "\t\n" }) {
      assertThrows(IllegalArgumentException.class, () -> client.discovery().getInstrument(symbol));
    }

    assertEquals(0, requests.get());
  }

  @Test
  void exposesStableNonNullFeaturesWithoutMakingARequest() {
    AtomicInteger requests = new AtomicInteger();
    TiingoHttpTransport transport = (uri, headers) -> {
      requests.incrementAndGet();
      return new TiingoHttpTransport.Response(200, "{}");
    };
    MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

    assertEquals("tiingo", client.provider());
    assertNotNull(client.historicalBars());
    assertNotNull(client.discovery());
    assertSame(client.historicalBars(), client.historicalBars());
    assertSame(client.discovery(), client.discovery());
    assertEquals(0, requests.get());
  }

  @Test
  void mapsProviderStatusCodesToGlobalErrorCodes() {
    assertStatusMapsTo(401, MarketDataException.Code.AUTHENTICATION);
    assertStatusMapsTo(403, MarketDataException.Code.AUTHENTICATION);
    assertStatusMapsTo(404, MarketDataException.Code.NOT_FOUND);
    assertStatusMapsTo(429, MarketDataException.Code.RATE_LIMITED);
    assertStatusMapsTo(500, MarketDataException.Code.PROVIDER_ERROR);
  }

  @Test
  void rejectsMalformedMetadataAndMissingRequiredFields() {
    for (String json : List.of(
      "not json",
      "[]",
      "null",
      "{}",
      "{\"ticker\":\"AAPL\",\"exchangeCode\":\"NASDAQ\"}",
      "{\"ticker\":\"AAPL\",\"name\":\"Apple Inc\"}",
      "{\"name\":\"Apple Inc\",\"exchangeCode\":\"NASDAQ\"}"
    )) {
      TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(200, json);
      MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

      MarketDataException exception = assertThrows(MarketDataException.class, () ->
        client.discovery().getInstrument("AAPL")
      );

      assertEquals(MarketDataException.Code.INVALID_RESPONSE, exception.code(), json);
    }
  }

  private static void assertStatusMapsTo(int status, MarketDataException.Code expectedCode) {
    TiingoHttpTransport transport = (uri, headers) -> new TiingoHttpTransport.Response(status, "provider error");
    MarketDataClient client = new TiingoMarketDataClient("test-token", BASE_URI, transport);

    MarketDataException exception = assertThrows(MarketDataException.class, () ->
      client.discovery().getInstrument("AAPL")
    );

    assertEquals(expectedCode, exception.code());
  }
}
