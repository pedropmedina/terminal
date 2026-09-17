package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class IntradayRequestTest {

  @Test
  void normalizesTheSymbolAndDefaultsOptionalControls() {
    IntradayRequest request = new IntradayRequest(
      " aapl ",
      LocalDate.parse("2024-01-02"),
      LocalDate.parse("2024-01-03"),
      Duration.ofMinutes(5)
    );

    assertEquals("AAPL", request.symbol());
    assertEquals(false, request.includeAfterHours());
    assertEquals(false, request.forceFill());
  }

  @Test
  void rejectsInvalidDateRangesAndIntervals() {
    LocalDate date = LocalDate.parse("2024-01-02");

    assertThrows(IllegalArgumentException.class, () ->
      new IntradayRequest("AAPL", date.plusDays(1), date, Duration.ofMinutes(5))
    );
    assertThrows(IllegalArgumentException.class, () -> new IntradayRequest("AAPL", date, date, Duration.ZERO));
    assertThrows(IllegalArgumentException.class, () -> new IntradayRequest("AAPL", date, date, Duration.ofSeconds(90)));
    assertThrows(IllegalArgumentException.class, () -> new IntradayRequest("AAPL", date, date, Duration.ofDays(1)));
  }
}
