package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.control.Label;
import org.junit.jupiter.api.Test;

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
      Button symbolButton = assertInstanceOf(Button.class, statusLine.getChildren().get(0));
      assertEquals("ACME", symbolButton.getText());
      assertEquals(Variant.GHOST, symbolButton.getVariant());
      assertEquals(Size.DEFAULT, symbolButton.getSize());
      Button intervalButton = assertInstanceOf(Button.class, statusLine.getChildren().get(1));
      assertEquals("1D", intervalButton.getText());
      assertEquals(Variant.GHOST, intervalButton.getVariant());
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

  @Test
  void handlesIntervalClicks() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      AtomicBoolean clicked = new AtomicBoolean();
      statusLine.onIntervalClick(() -> clicked.set(true));

      assertInstanceOf(Button.class, statusLine.getChildren().get(1)).fire();

      assertTrue(clicked.get());
    });
  }

  @Test
  void updatesTheDisplayedInterval() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);

      statusLine.setInterval(ChartInterval.FIVE_MINUTES);

      Button intervalButton = assertInstanceOf(Button.class, statusLine.getChildren().get(1));
      assertEquals("5M", intervalButton.getText());
      assertEquals("Select interval, currently 5M", intervalButton.getAccessibleText());
      assertEquals("ACME  5M   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));
    });
  }

  @Test
  void updatesTheDisplayedInstrument() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);

      statusLine.setStockSymbol("WIDGET");

      Button symbolButton = assertInstanceOf(Button.class, statusLine.getChildren().get(0));
      assertEquals("WIDGET", symbolButton.getText());
      assertEquals("WIDGET  1D   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));
    });
  }
}
