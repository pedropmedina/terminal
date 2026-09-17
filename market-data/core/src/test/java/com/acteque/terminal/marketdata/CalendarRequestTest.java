package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class CalendarRequestTest {

  @Test
  void normalizesTheSymbol() {
    CalendarRequest request = new CalendarRequest(
      " aapl ",
      LocalDate.parse("2024-01-01"),
      LocalDate.parse("2024-01-31")
    );

    assertEquals("AAPL", request.symbol());
    assertEquals(CalendarInterval.DAILY, request.interval());
  }

  @Test
  void preservesCalendarIntervals() {
    for (CalendarInterval interval : CalendarInterval.values()) {
      CalendarRequest request = new CalendarRequest(
        "AAPL",
        LocalDate.of(2024, 1, 1),
        LocalDate.of(2024, 12, 31),
        interval
      );
      assertEquals(interval, request.interval());
    }
  }

  @Test
  void rejectsAMissingInterval() {
    assertThrows(NullPointerException.class, () ->
      new CalendarRequest("AAPL", LocalDate.of(2024, 1, 1), LocalDate.of(2024, 12, 31), null)
    );
  }

  @Test
  void rejectsAnInvertedDateRange() {
    assertThrows(IllegalArgumentException.class, () ->
      new CalendarRequest("AAPL", LocalDate.parse("2024-02-01"), LocalDate.parse("2024-01-01"))
    );
  }
}
