package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.tooltip.Tooltip;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class ChartTest {

  @Test
  void identifierUsesTheConfiguredColorAsAPassiveStatusLineBorder() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        new AppThemeManager(new Scene(chart, 800.0, 500.0), AppTheme.LIGHT);
        Color color = Color.hsb(210.0, 0.72, 0.85);

        chartController.setIdentifierColor(color);
        chartController.setIdentifierVisible(true);
        chart.applyCss();
        chart.layout();

        Region identifier = assertInstanceOf(Region.class, chart.lookup(".chart-identifier"));
        HBox statusContent = assertInstanceOf(HBox.class, chart.lookup(".chart-status-content"));
        Region statusLine = assertInstanceOf(Region.class, chart.lookup(".chart-status-line"));
        var identifierFill = identifier.getBackground().getFills().getFirst();
        assertEquals(color, identifierFill.getFill());
        assertTrue(identifierFill.getRadii().isUniform());
        assertEquals(3.0, identifierFill.getRadii().getTopLeftHorizontalRadius());
        assertEquals(6.0, identifier.getWidth());
        assertEquals(statusLine.getHeight() * 0.8, identifier.getBoundsInParent().getHeight(), 0.01);
        assertEquals(Pos.CENTER_LEFT, statusContent.getAlignment());
        assertEquals(6.0, statusContent.getSpacing());
        assertTrue(identifier.getBoundsInParent().getMaxX() <= statusLine.getBoundsInParent().getMinX());
        assertEquals(statusLine.getBoundsInParent().getCenterY(), identifier.getBoundsInParent().getCenterY(), 0.01);
        Bounds identifierBounds = chart.sceneToLocal(identifier.localToScene(identifier.getBoundsInLocal()));
        assertEquals(12.0, identifierBounds.getMinX(), 0.01);
        assertTrue(identifier.isMouseTransparent());
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
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        assertTrue(chart.lookupAll(".chart-workspace-interval-selection-dialog").isEmpty());

        chart.fireEvent(shortcutEvent(KeyCode.I));

        assertTrue(chartController.intervalSelectionOpenProperty().get());
        chartController.closeIntervalSelection();
      }
    });
  }

  @Test
  void dismissesTheStatusTooltipWhenAShortcutOpensAModal() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        Stage stage = new Stage();
        stage.setScene(new Scene(chart, 800.0, 500.0));
        Platform.setImplicitExit(false);
        stage.show();
        chart.applyCss();
        chart.layout();

        try {
          HBox statusLine = (HBox) chart.lookup(".chart-status-line");
          Tooltip tooltip = (Tooltip) statusLine.getChildren().get(1);
          tooltip.show();
          assertTrue(tooltip.isShowing());

          chart.fireEvent(shortcutEvent(KeyCode.I));

          assertTrue(chartController.intervalSelectionOpenProperty().get());
          assertFalse(tooltip.isShowing());
        } finally {
          chartController.closeIntervalSelection();
          stage.close();
        }
      }
    });
  }

  @Test
  void closingAnIntervalModalDoesNotReshowTheTriggerTooltipWhenFocusReturns() throws InterruptedException {
    AtomicReference<Button> buttonReference = new AtomicReference<>();
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    AtomicReference<Chart> chartControllerReference = new AtomicReference<>();

    FxTestSupport.runAndWait(() -> {
      Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of));
      StackPane chart = chartController.getView();
      Stage stage = new Stage();
      stage.setX(200.0);
      stage.setY(200.0);
      stage.setScene(new Scene(chart, 800.0, 500.0));
      new AppThemeManager(stage.getScene(), AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();
      chart.applyCss();
      chart.layout();

      HBox statusLine = (HBox) chart.lookup(".chart-status-line");
      Tooltip tooltip = (Tooltip) statusLine.getChildren().get(1);
      Button intervalButton = (Button) tooltip.getTrigger().getTarget();
      buttonReference.set(intervalButton);
      tooltipReference.set(tooltip);
      stageReference.set(stage);
      chartControllerReference.set(chartController);
    });

    try {
      FxTestSupport.runAndWait(() -> stageReference.get().requestFocus());

      FxTestSupport.runAndWait(() -> {
        Button intervalButton = buttonReference.get();
        Tooltip tooltip = tooltipReference.get();
        Bounds bounds = intervalButton.localToScreen(intervalButton.getBoundsInLocal());
        tooltip.show();
        assertTrue(tooltip.isShowing());

        click(intervalButton, bounds);

        assertTrue(chartControllerReference.get().intervalSelectionOpenProperty().get());
        assertTrue(intervalButton.isDisabled());
        assertFalse(tooltip.isShowing());
      });

      FxTestSupport.runAndWait(() -> {
        assertTrue(buttonReference.get().isDisabled());
        assertFalse(tooltipReference.get().isShowing());
        chartControllerReference.get().closeIntervalSelection();
      });

      FxTestSupport.runAndWait(() -> {
        assertFalse(buttonReference.get().isDisabled());
        assertFalse(buttonReference.get().isFocusVisible());
        assertFalse(tooltipReference.get().isShowing());
      });

      Thread.sleep(500L);
      FxTestSupport.runAndWait(() -> assertFalse(tooltipReference.get().isShowing()));
    } finally {
      FxTestSupport.runAndWait(() -> {
        stageReference.get().close();
        chartControllerReference.get().close();
      });
    }
  }

  private static void click(Button button, Bounds screenBounds) {
    button.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, screenBounds, true));
    button.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, screenBounds, false));
  }

  private static KeyEvent shortcutEvent(KeyCode keyCode) {
    boolean macOs = System.getProperty("os.name", "").startsWith("Mac");
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, !macOs, false, macOs);
  }

  private static KeyEvent plainKeyEvent(KeyCode keyCode) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false);
  }

  private static MouseEvent mouseEvent(
    javafx.event.EventType<MouseEvent> eventType,
    Bounds screenBounds,
    boolean primaryButtonDown
  ) {
    return new MouseEvent(
      eventType,
      screenBounds.getWidth() / 2.0,
      screenBounds.getHeight() / 2.0,
      (screenBounds.getMinX() + screenBounds.getMaxX()) / 2.0,
      (screenBounds.getMinY() + screenBounds.getMaxY()) / 2.0,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      primaryButtonDown,
      false,
      false,
      false,
      false,
      true,
      null
    );
  }
}
