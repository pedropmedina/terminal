package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class MarketDataSessionDefaultMetadataTest {

  @Test
  void retainsActualProviderDetails() throws Exception {
    Instrument details = new Instrument(
      "Provider:Ab.C",
      Optional.of("Name"),
      Optional.of("Market"),
      Optional.of("Description")
    );
    try (MarketDataSessionDefault controller = new MarketDataSessionDefault(client(symbol -> details), "IBM")) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals("IBM", loaded.symbol());
      assertEquals("Name", loaded.displayName());
      assertEquals(details, loaded.details());
    }
  }

  @Test
  void metadataFailureProducesFallbackDetailsAndLegacyConstructorStillWorks() throws Exception {
    try (
      MarketDataSessionDefault controller = new MarketDataSessionDefault(
        client(symbol -> {
          throw new MarketDataException(MarketDataException.Code.NETWORK, "offline");
        }),
        "IBM"
      )
    ) {
      var loaded = controller.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS);
      assertEquals(new Instrument("IBM", Optional.empty(), Optional.empty(), Optional.empty()), loaded.details());
      assertEquals("IBM", loaded.displayName());
    }
    assertEquals(Optional.of("IBM name"), new InstrumentLoadResult("IBM", "IBM name", List.of()).details().name());
  }

  static MarketDataClient client(java.util.function.Function<String, Instrument> metadata) {
    return new MarketDataClient() {
      private final HistoricalData historical = new HistoricalData() {
        public List<CalendarData> getCalendarData(CalendarRequest request) {
          return List.of();
        }

        public List<IntradayData> getIntradayData(IntradayRequest request) {
          return List.of();
        }
      };

      public String provider() {
        return "some-provider";
      }

      public Optional<HistoricalData> historical() {
        return Optional.of(historical);
      }

      public Optional<InstrumentCatalog> catalog() {
        return Optional.of(
          new InstrumentCatalog() {
            public List<Instrument> getInstruments() {
              throw new AssertionError("Session should only request metadata");
            }

            public Instrument getInstrument(String symbol) {
              return metadata.apply(symbol);
            }
          }
        );
      }
    };
  }
}
