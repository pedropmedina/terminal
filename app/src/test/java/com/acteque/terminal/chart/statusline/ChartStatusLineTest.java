package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.tooltip.Tooltip;
import java.net.URI;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.SVGPath;
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
  void displaysAFixedSizeFallbackUntilTheLogoArrives() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      FlowPane statusLine = view(feature);
      StackPane root = new StackPane(statusLine);
      Scene scene = new Scene(root, 800, 500);
      AppThemeManager themes = new AppThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();
      root.layout();
      Button instrument = triggerTarget(instrumentTooltip(statusLine));
      StackPane slot = assertInstanceOf(StackPane.class, instrument.getGraphic());
      assertEquals("A", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
      feature.setInstrumentLogo(LOGO, new WritableImage(64, 64));
      root.applyCss();
      root.layout();
      slot = assertInstanceOf(StackPane.class, instrument.getGraphic());
      assertEquals(20, slot.getWidth());
      assertEquals(20, slot.getHeight());
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
      FlowPane statusLine = assertInstanceOf(FlowPane.class, builder.build());
      WritableImage image = new WritableImage(64, 64);
      interactor.setInstrumentLogo(LOGO, image);
      interactor.setPricePoint(PRICE_POINT);
      Button originalInstrument = triggerTarget(instrumentTooltip(statusLine));
      SVGPath originalSeparator = selectionSeparator(statusLine);

      builder.refreshView();

      Button refreshedInstrument = triggerTarget(instrumentTooltip(statusLine));
      assertNotSame(originalInstrument, refreshedInstrument);
      assertSame(originalSeparator, selectionSeparator(statusLine));
      StackPane slot = assertInstanceOf(StackPane.class, refreshedInstrument.getGraphic());
      assertSame(image, assertInstanceOf(ImageView.class, slot.getChildren().getFirst()).getImage());
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());

      interactor.setInstrumentName("Widget Industries");
      slot = assertInstanceOf(StackPane.class, refreshedInstrument.getGraphic());
      assertEquals("W", assertInstanceOf(Label.class, slot.getChildren().getFirst()).getText());
    });
  }

  @Test
  void displaysTheSelectedPricePointInAChartOverlay() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      FlowPane statusLine = view(feature);
      feature.setPricePoint(PRICE_POINT);

      assertEquals(2, statusLine.getChildren().size());
      assertTrue(instrument(statusLine).getStyleClass().contains("chart-status-line-selection-group"));
      assertTrue(metadata(statusLine).getStyleClass().contains("chart-status-line-metadata-group"));
      assertEquals(4, instrument(statusLine).getChildren().size());
      assertSame(identifier(statusLine), instrument(statusLine).getChildren().get(0));
      Tooltip instrumentTooltip = instrumentTooltip(statusLine);
      assertSame(instrumentTooltip, instrument(statusLine).getChildren().get(1));
      Button instrumentButton = triggerTarget(instrumentTooltip);
      assertEquals("ACME", instrumentButton.getText());
      assertEquals(Variant.GHOST, instrumentButton.getVariant());
      assertEquals(Size.DEFAULT, instrumentButton.getSize());
      assertFalse(instrumentButton.getProperties().containsValue(instrumentTooltip));
      assertEquals(1, instrumentTooltip.getContentNodes().size());
      assertEquals(
        "Select a different instrument",
        assertInstanceOf(Label.class, instrumentTooltip.getContentNodes().getFirst()).getText()
      );

      SVGPath separator = selectionSeparator(statusLine);
      assertEquals(LucideIcons.DOT.pathData(), separator.getContent());
      assertTrue(separator.isMouseTransparent());

      Tooltip intervalTooltip = intervalTooltip(statusLine);
      assertSame(intervalTooltip, instrument(statusLine).getChildren().get(3));
      Button intervalButton = triggerTarget(intervalTooltip);
      assertEquals("Daily", intervalButton.getText());
      assertEquals("Select interval, currently Daily", intervalButton.getAccessibleText());
      assertEquals(Variant.GHOST, intervalButton.getVariant());
      assertFalse(intervalButton.getProperties().containsValue(intervalTooltip));
      assertEquals(1, intervalTooltip.getContentNodes().size());
      assertEquals(
        "Select a different interval",
        assertInstanceOf(Label.class, intervalTooltip.getContentNodes().getFirst()).getText()
      );
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());
    });
  }

  @Test
  void wrapsTheMetadataAsAWholeBlockWhenWidthIsConstrained() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      FlowPane statusLine = view(feature);
      feature.setPricePoint(PRICE_POINT);
      StackPane host = new StackPane(statusLine);
      Scene scene = new Scene(host, 800, 200);
      new AppThemeManager(scene, AppTheme.LIGHT);
      host.applyCss();

      HBox instrument = instrument(statusLine);
      HBox metadata = metadata(statusLine);
      double wideWidth = instrument.prefWidth(-1) + statusLine.getHgap() + metadata.prefWidth(-1) + 2.0;
      statusLine.resize(wideWidth, statusLine.prefHeight(wideWidth));
      statusLine.layout();

      assertEquals(instrument.getBoundsInParent().getCenterY(), metadata.getBoundsInParent().getCenterY(), 0.01);

      double narrowWidth = Math.max(instrument.prefWidth(-1), metadata.prefWidth(-1));
      statusLine.resize(narrowWidth, statusLine.prefHeight(narrowWidth));
      statusLine.layout();

      assertTrue(metadata.getBoundsInParent().getMinY() >= instrument.getBoundsInParent().getMaxY());
      assertTrue(metadata.getBoundsInParent().getMaxX() <= statusLine.getWidth() + 0.01);
      assertSame(ohlcv(statusLine), metadata.getChildren().getFirst());
    });
  }

  @Test
  void keepsTheSeparatorFlushWithTheSelectionButtons() {
    FxTestSupport.runAndWait(() -> {
      FlowPane statusLine = view(statusLine());
      StackPane root = new StackPane(statusLine);
      Scene scene = new Scene(root, 800, 200);
      new AppThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();
      root.layout();

      HBox selectionGroup = instrument(statusLine);
      Tooltip instrumentTooltip = instrumentTooltip(statusLine);
      SVGPath separator = selectionSeparator(statusLine);
      Tooltip intervalTooltip = intervalTooltip(statusLine);
      Button instrumentButton = triggerTarget(instrumentTooltip);
      Button intervalButton = triggerTarget(intervalTooltip);

      assertEquals(0.0, selectionGroup.getSpacing());
      assertNull(HBox.getMargin(separator));
      assertTrue(separator.getLayoutBounds().getWidth() < 8.0);
      assertEquals(3.0, instrumentButton.getPadding().getRight());
      assertEquals(3.0, intervalButton.getPadding().getLeft());
      assertEquals(3.0, instrumentButton.getPadding().getLeft());
      assertEquals(3.0, intervalButton.getPadding().getRight());
      assertEquals(instrumentTooltip.getBoundsInParent().getMaxX(), separator.getBoundsInParent().getMinX(), 0.01);
      assertEquals(separator.getBoundsInParent().getMaxX(), intervalTooltip.getBoundsInParent().getMinX(), 0.01);
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
      FlowPane statusLine = view(feature);

      triggerTarget(instrumentTooltip(statusLine)).fire();
      triggerTarget(intervalTooltip(statusLine)).fire();

      assertTrue(instrumentClicked.get());
      assertTrue(intervalClicked.get());
    });
  }

  @Test
  void updatesBoundIntervalAndInstrumentNodes() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLine feature = statusLine();
      FlowPane statusLine = view(feature);
      feature.setPricePoint(PRICE_POINT);

      feature.setInterval(ChartInterval.FIVE_MINUTES);
      feature.setInstrumentName("Widget Industries");

      Button intervalButton = triggerTarget(intervalTooltip(statusLine));
      assertEquals("5 minutes", intervalButton.getText());
      assertEquals("Select interval, currently 5 minutes", intervalButton.getAccessibleText());
      assertEquals("Widget Industries", triggerTarget(instrumentTooltip(statusLine)).getText());
      assertEquals("O104.00  H108.25  L103.50  C107.75  Vol2.50 M", ohlcv(statusLine).getText());

      feature.clearPricePoint();
      assertEquals("", ohlcv(statusLine).getText());
    });
  }

  private static ChartStatusLine statusLine() {
    return new ChartStatusLine("ACME", ChartInterval.DAILY, new SimpleBooleanProperty(false), () -> {}, () -> {});
  }

  private static FlowPane view(ChartStatusLine feature) {
    Region firstView = feature.getView();
    assertSame(firstView, feature.getView());
    return assertInstanceOf(FlowPane.class, firstView);
  }

  private static HBox instrument(FlowPane statusLine) {
    return assertInstanceOf(HBox.class, statusLine.getChildren().get(0));
  }

  private static HBox metadata(FlowPane statusLine) {
    return assertInstanceOf(HBox.class, statusLine.getChildren().get(1));
  }

  private static Region identifier(FlowPane statusLine) {
    return assertInstanceOf(Region.class, instrument(statusLine).getChildren().get(0));
  }

  private static Tooltip instrumentTooltip(FlowPane statusLine) {
    return assertInstanceOf(Tooltip.class, instrument(statusLine).getChildren().get(1));
  }

  private static Tooltip intervalTooltip(FlowPane statusLine) {
    return assertInstanceOf(Tooltip.class, instrument(statusLine).getChildren().get(3));
  }

  private static SVGPath selectionSeparator(FlowPane statusLine) {
    return assertInstanceOf(SVGPath.class, instrument(statusLine).getChildren().get(2));
  }

  private static Label ohlcv(FlowPane statusLine) {
    return assertInstanceOf(Label.class, metadata(statusLine).getChildren().getFirst());
  }

  private static Button triggerTarget(Tooltip tooltip) {
    return assertInstanceOf(Button.class, tooltip.getTrigger().getTarget());
  }
}
