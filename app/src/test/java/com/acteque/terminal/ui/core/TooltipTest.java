package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

class TooltipTest {

  @Test
  void composesTooltipContentInternally() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip("Add to library");
      Button button = new Button("Add");
      tooltip.install(button);

      assertSame(tooltip.getGraphic(), tooltip.getPopup().getParent());
      assertEquals("Add to library", ((Label) tooltip.getContentNodes().getFirst()).getText());
      assertTrue(button.getProperties().containsValue(tooltip));
      assertTrue(tooltip.getGraphic().getStyleClass().contains("core-tooltip-content"));
      assertTrue(tooltip.getPopup().getStyleClass().contains("core-tooltip-popup"));
      assertTrue(tooltip.getArrow().getStyleClass().contains("core-tooltip-arrow"));
      assertFalse(tooltip.isShowing());
      tooltip.uninstall(button);
    });
  }

  @Test
  void defaultsMatchTheProvidedPositionerProps() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip();

      assertEquals(Duration.millis(350.0), tooltip.getShowDelay());
      assertEquals(Duration.INDEFINITE, tooltip.getShowDuration());
      assertEquals(Tooltip.Align.CENTER, tooltip.getAlign());
      assertEquals(Tooltip.Side.TOP, tooltip.getSide());
      assertEquals(0.0, tooltip.getAlignOffset());
      assertEquals(4.0, tooltip.getSideOffset());
    });
  }

  @Test
  void delegatesNativeTooltipTimingAndInstallation() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip("Add");
      tooltip.setShowDelay(Duration.millis(250.0));
      Button button = new Button("Add");
      tooltip.install(button);

      assertEquals(Duration.millis(250.0), tooltip.getShowDelay());
      assertTrue(button.getProperties().containsValue(tooltip));

      tooltip.setShowDelay(Duration.millis(500.0));
      assertEquals(Duration.millis(500.0), tooltip.getShowDelay());

      tooltip.uninstall(button);
      assertFalse(button.getProperties().containsValue(tooltip));
    });
  }

  @Test
  void exposesBindablePositionerProperties() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip();

      tooltip.setAlign(Tooltip.Align.END);
      tooltip.setSide(Tooltip.Side.INLINE_END);
      tooltip.setAlignOffset(3.0);
      tooltip.setSideOffset(8.0);

      assertEquals(Tooltip.Align.END, tooltip.getAlign());
      assertEquals(Tooltip.Side.INLINE_END, tooltip.getSide());
      assertEquals(3.0, tooltip.getAlignOffset());
      assertEquals(8.0, tooltip.getSideOffset());
    });
  }

  @Test
  void rejectsNullRequiredPositionerValues() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip();

      assertThrows(NullPointerException.class, () -> new Tooltip((String) null));
      assertThrows(NullPointerException.class, () -> tooltip.setAlign(null));
      assertThrows(NullPointerException.class, () -> tooltip.setSide(null));
    });
  }

  @Test
  void mapsThePermittedShadcnSizingSpacingAndColorsToJavaFxCss() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = new Tooltip(new Label("Add to library"));
      StackPane root = new StackPane(tooltip.getGraphic());
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      root.applyCss();

      assertEquals(320.0, tooltip.getPopup().getMaxWidth());
      assertEquals(new Insets(6.0, 12.0, 6.0, 12.0), tooltip.getPopup().getPadding());
      assertEquals(6.0, tooltip.getPopup().getSpacing());
      assertEquals(12.0, ((Label) tooltip.getContentNodes().getFirst()).getFont().getSize());
      assertEquals(Color.web("#0a0a0a"), tooltip.getPopup().getBackground().getFills().getFirst().getFill());
      assertEquals(
        6.0,
        tooltip.getPopup().getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius()
      );
      assertEquals(Color.web("#ffffff"), ((Label) tooltip.getContentNodes().getFirst()).getTextFill());
      assertEquals(10.0, tooltip.getArrow().getPrefWidth());
      assertEquals(10.0, tooltip.getArrow().getPrefHeight());
      assertEquals(45.0, tooltip.getArrow().getRotate());
      assertEquals(Color.web("#0a0a0a"), tooltip.getArrow().getBackground().getFills().getFirst().getFill());
      assertEquals(0.0, tooltip.getArrow().getOpacity());

      tooltip.setOpenState(true);
      root.applyCss();

      assertEquals(1.0, tooltip.getArrow().getOpacity());
    });
  }

  @Test
  void mapsForegroundAndBackgroundTokensInDarkMode() {
    FxTestSupport.runAndWait(() -> {
      Label label = new Label("Add to library");
      Tooltip tooltip = new Tooltip(label);
      StackPane root = new StackPane(tooltip.getGraphic());
      new ThemeManager(new Scene(root), AppTheme.DARK);

      root.applyCss();

      assertEquals(Color.web("#fafafa"), tooltip.getPopup().getBackground().getFills().getFirst().getFill());
      assertEquals(Color.web("#0a0a0a"), label.getTextFill());
    });
  }

  @Test
  void centersTheArrowOnTheContentForEverySideAndAlignment() {
    FxTestSupport.runAndWait(() -> {
      Button anchor = new Button("Anchor");
      StackPane root = new StackPane(anchor);
      Stage stage = new Stage();
      stage.setX(200.0);
      stage.setY(200.0);
      stage.setScene(new Scene(root, 400.0, 300.0));
      new ThemeManager(stage.getScene(), AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();

      try {
        root.applyCss();
        root.layout();
        Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
        Tooltip tooltip = new Tooltip("Tooltip content");
        tooltip.recordAnchor(anchor);
        tooltip.show(anchor, anchorBounds.getMinX(), anchorBounds.getMinY());

        for (Tooltip.Side side : new Tooltip.Side[] {
          Tooltip.Side.TOP,
          Tooltip.Side.BOTTOM,
          Tooltip.Side.LEFT,
          Tooltip.Side.RIGHT,
        }) {
          for (Tooltip.Align align : Tooltip.Align.values()) {
            tooltip.setSide(side);
            tooltip.setAlign(align);
            tooltip.reposition();
            tooltip.getGraphic().applyCss();
            tooltip.getPopup().getParent().layout();

            assertArrowGeometry(tooltip, side, side + " " + align);
          }
        }
        tooltip.hide();
      } finally {
        stage.close();
      }
    });
  }

  private static void assertArrowGeometry(Tooltip tooltip, Tooltip.Side side, String message) {
    Bounds contentBounds = tooltip.getGraphic().getLayoutBounds();
    Bounds arrowLayoutBounds = tooltip.getArrow().getBoundsInParent();
    assertTrue(arrowLayoutBounds.getMinX() >= contentBounds.getMinX() - 0.01, message);
    assertTrue(arrowLayoutBounds.getMinY() >= contentBounds.getMinY() - 0.01, message);
    assertTrue(arrowLayoutBounds.getMaxX() <= contentBounds.getMaxX() + 0.01, message);
    assertTrue(arrowLayoutBounds.getMaxY() <= contentBounds.getMaxY() + 0.01, message);

    double arrowCenterX = tooltip.getArrow().getLayoutX() + tooltip.getArrow().getWidth() / 2.0;
    double arrowCenterY = tooltip.getArrow().getLayoutY() + tooltip.getArrow().getHeight() / 2.0;
    if (side == Tooltip.Side.TOP || side == Tooltip.Side.BOTTOM) {
      assertEquals(tooltip.getPopup().getLayoutX() + tooltip.getPopup().getWidth() / 2.0, arrowCenterX, 0.01, message);
    } else {
      assertEquals(tooltip.getPopup().getLayoutY() + tooltip.getPopup().getHeight() / 2.0, arrowCenterY, 0.01, message);
    }

    double overlap = switch (side) {
      case TOP -> tooltip.getPopup().getLayoutY() + tooltip.getPopup().getHeight() - arrowCenterY;
      case BOTTOM -> arrowCenterY - tooltip.getPopup().getLayoutY();
      case LEFT -> tooltip.getPopup().getLayoutX() + tooltip.getPopup().getWidth() - arrowCenterX;
      case RIGHT -> arrowCenterX - tooltip.getPopup().getLayoutX();
      case INLINE_START, INLINE_END -> throw new AssertionError("Expected a physical side");
    };
    assertEquals(1.0, overlap, 0.01, message);
  }
}
