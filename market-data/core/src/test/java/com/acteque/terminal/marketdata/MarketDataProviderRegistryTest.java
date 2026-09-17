package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class MarketDataProviderRegistryTest {

  @Test
  void selectsFactoriesAndCreatesIndependentConfiguredInstances() {
    try (var registry = new MarketDataProviderRegistry(List.of(factory("first"), factory("second")))) {
      var first = (Client) registry.create("first", Map.of("account", "one")::get);
      var another = (Client) registry.create("first", Map.of("account", "two")::get);
      assertNotSame(first, another);
      assertEquals("one", first.account);
      assertEquals("two", another.account);
      assertEquals("second", registry.create("second", key -> null).provider());
      assertThrows(IllegalArgumentException.class, () -> registry.create("unknown", key -> null));
    }
  }

  @Test
  void rejectsDuplicateAndMalformedRegistrations() {
    assertThrows(IllegalArgumentException.class, () ->
      new MarketDataProviderRegistry(List.of(factory("same"), factory("same")))
    );
    assertThrows(IllegalArgumentException.class, () -> new MarketDataProviderRegistry(List.of(factory("UPPER"))));
  }

  @Test
  void sessionsShareCapabilitiesButNotStateOrProviderOwnership() throws Exception {
    Client client;
    var registry = new MarketDataProviderRegistry(List.of(factory("test")));
    try (registry) {
      client = (Client) registry.create("test", key -> null);
      assertTrue(client.catalog().isEmpty());
      try (
        var first = new MarketDataSessionDefault(client, "IBM");
        var second = new MarketDataSessionDefault(client, "AAPL")
      ) {
        assertEquals("IBM", first.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).displayName());
        first.close();
        assertEquals(0, client.closes);
        assertEquals("AAPL", second.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
      }
      assertEquals(0, client.closes);
    }
    assertEquals(1, client.closes);
    registry.close();
    assertEquals(1, client.closes);
    assertThrows(IllegalStateException.class, () -> registry.create("test", key -> null));
  }

  @Test
  void catalogOnlyProviderIsValidButCannotBackAHistorySession() {
    try (
      MarketDataClient client = new MarketDataClient() {
        public String provider() {
          return "catalog-only";
        }

        public Optional<InstrumentCatalog> catalog() {
          return Optional.of(
            new InstrumentCatalog() {
              public List<Instrument> getInstruments() {
                return List.of();
              }

              public Instrument getInstrument(String symbol) {
                throw new MarketDataException(MarketDataException.Code.NOT_FOUND, "Unknown symbol");
              }
            }
          );
        }
      }
    ) {
      assertTrue(client.historical().isEmpty());
      assertTrue(client.catalog().isPresent());
      assertTrue(
        assertThrows(IllegalArgumentException.class, () -> new MarketDataSessionDefault(client, "IBM"))
          .getMessage()
          .contains("historical data")
      );
    }
  }

  @Test
  void closesRemainingClientsAfterACloseFailure() {
    var registry = new MarketDataProviderRegistry(List.of(factory("test")));
    Client first = (Client) registry.create("test", key -> null);
    Client second = (Client) registry.create("test", key -> null);
    second.failClose = true;
    assertEquals(1, assertThrows(IllegalStateException.class, registry::close).getSuppressed().length);
    assertEquals(1, first.closes);
    assertEquals(1, second.closes);
    registry.close();
    assertEquals(1, first.closes);
  }

  @Test
  void rejectsAndClosesAMismatchedClient() {
    Client mismatched = new Client("other", null);
    var factory = new MarketDataProviderFactory() {
      public String provider() {
        return "test";
      }

      public MarketDataClient create(Function<String, String> settings) {
        return mismatched;
      }
    };
    try (var registry = new MarketDataProviderRegistry(List.of(factory))) {
      assertThrows(IllegalStateException.class, () -> registry.create("test", key -> null));
      assertEquals(1, mismatched.closes);
    }
  }

  private static MarketDataProviderFactory factory(String id) {
    return new MarketDataProviderFactory() {
      public String provider() {
        return id;
      }

      public MarketDataClient create(Function<String, String> configuration) {
        return new Client(id, configuration.apply("account"));
      }
    };
  }

  private static final class Client implements MarketDataClient {

    private final String id;
    private final String account;
    private int closes;
    private boolean failClose;
    private final HistoricalData historical = new HistoricalData() {
      public List<CalendarData> getCalendarData(CalendarRequest request) {
        assertEquals(0, closes);
        return List.of();
      }

      public List<IntradayData> getIntradayData(IntradayRequest request) {
        return List.of();
      }
    };

    private Client(String id, String account) {
      this.id = id;
      this.account = account;
    }

    public String provider() {
      return id;
    }

    public Optional<HistoricalData> historical() {
      return Optional.of(historical);
    }

    public void close() {
      closes++;
      if (failClose) throw new IllegalStateException("close failed");
    }
  }
}
