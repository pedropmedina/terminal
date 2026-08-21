package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyBarRequestTest {

  @Test
  void normalizesTheSymbol() {
    DailyBarRequest request = new DailyBarRequest(
      " aapl ",
      LocalDate.parse("2024-01-01"),
      LocalDate.parse("2024-01-31")
    );

    assertEquals("AAPL", request.symbol());
  }

  @Test
  void rejectsAnInvertedDateRange() {
    assertThrows(IllegalArgumentException.class, () ->
      new DailyBarRequest("AAPL", LocalDate.parse("2024-02-01"), LocalDate.parse("2024-01-01"))
    );
  }
}
