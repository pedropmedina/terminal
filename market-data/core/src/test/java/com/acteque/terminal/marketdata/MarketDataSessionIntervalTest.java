package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class MarketDataSessionIntervalTest {

  private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-08-21T12:00:00Z"), ZoneOffset.UTC);

  @Test
  void loadsAndPagesIntradayHistoryWithoutDiscardingTimestamps() {
    StubClient client = new StubClient();
    client.intradayResponses.add(List.of(bar("2026-08-20T14:30:00Z"), bar("2026-08-20T14:35:00Z")));
    client.intradayResponses.add(List.of(bar("2026-08-19T14:30:00Z")));
    try (
      MarketDataSessionDefault session = new MarketDataSessionDefault(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      HistoricalInterval interval = new HistoricalInterval.Intraday(Duration.ofMinutes(5));
      assertTrue(session.supports(interval));
      assertFalse(session.supports(new HistoricalInterval.Intraday(Duration.ofSeconds(5))));

      InstrumentHistoryLoadResult initial = session.loadInitial(interval).toCompletableFuture().join();
      HistoricalPage earlier = session.loadEarlierHistory().toCompletableFuture().join();

      assertEquals(interval, initial.history().interval());
      assertEquals(
        List.of(Instant.parse("2026-08-20T14:30:00Z"), Instant.parse("2026-08-20T14:35:00Z")),
        initial.history().intradayData().stream().map(IntradayData::timestamp).toList()
      );
      assertEquals(3, earlier.intradayData().size());
      assertEquals(Instant.parse("2026-08-19T14:30:00Z"), earlier.intradayData().getFirst().timestamp());
      assertEquals(Duration.ofMinutes(5), client.intradayRequests.getFirst().interval());
      assertEquals(LocalDate.parse("2026-08-19"), client.intradayRequests.getLast().endDate());
    }
  }

  @Test
  void changingToMonthlyReplacesTheIntradaySnapshot() {
    StubClient client = new StubClient();
    client.intradayResponses.add(List.of(bar("2026-08-20T14:30:00Z")));
    client.calendarResponses.add(List.of(calendarBar("2026-08-01")));
    try (
      MarketDataSessionDefault session = new MarketDataSessionDefault(
        client,
        "IBM",
        CLOCK,
        Executors.newVirtualThreadPerTaskExecutor()
      )
    ) {
      session
        .loadInitial(new HistoricalInterval.Intraday(Duration.ofHours(1)))
        .toCompletableFuture()
        .join();
      InstrumentHistoryLoadResult monthly = session
        .loadInstrumentHistory("IBM", new HistoricalInterval.Calendar(CalendarInterval.MONTHLY))
        .toCompletableFuture()
        .join();

      assertInstanceOf(HistoricalInterval.Calendar.class, monthly.history().interval());
      assertEquals(
        List.of(LocalDate.parse("2026-08-01")),
        monthly.history().calendarData().stream().map(CalendarData::date).toList()
      );
      assertTrue(monthly.history().intradayData().isEmpty());
      assertEquals(CalendarInterval.MONTHLY, client.calendarRequests.getFirst().interval());
    }
  }

  private static IntradayData bar(String timestamp) {
    return new IntradayData("IBM", Instant.parse(timestamp), prices());
  }

  private static CalendarData calendarBar(String date) {
    return new CalendarData(
      "IBM",
      LocalDate.parse(date),
      prices(),
      Optional.empty(),
      Optional.empty(),
      Optional.empty()
    );
  }

  private static Ohlcv prices() {
    return new Ohlcv(BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE);
  }

  private static final class StubClient implements MarketDataClient, HistoricalData {

    private final List<List<IntradayData>> intradayResponses = new ArrayList<>();
    private final List<List<CalendarData>> calendarResponses = new ArrayList<>();
    private final List<IntradayRequest> intradayRequests = new ArrayList<>();
    private final List<CalendarRequest> calendarRequests = new ArrayList<>();

    @Override
    public String provider() {
      return "stub";
    }

    @Override
    public Optional<HistoricalData> historical() {
      return Optional.of(this);
    }

    @Override
    public boolean supports(Duration interval) {
      return (
        interval.compareTo(Duration.ofMinutes(1)) >= 0 &&
        interval.compareTo(Duration.ofDays(1)) < 0 &&
        interval.toSecondsPart() == 0
      );
    }

    @Override
    public boolean supports(CalendarInterval interval) {
      return true;
    }

    @Override
    public List<CalendarData> getCalendarData(CalendarRequest request) {
      calendarRequests.add(request);
      return calendarResponses.removeFirst();
    }

    @Override
    public List<IntradayData> getIntradayData(IntradayRequest request) {
      intradayRequests.add(request);
      return intradayResponses.removeFirst();
    }
  }
}
