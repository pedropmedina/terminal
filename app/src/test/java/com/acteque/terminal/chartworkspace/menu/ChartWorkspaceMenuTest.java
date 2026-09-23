package com.acteque.terminal.chartworkspace.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.Separator;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.kbd.Kbd;
import com.acteque.terminal.ui.kbd.KbdGroup;
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.net.URI;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

class ChartWorkspaceMenuTest {

  private static final InstrumentLogo LOGO = new InstrumentLogo(
    URI.create("https://images.example.com/IBM.png"),
    "Logos by Example",
    URI.create("https://example.com")
  );
  private static final byte[] PNG = Base64.getDecoder().decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR4nGP4z8DwHwAFAAH/iZk9HQAAAABJRU5ErkJggg=="
  );

  @Test
  void followsTheSelectedChartAndShowsMultiChartActions() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart first = chart("IBM");
        Chart second = chart("MSFT");
        ChartWorkspaceMenu menu = new ChartWorkspaceMenu(first)
      ) {
        ChartWorkspaceMenuItems root = assertInstanceOf(ChartWorkspaceMenuItems.class, menu.getView());
        HBox buttons = root.getButtons();
        StackPane sceneRoot = new StackPane(root);
        new AppThemeManager(new Scene(sceneRoot, 500.0, 100.0), AppTheme.LIGHT);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("IBM", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
        assertEquals(5, buttons.getChildren().size());
        Separator separator = assertInstanceOf(Separator.class, buttons.getChildren().get(3));
        assertEquals(Orientation.VERTICAL, separator.getOrientation());
        assertEquals(20.0, separator.getHeight());

        Color color = Color.hsb(210.0, 0.72, 0.85);
        second.setIdentifierColor(color);
        second.setInstrument("AAPL", List.of(), Optional.empty());
        second.setChartType(ChartType.AREA);
        menu.setActiveChart(second);
        menu.setMultipleCharts(true);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("AAPL", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
        assertEquals(6, buttons.getChildren().size());
        assertNull(root.getClip());

        first.setInstrument("IGNORED", List.of(), Optional.empty());
        assertEquals("AAPL", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
      }
    });
  }

  @Test
  void showsTheActiveChartLogoInAnIdentifierColoredButtonAndFallsBackToTheSymbol() {
    FxTestSupport.runAndWait(() -> {
      CompletableFuture<Optional<byte[]>> pendingLogo = new CompletableFuture<>();
      LogoSession logos = ignored -> pendingLogo;
      try (
        Chart first = new Chart("IBM", ChartInterval.DAILY, List.of(), logos, Runnable::run);
        Chart second = chart("MSFT");
        ChartWorkspaceMenu menu = new ChartWorkspaceMenu(first)
      ) {
        Color firstColor = Color.hsb(24.0, 0.72, 0.85);
        Color secondColor = Color.hsb(210.0, 0.72, 0.85);
        first.setIdentifierColor(firstColor);
        second.setIdentifierColor(secondColor);
        first.setInstrument("IBM", List.of(), Optional.of(LOGO));

        ChartWorkspaceMenuItems root = assertInstanceOf(ChartWorkspaceMenuItems.class, menu.getView());
        Button instrument = assertInstanceOf(Button.class, root.getButtons().getChildren().getFirst());
        StackPane sceneRoot = new StackPane(root);
        new AppThemeManager(new Scene(sceneRoot, 500.0, 100.0), AppTheme.LIGHT);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("IBM", instrument.getText());
        assertEquals(Size.ICON, instrument.getSize());
        assertEquals(OverrunStyle.ELLIPSIS, instrument.getTextOverrun());
        assertEquals(30.0, instrument.getWidth());
        assertEquals(30.0, instrument.getHeight());
        assertEquals(Color.TRANSPARENT, instrument.getBackground().getFills().getFirst().getFill());
        assertEquals(Color.web("#0a0a0a"), instrument.getTextFill());
        assertColorEquals(
          firstColor,
          assertInstanceOf(Color.class, instrument.getBorder().getStrokes().getFirst().getTopStroke())
        );
        assertEquals(2.5, instrument.getBorder().getStrokes().getFirst().getWidths().getTop());
        assertTrue(
          instrument.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius() >=
            instrument.getHeight() / 2.0
        );

        menu.setActiveChart(second);
        pendingLogo.complete(Optional.of(PNG));
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("MSFT", instrument.getText());
        assertEquals(Size.ICON, instrument.getSize());
        assertEquals(30.0, instrument.getWidth());
        assertEquals(30.0, instrument.getHeight());
        assertColorEquals(
          secondColor,
          assertInstanceOf(Color.class, instrument.getBorder().getStrokes().getFirst().getTopStroke())
        );

        menu.setActiveChart(first);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("", instrument.getText());
        assertEquals(Size.ICON, instrument.getSize());
        Region logo = assertInstanceOf(Region.class, instrument.getGraphic());
        BackgroundImage logoBackground = logo.getBackground().getImages().getFirst();
        assertSame(first.instrumentLogoImageProperty().getValue(), logoBackground.getImage());
        assertEquals(BackgroundRepeat.NO_REPEAT, logoBackground.getRepeatX());
        assertEquals(BackgroundRepeat.NO_REPEAT, logoBackground.getRepeatY());
        assertEquals(BackgroundPosition.CENTER, logoBackground.getPosition());
        assertTrue(logoBackground.getSize().isCover());
        assertEquals(25.0, logo.getWidth());
        assertEquals(25.0, logo.getHeight());
        assertInstanceOf(Circle.class, logo.getClip());
        assertEquals(30.0, instrument.getWidth());
        assertEquals(30.0, instrument.getHeight());
        assertColorEquals(
          firstColor,
          assertInstanceOf(Color.class, instrument.getBorder().getStrokes().getFirst().getTopStroke())
        );
        assertEquals("Select symbol or instrument, currently IBM", instrument.getAccessibleText());
      }
    });
  }

  @Test
  void observesLogosLoadedByTheActiveChart() {
    FxTestSupport.runAndWait(() -> {
      LogoSession logos = ignored -> CompletableFuture.completedFuture(Optional.of(PNG));
      try (
        Chart chart = new Chart("IBM", ChartInterval.DAILY, List.of(), logos, Runnable::run);
        ChartWorkspaceMenu menu = new ChartWorkspaceMenu(chart)
      ) {
        ChartWorkspaceMenuItems root = assertInstanceOf(ChartWorkspaceMenuItems.class, menu.getView());
        Button instrument = assertInstanceOf(Button.class, root.getButtons().getChildren().getFirst());
        assertEquals("IBM", instrument.getText());
        assertNull(instrument.getGraphic());

        chart.setInstrument("IBM", List.of(), Optional.of(LOGO));

        assertEquals("", instrument.getText());
        assertInstanceOf(Region.class, instrument.getGraphic());
      }
    });
  }

  @Test
  void routesEveryActionWithoutCapturingAChart() {
    FxTestSupport.runAndWait(() -> {
      try (Chart chart = chart("IBM"); ChartWorkspaceMenu menu = new ChartWorkspaceMenu(chart)) {
        AtomicInteger instrumentRequests = new AtomicInteger();
        AtomicInteger intervalRequests = new AtomicInteger();
        AtomicInteger chartTypeRequests = new AtomicInteger();
        AtomicInteger closes = new AtomicInteger();
        List<ChartSplitDirection> splits = new ArrayList<>();
        menu.onInstrumentSelectionRequested(instrumentRequests::incrementAndGet);
        menu.onIntervalSelectionRequested(intervalRequests::incrementAndGet);
        menu.onChartTypeSelectionRequested(chartTypeRequests::incrementAndGet);
        menu.onSplitRequested(splits::add);
        menu.onCloseRequested(closes::incrementAndGet);
        menu.setMultipleCharts(true);

        ChartWorkspaceMenuItems root = assertInstanceOf(ChartWorkspaceMenuItems.class, menu.getView());
        HBox buttons = root.getButtons();
        assertInstanceOf(Button.class, buttons.getChildren().get(0)).fire();
        assertInstanceOf(Button.class, buttons.getChildren().get(1)).fire();
        assertInstanceOf(Button.class, buttons.getChildren().get(2)).fire();
        Separator separator = assertInstanceOf(Separator.class, buttons.getChildren().get(3));
        assertEquals(Orientation.VERTICAL, separator.getOrientation());
        PopoverTrigger split = assertInstanceOf(PopoverTrigger.class, buttons.getChildren().get(4));
        assertSame(
          LucideIcons.SQUARE_SPLIT_HORIZONTAL,
          assertInstanceOf(LucideIcon.class, split.getGraphic()).getGlyph()
        );
        VBox splitActions = assertInstanceOf(VBox.class, split.getPopover().getContent().getChildren().getFirst());
        List<Button> splitButtons = splitActions.getChildren().stream().map(Button.class::cast).toList();
        assertEquals(
          List.of("Split right", "Split down", "Split up", "Split left"),
          splitButtons.stream().map(ChartWorkspaceMenuTest::splitActionLabel).toList()
        );
        assertEquals(
          List.of("L", "J", "K", "H"),
          splitButtons.stream().map(ChartWorkspaceMenuTest::shortcutKey).toList()
        );
        splitButtons.forEach(Button::fire);
        Button close = assertInstanceOf(Button.class, buttons.getChildren().get(5));
        assertEquals(Variant.GHOST, close.getVariant());
        assertSame(LucideIcons.X, assertInstanceOf(LucideIcon.class, close.getGraphic()).getGlyph());
        close.fire();

        assertEquals(1, instrumentRequests.get());
        assertEquals(1, intervalRequests.get());
        assertEquals(1, chartTypeRequests.get());
        assertEquals(1, closes.get());
        assertEquals(
          List.of(
            ChartSplitDirection.RIGHT,
            ChartSplitDirection.BOTTOM,
            ChartSplitDirection.TOP,
            ChartSplitDirection.LEFT
          ),
          splits
        );
        assertSame(root, menu.getView());
      }
    });
  }

  private static Chart chart(String symbol) {
    return new Chart(symbol, ChartInterval.DAILY, List.of());
  }

  private static String splitActionLabel(Button button) {
    HBox content = assertInstanceOf(HBox.class, button.getGraphic());
    assertEquals(3, content.getChildren().size());
    assertSame(Priority.ALWAYS, HBox.getHgrow(content.getChildren().get(1)));
    return assertInstanceOf(Label.class, content.getChildren().getFirst()).getText();
  }

  private static String shortcutKey(Button button) {
    HBox content = assertInstanceOf(HBox.class, button.getGraphic());
    KbdGroup group = assertInstanceOf(KbdGroup.class, content.getChildren().getLast());
    assertEquals(2, group.getChildren().size());
    assertEquals("⌘", assertInstanceOf(Kbd.class, group.getChildren().getFirst()).getText());
    return assertInstanceOf(Kbd.class, group.getChildren().getLast()).getText();
  }

  private static void assertColorEquals(Color expected, Color actual) {
    assertEquals(expected.getRed(), actual.getRed(), 0.000001);
    assertEquals(expected.getGreen(), actual.getGreen(), 0.000001);
    assertEquals(expected.getBlue(), actual.getBlue(), 0.000001);
    assertEquals(expected.getOpacity(), actual.getOpacity(), 0.000001);
  }
}
