package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
  void displaysAndClearsTheSelectedPricePoint() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = new ChartStatusLine();
      HBox statusLine = view(feature);

      feature.setPricePoint(PRICE_POINT);

      assertEquals(1, statusLine.getChildren().size());
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", metadata(statusLine).getText());
      assertEquals("Open, high, low, close, and volume", metadata(statusLine).getAccessibleText());
      assertTrue(metadata(statusLine).isMouseTransparent());

      feature.clearPricePoint();

      assertEquals("", metadata(statusLine).getText());
    });
  }

  @Test
  void preservesMetadataAcrossViewRefreshes() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
      ChartStatusLineViewBuilder builder = new ChartStatusLineViewBuilder(model);
      HBox statusLine = assertInstanceOf(HBox.class, builder.build());
      interactor.setPricePoint(PRICE_POINT);
      Label original = metadata(statusLine);

      builder.refreshView();

      assertNotSame(original, metadata(statusLine));
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", metadata(statusLine).getText());
    });
  }

  @Test
  void exposesOneReusablePassiveMetadataView() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = new ChartStatusLine();
      HBox statusLine = view(feature);

      assertSame(statusLine, feature.getView());
      assertFalse(statusLine.isPickOnBounds());
      assertEquals(1, statusLine.getChildren().size());
      assertEquals(0, statusLine.lookupAll(".core-button").size());
    });
  }

  private static HBox view(ChartStatusLine feature) {
    Region firstView = feature.getView();
    assertSame(firstView, feature.getView());
    return assertInstanceOf(HBox.class, firstView);
  }

  private static Label metadata(HBox statusLine) {
    return assertInstanceOf(Label.class, statusLine.getChildren().getFirst());
  }
}
