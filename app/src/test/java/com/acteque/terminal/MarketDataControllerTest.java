package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.Ohlcv;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class MarketDataControllerTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void loadsInitialAndEarlierPagesIntoOneOrderedDeduplicatedSnapshot() {
    StubMarketDataClient client = new StubMarketDataClient(
      List.of(bar("2026-02-23", "101"), bar("2026-08-20", "102")),
      List.of(bar("2025-09-02", "99"), bar("2026-02-23", "100"))
    );
    try (
      MarketDataController controller = new MarketDataController(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      List<PricePoint> initial = controller.loadInitial();
      List<PricePoint> withEarlierHistory = controller.loadEarlier().toCompletableFuture().join();

      assertEquals(List.of(date("2026-02-23"), date("2026-08-20")), dates(initial));
      assertEquals(
        List.of(date("2025-09-02"), date("2026-02-23"), date("2026-08-20")),
        dates(withEarlierHistory)
      );
      assertEquals(100.0, withEarlierHistory.get(1).close());
      assertEquals(
        List.of(
          new DailyBarRequest("IBM", date("2026-02-21"), date("2026-08-21")),
          new DailyBarRequest("IBM", date("2025-08-23"), date("2026-02-22"))
        ),
        client.requests
      );
    }
  }

  @Test
  void stopsRequestingOnceAnEarlierPageAddsNoData() {
    StubMarketDataClient client = new StubMarketDataClient(List.of(bar("2026-02-23", "101")), List.of());
    try (
      MarketDataController controller = new MarketDataController(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      controller.loadInitial();
      controller.loadEarlier().toCompletableFuture().join();
      controller.loadEarlier().toCompletableFuture().join();

      assertEquals(2, client.requests.size());
    }
  }

  private static List<LocalDate> dates(List<PricePoint> points) {
    return points.stream().map(PricePoint::date).toList();
  }

  private static LocalDate date(String value) {
    return LocalDate.parse(value);
  }

  private static DailyBar bar(String date, String close) {
    BigDecimal price = new BigDecimal(close);
    Ohlcv prices = new Ohlcv(price, price, price, price, new BigDecimal("1000"));
    return new DailyBar("IBM", LocalDate.parse(date), prices, Optional.empty(), Optional.empty(), Optional.empty());
  }

  private static final class StubMarketDataClient implements MarketDataClient {

    private final List<List<DailyBar>> responses;
    private final List<DailyBarRequest> requests = new ArrayList<>();
    private int responseIndex;

    @SafeVarargs
    private StubMarketDataClient(List<DailyBar>... responses) {
      this.responses = List.of(responses);
    }

    @Override
    public String provider() {
      return "stub";
    }

    @Override
    public List<DailyBar> getDailyBars(DailyBarRequest request) {
      requests.add(request);
      return responses.get(responseIndex++);
    }

    @Override
    public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
      throw new UnsupportedOperationException();
    }
  }
}
