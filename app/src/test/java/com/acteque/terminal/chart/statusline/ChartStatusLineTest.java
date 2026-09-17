package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.tooltip.Tooltip;
import java.net.URI;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
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
      ChartStatusLine feature = statusLine();
      HBox statusLine = view(feature);
      StackPane root = new StackPane(statusLine);
      Scene scene = new Scene(root, 800, 500);
      ThemeManager themes = new ThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();
      root.layout();
      Button symbol = triggerTarget(symbolTooltip(statusLine));
      StackPane slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertEquals("A", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
      Label attribution = logoAttribution(statusLine);
      assertFalse(attribution.isVisible());
      assertFalse(attribution.isManaged());

      feature.setInstrumentLogo(LOGO, new WritableImage(64, 64));
      root.applyCss();
      root.layout();
      slot = assertInstanceOf(StackPane.class, symbol.getGraphic());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
      assertTrue(attribution.isVisible());
      assertTrue(attribution.isManaged());
      assertEquals("Logos by Example", attribution.getText());
      assertSame(attribution, symbolTooltipContent(statusLine).getChildren().get(2));
      themes.setTheme(AppTheme.DARK);
      root.applyCss();
    });
  }

  @Test
  void preservesStateAcrossViewRefreshesAndClearsTheLogoOnInstrumentChanges() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
      interactor.initialize("ACME", ChartInterval.DAILY);
      ChartStatusLineViewBuilder builder = new ChartStatusLineViewBuilder(
        model,
        new SimpleBooleanProperty(false),
        () -> {},
        () -> {}
      );
      HBox statusLine = assertInstanceOf(HBox.class, builder.build());
      WritableImage image = new WritableImage(64, 64);
      interactor.setInstrumentLogo(LOGO, image);
      interactor.setPricePoint(PRICE_POINT);
      Button originalSymbol = triggerTarget(symbolTooltip(statusLine));

      builder.refreshView();

      Button refreshedSymbol = triggerTarget(symbolTooltip(statusLine));
      assertNotSame(originalSymbol, refreshedSymbol);
      StackPane slot = assertInstanceOf(StackPane.class, refreshedSymbol.getGraphic());
      assertSame(image, assertInstanceOf(ImageView.class, slot.getChildren().getFirst()).getImage());
      assertEquals(LOGO.attributionText(), logoAttribution(statusLine).getText());
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());

      interactor.setInstrumentName("Widget Industries");
      slot = assertInstanceOf(StackPane.class, refreshedSymbol.getGraphic());
      assertEquals("W", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
      assertFalse(logoAttribution(statusLine).isVisible());
    });
  }

  @Test
  void displaysTheSelectedPricePointInAChartOverlay() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      HBox statusLine = view(feature);
      feature.setPricePoint(PRICE_POINT);

      assertEquals(3, statusLine.getChildren().size());
      Tooltip symbolTooltip = symbolTooltip(statusLine);
      assertSame(symbolTooltip, statusLine.getChildren().get(0));
      Button symbolButton = triggerTarget(symbolTooltip);
      assertEquals("ACME", symbolButton.getText());
      assertEquals(Variant.GHOST, symbolButton.getVariant());
      assertEquals(Size.DEFAULT, symbolButton.getSize());
      assertFalse(symbolButton.getProperties().containsValue(symbolTooltip));
      VBox symbolTooltipContent = symbolTooltipContent(statusLine);
      assertEquals(
        "Click to select a different symbol",
        assertInstanceOf(Label.class, symbolTooltipContent.getChildren().getFirst()).getText()
      );
      assertEquals(
        "Shortcut: ⌘F, ⌘/, or ⌘P",
        assertInstanceOf(Label.class, symbolTooltipContent.getChildren().get(1)).getText()
      );
      assertSame(logoAttribution(statusLine), symbolTooltipContent.getChildren().get(2));

      Tooltip intervalTooltip = intervalTooltip(statusLine);
      assertSame(intervalTooltip, statusLine.getChildren().get(1));
      Button intervalButton = triggerTarget(intervalTooltip);
      assertEquals("Daily", intervalButton.getText());
      assertEquals("Select interval, currently Daily", intervalButton.getAccessibleText());
      assertEquals(Variant.GHOST, intervalButton.getVariant());
      assertFalse(intervalButton.getProperties().containsValue(intervalTooltip));
      VBox intervalTooltipContent = assertInstanceOf(VBox.class, intervalTooltip.getContentNodes().getFirst());
      assertEquals(
        "Click to select a different interval",
        assertInstanceOf(Label.class, intervalTooltipContent.getChildren().getFirst()).getText()
      );
      assertEquals(
        "Shortcut: ⌘I",
        assertInstanceOf(Label.class, intervalTooltipContent.getChildren().get(1)).getText()
      );
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());
    });
  }

  @Test
  void handlesInstrumentAndIntervalClicks() {
    FxTestSupport.runAndWait(() -> {
      AtomicBoolean instrumentClicked = new AtomicBoolean();
      AtomicBoolean intervalClicked = new AtomicBoolean();
      ChartStatusLine feature = new ChartStatusLine(
        "ACME",
        ChartInterval.DAILY,
        new SimpleBooleanProperty(false),
        () -> instrumentClicked.set(true),
        () -> intervalClicked.set(true)
      );
      HBox statusLine = view(feature);

      triggerTarget(symbolTooltip(statusLine)).fire();
      triggerTarget(intervalTooltip(statusLine)).fire();

      assertTrue(instrumentClicked.get());
      assertTrue(intervalClicked.get());
    });
  }

  @Test
  void updatesBoundIntervalAndInstrumentNodes() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      HBox statusLine = view(feature);
      feature.setPricePoint(PRICE_POINT);

      feature.setInterval(ChartInterval.FIVE_MINUTES);
      feature.setInstrumentName("Widget Industries");

      Button intervalButton = triggerTarget(intervalTooltip(statusLine));
      assertEquals("5 minutes", intervalButton.getText());
      assertEquals("Select interval, currently 5 minutes", intervalButton.getAccessibleText());
      assertEquals("Widget Industries", triggerTarget(symbolTooltip(statusLine)).getText());
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());

      feature.clearPricePoint();
      assertEquals("", ohlcv(statusLine).getText());
    });
  }

  private static ChartStatusLine statusLine() {
    return new ChartStatusLine("ACME", ChartInterval.DAILY, new SimpleBooleanProperty(false), () -> {}, () -> {});
  }

  private static HBox view(ChartStatusLine feature) {
    Region firstView = feature.getView();
    assertSame(firstView, feature.getView());
    return assertInstanceOf(HBox.class, firstView);
  }

  private static Tooltip symbolTooltip(HBox statusLine) {
    return assertInstanceOf(Tooltip.class, statusLine.getChildren().get(0));
  }

  private static Tooltip intervalTooltip(HBox statusLine) {
    return assertInstanceOf(Tooltip.class, statusLine.getChildren().get(1));
  }

  private static VBox symbolTooltipContent(HBox statusLine) {
    return assertInstanceOf(VBox.class, symbolTooltip(statusLine).getContentNodes().getFirst());
  }

  private static Label logoAttribution(HBox statusLine) {
    return assertInstanceOf(Label.class, symbolTooltipContent(statusLine).getChildren().get(2));
  }

  private static Label ohlcv(HBox statusLine) {
    return assertInstanceOf(Label.class, statusLine.getChildren().get(2));
  }

  private static Button triggerTarget(Tooltip tooltip) {
    return assertInstanceOf(Button.class, tooltip.getTrigger().getTarget());
  }
}
