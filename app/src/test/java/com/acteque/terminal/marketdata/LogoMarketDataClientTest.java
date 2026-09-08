package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class LogoMarketDataClientTest {

  static final InstrumentDetails DETAILS = new InstrumentDetails(
    "Provider:Ab.C",
    Optional.of("Name"),
    Optional.of("Market"),
    Optional.of("Description")
  );
  static final InstrumentLogo LOGO = new InstrumentLogo(
    URI.create("https://images.example.com/a.png"),
    "Logos",
    URI.create("https://example.com")
  );

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

      public HistoricalBarData historicalBars() {
        return history;
      }

      public InstrumentDiscovery discovery() {
        return discovery;
      }
    };
  }

  @Test
  void delegatesAndEnrichesActualDetailsWithoutLoadingBytes() {
    MarketDataClient delegate = client(symbol -> {
      assertEquals("requested", symbol);
      return DETAILS;
    });
    AtomicInteger loads = new AtomicInteger();
    InstrumentLogos logos = new InstrumentLogos() {
      public Optional<InstrumentLogo> findLogo(InstrumentDetails details) {
        assertSame(DETAILS, details);
        return Optional.of(LOGO);
      }

      public Optional<byte[]> load(InstrumentLogo logo) {
        loads.incrementAndGet();
        return Optional.empty();
      }
    };
    MarketDataClient decorated = new LogoMarketDataClient(delegate, logos);
    assertEquals(delegate.provider(), decorated.provider());
    assertSame(delegate.historicalBars(), decorated.historicalBars());
    assertSame(logos, decorated.instrumentLogos());
    assertSame(decorated.discovery(), decorated.discovery());
    assertEquals(DETAILS.withLogo(Optional.of(LOGO)), decorated.discovery().getInstrument("requested"));
    assertEquals(0, loads.get());
  }

  @Test
  void defaultsAreReusableAndMissingLogosLeaveMetadataIntact() {
    MarketDataClient delegate = client(symbol -> DETAILS);
    assertSame(delegate.instrumentLogos(), client(symbol -> DETAILS).instrumentLogos());
    assertTrue(delegate.instrumentLogos().findLogo(DETAILS).isEmpty());
    assertTrue(delegate.instrumentLogos().load(LOGO).isEmpty());
    assertEquals(DETAILS, new LogoMarketDataClient(delegate, InstrumentLogos.NONE).discovery().getInstrument("any"));
  }

  @Test
  void discoveryFailuresAreNotHidden() {
    MarketDataException failure = new MarketDataException(MarketDataException.Code.NETWORK, "unavailable");
    MarketDataClient decorated = new LogoMarketDataClient(
      client(symbol -> {
        throw failure;
      }),
      InstrumentLogos.NONE
    );
    assertSame(failure, assertThrows(MarketDataException.class, () -> decorated.discovery().getInstrument("any")));
  }
}
