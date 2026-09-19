package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.drawer.Drawer;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.togglegroup.ToggleGroupItem;
import com.acteque.terminal.ui.tooltip.Tooltip;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
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
  void menuShowsTheCurrentSymbolAndIntervalNotation() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        HBox menu = assertInstanceOf(HBox.class, chart.getChildren().get(1));
        Button symbolButton = assertInstanceOf(Button.class, menu.getChildren().get(0));
        Button intervalButton = assertInstanceOf(Button.class, menu.getChildren().get(1));

        assertEquals("ACME", symbolButton.getText());
        assertEquals("1D", intervalButton.getText());

        chartController.setInstrument("BTC-USD", "Bitcoin", List.of(), Optional.empty());
        assertEquals("BTC-USD", symbolButton.getText());

        intervalButton.fire();
        Dialog dialog = assertInstanceOf(Dialog.class, chart.lookup(".chart-interval-selection-dialog"));
        ToggleGroupItem fourHours = dialog
          .lookupAll(".chart-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .filter(item -> "4H".equals(item.getText()))
          .findFirst()
          .orElseThrow();
        fourHours.fire();

        assertEquals("4H", intervalButton.getText());
        assertEquals("Select interval, currently 4 hours", intervalButton.getAccessibleText());
      }
    });
  }

  @Test
  void chartTypeChangesUpdateTheMenuIcon() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        HBox menu = assertInstanceOf(HBox.class, chart.getChildren().get(1));
        Button button = assertInstanceOf(Button.class, menu.getChildren().get(2));

        chartController.setChartType(ChartType.LINE_WITH_MARKERS);
        assertSame(LucideIcons.CHART_NETWORK, assertInstanceOf(LucideIcon.class, button.getGraphic()).getGlyph());

        chartController.setChartType(ChartType.CANDLESTICK);
        assertSame(LucideIcons.CHART_CANDLESTICK, assertInstanceOf(LucideIcon.class, button.getGraphic()).getGlyph());
      }
    });
  }

  @Test
  void chartTypeMenuOpensDrawerAndAppliesTheSelectedType() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        new AppThemeManager(new Scene(chart, 1060, 760), AppTheme.LIGHT);
        HBox menu = assertInstanceOf(HBox.class, chart.getChildren().get(1));
        Button button = assertInstanceOf(Button.class, menu.getChildren().get(2));
        Drawer drawer = assertInstanceOf(Drawer.class, chart.lookup(".chart-settings-drawer"));

        chartController.setChartType(ChartType.CANDLESTICK);
        button.fire();
        chart.applyCss();
        chart.layout();
        assertTrue(drawer.isOpen());
        assertEquals(menu.getBoundsInParent().getMaxY(), StackPane.getMargin(drawer).getTop());
        Bounds menuBounds = menu.localToScene(menu.getBoundsInLocal());
        Bounds drawerBounds = drawer.getContent().localToScene(drawer.getContent().getLayoutBounds());
        assertTrue(drawerBounds.getMinY() >= menuBounds.getMaxY());
        assertTrue(drawer.lookup(".chart-settings-option").isFocusTraversable());

        chart
          .lookupAll(".chart-settings-option")
          .stream()
          .map(com.acteque.terminal.ui.togglegroup.ToggleGroupItem.class::cast)
          .filter(item -> item.getAccessibleText().startsWith("Area."))
          .findFirst()
          .orElseThrow()
          .fire();

        assertFalse(drawer.isOpen());
        assertSame(LucideIcons.CHART_AREA, assertInstanceOf(LucideIcon.class, button.getGraphic()).getGlyph());
        assertEquals("Chart type: Area", button.getAccessibleText());
      }
    });
  }

  @Test
  void settingsDrawerDismissesWithoutBlockingTheChartOrOtherDialogs() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        new Scene(chart, 1060, 760);
        HBox menu = assertInstanceOf(HBox.class, chart.getChildren().get(1));
        Button chartTypeButton = assertInstanceOf(Button.class, menu.getChildren().get(2));
        Button intervalButton = assertInstanceOf(Button.class, menu.getChildren().get(1));
        Drawer drawer = assertInstanceOf(Drawer.class, chart.lookup(".chart-settings-drawer"));

        chartTypeButton.fire();
        chart.fireEvent(plainKeyEvent(KeyCode.ESCAPE));
        assertFalse(drawer.isOpen());

        chartTypeButton.fire();
        AtomicInteger chartPresses = new AtomicInteger();
        chart.addEventHandler(MouseEvent.MOUSE_PRESSED, ignored -> chartPresses.incrementAndGet());
        chart.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, chart.getBoundsInLocal(), true));
        assertFalse(drawer.isOpen());
        assertEquals(1, chartPresses.get());

        chartTypeButton.fire();
        intervalButton.fire();
        assertFalse(drawer.isOpen());
        assertTrue(((Dialog) chart.lookup(".chart-interval-selection-dialog")).isOpen());
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
  void opensTheIntervalDialogFromItsPlatformShortcut() {
    FxTestSupport.runAndWait(() -> {
      try (
        Chart chartController = new Chart(List.of(), "ACME", ChartInterval.DAILY, new StubInstrumentCatalog(List::of))
      ) {
        StackPane chart = chartController.getView();
        Dialog dialog = (Dialog) chart.lookup(".chart-interval-selection-dialog");

        chart.fireEvent(shortcutEvent(KeyCode.I));

        assertTrue(dialog.isOpen());
        dialog.close();
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
      }
    });
  }

  @Test
  void closingAnIntervalModalDoesNotReshowTheTriggerTooltipWhenFocusReturns() throws InterruptedException {
    AtomicReference<Button> buttonReference = new AtomicReference<>();
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Dialog> dialogReference = new AtomicReference<>();
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
      Dialog dialog = (Dialog) chart.lookup(".chart-interval-selection-dialog");
      buttonReference.set(intervalButton);
      tooltipReference.set(tooltip);
      dialogReference.set(dialog);
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
