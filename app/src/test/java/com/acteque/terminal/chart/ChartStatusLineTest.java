package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import com.acteque.terminal.test.FxTestSupport;
import org.junit.jupiter.api.Test;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

class ChartStatusLineTest {

  private static final PricePoint PRICE_POINT = new PricePoint(
    LocalDate.of(2026, 8, 24),
    104.00,
    108.25,
    103.50,
    107.75,
    2_500_000
  );

  @Test
  void formatsTheSelectedPricePoint() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      assertEquals("ACME  1D   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));
    });
  }

  @Test
  void displaysTheSelectedPricePointInAChartOverlay() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      statusLine.setPricePoint(PRICE_POINT);

      assertEquals(3, statusLine.getChildren().size());
      assertEquals("ACME", assertInstanceOf(Button.class, statusLine.getChildren().get(0)).getText());
      assertEquals("1D", assertInstanceOf(Label.class, statusLine.getChildren().get(1)).getText());
      assertEquals(
        "O104.00  H108.25  L103.50  C107.75  Vol2.50 M",
        assertInstanceOf(Label.class, statusLine.getChildren().get(2)).getText()
      );
    });
  }

  @Test
  void handlesInstrumentClicks() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      AtomicBoolean clicked = new AtomicBoolean();
      statusLine.onInstrumentClick(() -> clicked.set(true));

      assertInstanceOf(Button.class, statusLine.getChildren().get(0)).fire();

      assertTrue(clicked.get());
    });
  }
}
