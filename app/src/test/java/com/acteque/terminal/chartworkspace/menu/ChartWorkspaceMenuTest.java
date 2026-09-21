package com.acteque.terminal.chartworkspace.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.Separator;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.kbd.Kbd;
import com.acteque.terminal.ui.kbd.KbdGroup;
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.junit.jupiter.api.Test;

class ChartWorkspaceMenuTest {

  @Test
  void followsTheSelectedChartAndShowsMultiChartActionsAndIdentifier() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart first = chart("IBM");
        Chart second = chart("MSFT");
        ChartWorkspaceMenu menu = new ChartWorkspaceMenu(first)
      ) {
        ChartWorkspaceMenuItems root = assertInstanceOf(ChartWorkspaceMenuItems.class, menu.getView());
        HBox buttons = root.getButtons();
        Region identifier = root.getIdentifier();
        StackPane sceneRoot = new StackPane(root);
        new AppThemeManager(new Scene(sceneRoot, 500.0, 100.0), AppTheme.LIGHT);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("IBM", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
        assertEquals(5, buttons.getChildren().size());
        Separator separator = assertInstanceOf(Separator.class, buttons.getChildren().get(3));
        assertEquals(Orientation.VERTICAL, separator.getOrientation());
        assertEquals(12.0, separator.getHeight());
        assertFalse(identifier.isVisible());

        Color color = Color.hsb(210.0, 0.72, 0.85);
        second.setIdentifierColor(color);
        second.setInstrument("AAPL", "Apple", List.of(), Optional.empty());
        second.setChartType(ChartType.AREA);
        menu.setActiveChart(second);
        menu.setMultipleCharts(true);
        sceneRoot.applyCss();
        sceneRoot.layout();

        assertEquals("AAPL", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
        assertEquals(6, buttons.getChildren().size());
        assertTrue(identifier.isVisible());
        assertEquals(4.0, identifier.getWidth());
        assertEquals(color, identifier.getBackground().getFills().getFirst().getFill());
        CornerRadii menuRadii = root.getBackground().getFills().getFirst().getRadii();
        CornerRadii identifierRadii = identifier.getBackground().getFills().getFirst().getRadii();
        assertEquals(0.0, identifierRadii.getTopLeftHorizontalRadius());
        assertEquals(0.0, identifierRadii.getBottomLeftHorizontalRadius());
        assertEquals(0.0, identifierRadii.getTopRightHorizontalRadius());
        assertEquals(0.0, identifierRadii.getBottomRightHorizontalRadius());
        assertFalse(identifier.isManaged());
        assertEquals(Pos.TOP_LEFT, StackPane.getAlignment(identifier));
        assertEquals(0.0, identifier.getLayoutX());
        assertEquals(0.0, identifier.getLayoutY());
        assertEquals(root.getHeight(), identifier.getHeight());
        Rectangle clip = assertInstanceOf(Rectangle.class, root.getClip());
        assertEquals(root.getWidth(), clip.getWidth());
        assertEquals(root.getHeight(), clip.getHeight());
        assertEquals(menuRadii.getTopLeftHorizontalRadius() * 2.0, clip.getArcWidth());
        assertEquals(menuRadii.getTopLeftVerticalRadius() * 2.0, clip.getArcHeight());

        first.setInstrument("IGNORED", "Ignored", List.of(), Optional.empty());
        assertEquals("AAPL", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
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
    return new Chart(List.of(), symbol, ChartInterval.DAILY);
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
}
