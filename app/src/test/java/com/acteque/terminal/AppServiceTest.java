package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketdata.*;
import com.acteque.terminal.marketlogos.*;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class AppServiceTest {

  @Test
  void selectsConfiguredProvidersAndSharesThemAcrossIndependentSessions() throws Exception {
    Data data = new Data();
    Logos logos = new Logos();
    AppService services = services(
      data,
      logos,
      Map.of("MARKET_DATA_PROVIDER", " data ", "MARKET_LOGO_PROVIDER", " logos ")
    );
    try (services) {
      assertTrue(services.catalog().getInstruments().isEmpty());
      try (
        var first = services.createMarketDataSession("IBM");
        var second = services.createMarketDataSession("AAPL");
        var logoSession = services.createLogoSession()
      ) {
        assertEquals("IBM", first.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        first.close();
        assertEquals("AAPL", second.loadInitial().toCompletableFuture().get(5, TimeUnit.SECONDS).symbol());
        assertEquals(0, data.closes);
        assertTrue(logoSession.findLogo(new LogoRequest("IBM", Optional.empty())).isEmpty());
      }
      assertEquals(0, logos.closes);
    }
    services.close();
    assertEquals(1, data.closes);
    assertEquals(1, logos.closes);
    assertThrows(IllegalStateException.class, services::createLogoSession);
    assertThrows(IllegalStateException.class, () -> services.createMarketDataSession("IBM"));
  }

  @Test
  void failedLogoSelectionClosesAlreadyCreatedMarketData() {
    Data data = new Data();
    assertThrows(IllegalArgumentException.class, () ->
      services(data, new Logos(), Map.of("MARKET_DATA_PROVIDER", "data", "MARKET_LOGO_PROVIDER", "missing"))
    );
    assertEquals(1, data.closes);
  }

  @Test
  void missingRequiredCapabilityClosesProvider() {
    Data data = new Data();
    data.hasHistory = false;
    assertThrows(IllegalStateException.class, () ->
      services(data, new Logos(), Map.of("MARKET_DATA_PROVIDER", "data"))
    );
    assertEquals(1, data.closes);
  }

  @Test
  void closesDataEvenWhenLogoCleanupFails() {
    Data data = new Data();
    Logos logos = new Logos();
    logos.failClose = true;
    var services = services(data, logos, Map.of("MARKET_DATA_PROVIDER", "data", "MARKET_LOGO_PROVIDER", "logos"));
    assertThrows(IllegalStateException.class, services::close);
    services.close();
    assertEquals(1, data.closes);
    assertEquals(1, logos.closes);
  }

  private static AppService services(Data data, Logos logos, Map<String, String> configuration) {
    return new AppService(
      configuration::get,
      List.of(
        new MarketDataProviderFactory() {
          public String provider() {
            return "data";
          }

          public MarketDataClient create(Function<String, String> settings) {
            return data;
          }
        }
      ),
      List.of(
        new LogoProviderFactory() {
          public String provider() {
            return "logos";
          }

          public LogoProvider create(Function<String, String> settings) {
            return logos;
          }
        }
      )
    );
  }

  private static final class Data implements MarketDataClient {

    int closes;
    boolean hasHistory = true;

    public String provider() {
      return "data";
    }

    public Optional<InstrumentCatalog> catalog() {
      return Optional.of(new StubInstrumentCatalog(List::of));
    }

    public Optional<HistoricalData> historical() {
      return hasHistory
        ? Optional.of(
            new HistoricalData() {
              public List<CalendarData> getCalendarData(CalendarRequest request) {
                return List.of();
              }

              public List<IntradayData> getIntradayData(IntradayRequest request) {
                return List.of();
              }
            }
          )
        : Optional.empty();
    }

    public void close() {
      closes++;
    }
  }

  private static final class Logos implements LogoProvider {

    int closes;
    boolean failClose;

    public String provider() {
      return "logos";
    }

    public Optional<InstrumentLogo> findLogo(LogoRequest request) {
      return Optional.empty();
    }

    public Optional<byte[]> load(InstrumentLogo logo) {
      return Optional.empty();
    }

    public void close() {
      closes++;
      if (failClose) throw new IllegalStateException("cleanup failed");
    }
  }
}
