package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import com.acteque.terminal.ui.core.tooltip.Tooltip;
import java.net.URI;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
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

  private static final InstrumentLogo LOGO = new InstrumentLogo(
    URI.create("https://images.example.com/ACME.png"),
    "Logos by Example",
    URI.create("https://example.com")
  );

  @Test
  void displaysAFixedSizeFallbackWithoutAttributionUntilTheLogoArrives() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      StackPane root = new StackPane(statusLine);
      Scene scene = new Scene(root, 800, 500);
      ThemeManager themes = new ThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();
      root.layout();
      Button symbol = triggerTarget(statusLine.symbolTooltip());
      StackPane slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertEquals("A", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
      assertFalse(statusLine.logoAttribution().isVisible());
      assertFalse(statusLine.logoAttribution().isManaged());

      statusLine.setInstrumentLogo(LOGO, new WritableImage(64, 64));
      root.applyCss();
      root.layout();
      slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
      assertTrue(statusLine.logoAttribution().isVisible());
      assertTrue(statusLine.logoAttribution().isManaged());
      assertEquals("Logos by Example", statusLine.logoAttribution().getText());
      VBox tooltipContent = assertInstanceOf(VBox.class, statusLine.symbolTooltip().getContentNodes().getFirst());
      assertSame(statusLine.logoAttribution(), tooltipContent.getChildren().get(1));
      themes.setTheme(AppTheme.DARK);
      root.applyCss();
    });
  }

  @Test
  void preservesTheLogoAndTooltipAttributionAcrossViewRefreshesAndClearsThemOnInstrumentChanges() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      WritableImage image = new WritableImage(64, 64);
      statusLine.setInstrumentLogo(LOGO, image);
      statusLine.refreshView();

      Button symbol = triggerTarget(statusLine.symbolTooltip());
      StackPane slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertSame(image, assertInstanceOf(ImageView.class, slot.getChildren().getFirst()).getImage());
      assertEquals(LOGO.attributionText(), statusLine.logoAttribution().getText());
      assertFalse(symbol.getProperties().containsValue(statusLine.symbolTooltip()));

      statusLine.setInstrumentName("Widget Industries");
      slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertEquals("W", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
      assertFalse(statusLine.logoAttribution().isVisible());
      statusLine.refreshView();
      assertFalse(statusLine.logoAttribution().isVisible());
    });
  }

  @Test
  void formatsLongIntervalNamesForEveryClassification() {
    String[] singularNames = { "1 tick", "1 second", "1 minute", "1 hour", "Daily", "Weekly", "Monthly" };
    String[] pluralNames = { "7 ticks", "7 seconds", "7 minutes", "7 hours", "7 days", "7 weeks", "7 months" };
    for (ChartInterval.Classification classification : ChartInterval.Classification.values()) {
      assertEquals(singularNames[classification.ordinal()], ChartInterval.of(1, classification).displayName());
      assertEquals(pluralNames[classification.ordinal()], ChartInterval.of(7, classification).displayName());
    }
    assertEquals("Daily", ChartInterval.DAILY.displayName());
    assertEquals("Weekly", ChartInterval.WEEKLY.displayName());
    assertEquals("Monthly", ChartInterval.MONTHLY.displayName());
    assertEquals("5 minutes", ChartInterval.FIVE_MINUTES.displayName());
    assertEquals("3 months", ChartInterval.THREE_MONTHS.displayName());
    assertEquals("1D", ChartInterval.DAILY.name());
    assertEquals("5M", ChartInterval.FIVE_MINUTES.name());
  }

  @Test
  void formatsTheSelectedPricePoint() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      assertEquals("ACME  Daily   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));
    });
  }

  @Test
  void displaysTheSelectedPricePointInAChartOverlay() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      statusLine.setPricePoint(PRICE_POINT);

      assertEquals(3, statusLine.getChildren().size());
      assertSame(statusLine.symbolTooltip(), statusLine.getChildren().get(0));
      Button symbolButton = triggerTarget(statusLine.symbolTooltip());
      assertEquals("ACME", symbolButton.getText());
      assertEquals(Variant.GHOST, symbolButton.getVariant());
      assertEquals(Size.DEFAULT, symbolButton.getSize());
      assertFalse(symbolButton.getProperties().containsValue(statusLine.symbolTooltip()));
      VBox symbolTooltipContent = assertInstanceOf(VBox.class, statusLine.symbolTooltip().getContentNodes().getFirst());
      assertEquals(
        "Click to select a different symbol",
        assertInstanceOf(Label.class, symbolTooltipContent.getChildren().getFirst()).getText()
      );
      assertSame(statusLine.logoAttribution(), symbolTooltipContent.getChildren().get(1));
      assertSame(statusLine.intervalTooltip(), statusLine.getChildren().get(1));
      Button intervalButton = triggerTarget(statusLine.intervalTooltip());
      assertEquals("Daily", intervalButton.getText());
      assertEquals("Select interval, currently Daily", intervalButton.getAccessibleText());
      assertEquals(Variant.GHOST, intervalButton.getVariant());
      assertFalse(intervalButton.getProperties().containsValue(statusLine.intervalTooltip()));
      assertEquals(
        "Click to select a different interval",
        assertInstanceOf(Label.class, statusLine.intervalTooltip().getContentNodes().getFirst()).getText()
      );
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

      triggerTarget(statusLine.symbolTooltip()).fire();

      assertTrue(clicked.get());
    });
  }

  @Test
  void handlesIntervalClicks() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);
      AtomicBoolean clicked = new AtomicBoolean();
      statusLine.onIntervalClick(() -> clicked.set(true));

      triggerTarget(statusLine.intervalTooltip()).fire();

      assertTrue(clicked.get());
    });
  }

  @Test
  void updatesTheDisplayedInterval() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);

      statusLine.setInterval(ChartInterval.FIVE_MINUTES);

      Button intervalButton = triggerTarget(statusLine.intervalTooltip());
      assertEquals("5 minutes", intervalButton.getText());
      assertEquals("Select interval, currently 5 minutes", intervalButton.getAccessibleText());
      assertEquals("ACME  5 minutes   O104.00  H108.25  L103.50  C107.75  Vol2.50 M", statusLine.text(PRICE_POINT));

      statusLine.refreshView();
      Button refreshedButton = triggerTarget(statusLine.intervalTooltip());
      assertEquals("5 minutes", refreshedButton.getText());
      assertEquals("Select interval, currently 5 minutes", refreshedButton.getAccessibleText());
    });
  }

  @Test
  void updatesTheDisplayedInstrument() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine statusLine = new ChartStatusLine("ACME", ChartInterval.DAILY);

      statusLine.setInstrumentName("Widget Industries");

      Button symbolButton = triggerTarget(statusLine.symbolTooltip());
      assertEquals("Widget Industries", symbolButton.getText());
      assertEquals(
        "Widget Industries  Daily   O104.00  H108.25  L103.50  C107.75  Vol2.50 M",
        statusLine.text(PRICE_POINT)
      );

      statusLine.refreshView();
      assertEquals("Widget Industries", triggerTarget(statusLine.symbolTooltip()).getText());
    });
  }

  private static Button triggerTarget(Tooltip tooltip) {
    return assertInstanceOf(Button.class, tooltip.getTrigger().getTarget());
  }
}
