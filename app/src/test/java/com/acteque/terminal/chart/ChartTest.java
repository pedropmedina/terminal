package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.marketdata.provider.tiingo.TiingoMarketDataClient;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.tooltip.Tooltip;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class ChartTest {

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
  void opensTheIntervalDialogFromItsPlatformShortcut() {
    FxTestSupport.runAndWait(() -> {
      TiingoMarketDataClient client = new TiingoMarketDataClient("test-token");
      Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, client.tickerCatalog);
      StackPane chart = chartController.getView();
      Dialog dialog = (Dialog) chart.lookup(".chart-interval-selection-dialog");

      chart.fireEvent(shortcutEvent(KeyCode.I));

      assertTrue(dialog.isOpen());
      dialog.close();
    });
  }

  @Test
  void dismissesTheStatusTooltipWhenAShortcutOpensAModal() {
    FxTestSupport.runAndWait(() -> {
      TiingoMarketDataClient client = new TiingoMarketDataClient("test-token");
      Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, client.tickerCatalog);
      StackPane chart = chartController.getView();
      Dialog dialog = (Dialog) chart.lookup(".chart-interval-selection-dialog");
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

        assertTrue(dialog.isOpen());
        assertFalse(tooltip.isShowing());
      } finally {
        dialog.close();
        stage.close();
      }
    });
  }

  @Test
  void closingAnIntervalModalDoesNotReshowTheTriggerTooltipWhenFocusReturns() throws InterruptedException {
    AtomicReference<Button> buttonReference = new AtomicReference<>();
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Dialog> dialogReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();

    FxTestSupport.runAndWait(() -> {
      TiingoMarketDataClient client = new TiingoMarketDataClient("test-token");
      Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, client.tickerCatalog);
      StackPane chart = chartController.getView();
      Stage stage = new Stage();
      stage.setX(200.0);
      stage.setY(200.0);
      stage.setScene(new Scene(chart, 800.0, 500.0));
      new ThemeManager(stage.getScene(), AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();
      chart.applyCss();
      chart.layout();

      HBox statusLine = (HBox) chart.lookup(".chart-status-line");
      Tooltip tooltip = (Tooltip) statusLine.getChildren().get(1);
      Button intervalButton = (Button) tooltip.getTrigger().getTarget();
      Dialog dialog = (Dialog) chart.lookup(".chart-interval-selection-dialog");
      buttonReference.set(intervalButton);
      tooltipReference.set(tooltip);
      dialogReference.set(dialog);
      stageReference.set(stage);
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

        assertTrue(dialogReference.get().isOpen());
        assertTrue(intervalButton.isDisabled());
        assertFalse(tooltip.isShowing());
      });

      FxTestSupport.runAndWait(() -> {
        assertTrue(buttonReference.get().isDisabled());
        assertFalse(tooltipReference.get().isShowing());
        dialogReference.get().close();
      });

      FxTestSupport.runAndWait(() -> {
        assertFalse(buttonReference.get().isDisabled());
        assertTrue(buttonReference.get().isFocused());
        assertFalse(buttonReference.get().isFocusVisible());
        assertFalse(tooltipReference.get().isShowing());
      });

      Thread.sleep(500L);
      FxTestSupport.runAndWait(() -> assertFalse(tooltipReference.get().isShowing()));
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
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
