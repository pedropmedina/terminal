package com.acteque.terminal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
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

  private final ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);

  @Test
  void formatsTheSelectedPricePoint() {
    assertEquals("ACME  1D   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));
  }

  @Test
  void displaysTheSelectedPricePointInAChartOverlay() {
    statusLine.setPricePoint(PRICE_POINT);

    assertInstanceOf(HBox.class, statusLine);
    assertEquals(3, statusLine.getChildren().size());
    assertEquals("ACME", sectionText(0));
    assertEquals("1D", sectionText(1));
    assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", sectionText(2));
  }

  @Test
  void handlesInstrumentClicks() {
    AtomicBoolean clicked = new AtomicBoolean();
    statusLine.onInstrumentClick(() -> clicked.set(true));

    instrumentSection().getOnMouseClicked().handle(null);

    assertTrue(clicked.get());
  }

  private String sectionText(int sectionIndex) {
    StackPane section = assertInstanceOf(StackPane.class, statusLine.getChildren().get(sectionIndex));
    return assertInstanceOf(Text.class, section.getChildren().get(0)).getText();
  }

  private StackPane instrumentSection() {
    return assertInstanceOf(StackPane.class, statusLine.getChildren().get(0));
  }
}
