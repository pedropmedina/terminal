package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import java.util.List;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ChartTest {

  @Test
  void identifierUsesTheConfiguredColorBesideThePassiveStatusMetadata() {
    FxTestSupport.runAndWait(() -> {
      try (Chart chartController = new Chart("ACME", ChartInterval.DAILY, List.of())) {
        StackPane chart = chartController.getView();
        new AppThemeManager(new Scene(chart, 800.0, 500.0), AppTheme.LIGHT);
        Color color = Color.hsb(210.0, 0.72, 0.85);

        chartController.setIdentifierColor(color);
        chartController.setIdentifierVisible(true);
        chart.applyCss();
        chart.layout();

        Region identifier = assertInstanceOf(Region.class, chart.lookup(".chart-identifier"));
        HBox statusContent = assertInstanceOf(HBox.class, chart.lookup(".chart-status-content"));
        HBox statusLine = assertInstanceOf(HBox.class, chart.lookup(".chart-status-line"));
        var identifierFill = identifier.getBackground().getFills().getFirst();
        assertEquals(color, identifierFill.getFill());
        assertTrue(identifierFill.getRadii().isUniform());
        assertEquals(3.0, identifierFill.getRadii().getTopLeftHorizontalRadius());
        assertEquals(6.0, identifier.getWidth());
        assertEquals(statusContent.getHeight() * 0.8, identifier.getBoundsInParent().getHeight(), 0.01);
        assertSame(statusContent, identifier.getParent());
        assertSame(identifier, statusContent.getChildren().getFirst());
        assertSame(statusLine, statusContent.getChildren().get(1));
        assertTrue(identifier.getBoundsInParent().getMaxX() <= statusLine.getBoundsInParent().getMinX());
        assertEquals(statusContent.getHeight() / 2.0, identifier.getBoundsInParent().getCenterY(), 0.01);
        Bounds identifierBounds = chart.sceneToLocal(identifier.localToScene(identifier.getBoundsInLocal()));
        assertEquals(12.0, identifierBounds.getMinX(), 0.01);
        assertTrue(identifier.isMouseTransparent());
      }
    });
  }

  @Test
  void keepsIdentifierAndMetadataTogetherInsideTheChart() {
    FxTestSupport.runAndWait(() -> {
      PricePoint point = new PricePoint(LocalDate.of(2026, 8, 24), 104.00, 108.25, 103.50, 107.75, 2_500_000);
      try (Chart chartController = new Chart("ACME", ChartInterval.DAILY, List.of(point))) {
        StackPane chart = chartController.getView();
        new AppThemeManager(new Scene(chart, 420.0, 500.0), AppTheme.LIGHT);
        chartController.setIdentifierVisible(true);
        chart.applyCss();
        chart.layout();

        HBox statusContent = assertInstanceOf(HBox.class, chart.lookup(".chart-status-content"));
        Region identifier = assertInstanceOf(Region.class, chart.lookup(".chart-identifier"));
        HBox statusLine = assertInstanceOf(HBox.class, chart.lookup(".chart-status-line"));
        Bounds contentBounds = chart.sceneToLocal(statusContent.localToScene(statusContent.getBoundsInLocal()));

        assertSame(identifier, statusContent.getChildren().getFirst());
        assertSame(statusLine, statusContent.getChildren().get(1));
        assertEquals(identifier.getBoundsInParent().getCenterY(), statusLine.getBoundsInParent().getCenterY(), 0.01);
        assertTrue(contentBounds.getMaxX() <= chart.getWidth() - 12.0 + 0.01);
      }
    });
  }

  @Test
  void mapsPlatformShortcutsToTheChartDialogs() {
    FxTestSupport.runAndWait(() -> {
      for (KeyCode keyCode : List.of(KeyCode.F, KeyCode.SLASH, KeyCode.P)) {
        KeyEvent event = shortcutEvent(keyCode);
        assertTrue(ChartViewBuilder.isInstrumentSearchShortcut(event));
        assertFalse(ChartViewBuilder.isIntervalSelectionShortcut(event));
      }

      KeyEvent intervalEvent = shortcutEvent(KeyCode.I);
      assertFalse(ChartViewBuilder.isInstrumentSearchShortcut(intervalEvent));
      assertTrue(ChartViewBuilder.isIntervalSelectionShortcut(intervalEvent));
      assertFalse(ChartViewBuilder.isInstrumentSearchShortcut(plainKeyEvent(KeyCode.F)));
      assertFalse(ChartViewBuilder.isIntervalSelectionShortcut(plainKeyEvent(KeyCode.I)));
    });
  }

  @Test
  void requestsWorkspaceIntervalSelectionFromItsPlatformShortcut() {
    FxTestSupport.runAndWait(() -> {
      try (Chart chartController = new Chart("ACME", ChartInterval.DAILY, List.of())) {
        StackPane chart = chartController.getView();
        assertTrue(chart.lookupAll(".chart-workspace-interval-selection-dialog").isEmpty());

        chart.fireEvent(shortcutEvent(KeyCode.I));

        assertTrue(chartController.intervalSelectionOpenProperty().get());
        chartController.closeIntervalSelection();
      }
    });
  }

  private static KeyEvent shortcutEvent(KeyCode keyCode) {
    boolean macOs = System.getProperty("os.name", "").startsWith("Mac");
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, !macOs, false, macOs);
  }

  private static KeyEvent plainKeyEvent(KeyCode keyCode) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false);
  }
}
