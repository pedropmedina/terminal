package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class PriceChartStatusLineTest {

  private static final PricePoint PRICE_POINT = new PricePoint(
    LocalDate.of(2026, 8, 24),
    104.00,
    108.25,
    103.50,
    107.75,
    2_500_000
  );

  private final PriceChartStatusLine statusLine = new PriceChartStatusLine("ACME", ChartInterval.DAILY);

  @Test
  void formatsTheSelectedPricePoint() {
    assertEquals(
      "ACME  1D   O104.00  H108.25  L103.50  C107.75  Vol2.50 M",
      statusLine.text(PRICE_POINT)
    );
  }
}
