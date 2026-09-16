package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DefaultMarketDataSessionMetadataTest {

  @Test
  void retainsActualProviderDetails() throws Exception {
    InstrumentDetails details = new InstrumentDetails(
      "Provider:Ab.C",
      Optional.of("Name"),
      Optional.of("Market"),
      Optional.of("Description")
    );
    try (DefaultMarketDataSession controller = new DefaultMarketDataSession(client(symbol -> details), "IBM")) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals("IBM", loaded.symbol());
      assertEquals("Name", loaded.displayName());
      assertEquals(details, loaded.details());
    }
  }

  @Test
  void metadataFailureProducesFallbackDetailsAndLegacyConstructorStillWorks() throws Exception {
    try (
      DefaultMarketDataSession controller = new DefaultMarketDataSession(
        client(symbol -> {
          throw new MarketDataException(MarketDataException.Code.NETWORK, "offline");
        }),
        "IBM"
      )
    ) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals(
        new InstrumentDetails("IBM", Optional.empty(), Optional.empty(), Optional.empty()),
        loaded.details()
      );
      assertEquals("IBM", loaded.displayName());
    }
    assertEquals(Optional.of("IBM name"), new LoadedInstrument("IBM", "IBM name", List.of()).details().name());
  }

  static MarketDataClient client(InstrumentDiscovery discovery) {
    return new MarketDataClient() {
      private final HistoricalBarData history = new HistoricalBarData() {
        public List<DailyBar> getDailyBars(DailyBarRequest request) {
          return List.of();
        }

        public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
          return List.of();
        }
      };

      public String provider() {
        return "some-provider";
      }

      public Optional<HistoricalBarData> historicalBars() {
        return Optional.of(history);
      }

      public Optional<InstrumentDiscovery> discovery() {
        return Optional.of(discovery);
      }
    };
  }
}
