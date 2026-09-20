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
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
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

        assertEquals("IBM", assertInstanceOf(Button.class, buttons.getChildren().getFirst()).getText());
        assertEquals(4, buttons.getChildren().size());
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
        assertEquals(5, buttons.getChildren().size());
        assertTrue(identifier.isVisible());
        assertEquals(4.0, identifier.getWidth());
        assertEquals(color, identifier.getBackground().getFills().getFirst().getFill());

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
        PopoverTrigger split = assertInstanceOf(PopoverTrigger.class, buttons.getChildren().get(3));
        split
          .getPopover()
          .getContent()
          .lookupAll(".chart-workspace-split-action")
          .stream()
          .map(Button.class::cast)
          .sorted((first, second) -> first.getText().compareTo(second.getText()))
          .forEach(Button::fire);
        assertInstanceOf(Button.class, buttons.getChildren().get(4)).fire();

        assertEquals(1, instrumentRequests.get());
        assertEquals(1, intervalRequests.get());
        assertEquals(1, chartTypeRequests.get());
        assertEquals(1, closes.get());
        assertEquals(
          List.of(
            ChartSplitDirection.BOTTOM,
            ChartSplitDirection.LEFT,
            ChartSplitDirection.RIGHT,
            ChartSplitDirection.TOP
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
}
