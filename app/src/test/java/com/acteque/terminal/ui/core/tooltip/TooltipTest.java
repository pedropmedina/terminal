package com.acteque.terminal.ui.core.tooltip;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.TraversalDirection;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.junit.jupiter.api.Test;

class TooltipTest {

  @Test
  void composesTooltipContentInternally() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("Add");
      TooltipTrigger trigger = new TooltipTrigger(button);
      TooltipContent content = new TooltipContent("Add to library");
      Tooltip tooltip = new Tooltip(trigger, content);

      assertSame(tooltip.getGraphic(), tooltip.getPopup().getParent());
      assertEquals("Add to library", ((Label) tooltip.getContentNodes().getFirst()).getText());
      assertSame(trigger, tooltip.getChildren().getFirst());
      assertSame(button, trigger.getTarget());
      assertSame(content, tooltip.getContent());
      assertFalse(button.getProperties().containsValue(tooltip));
      assertTrue(tooltip.getGraphic().getStyleClass().contains("core-tooltip-content"));
      assertTrue(tooltip.getPopup().getStyleClass().contains("core-tooltip-popup"));
      assertTrue(tooltip.getArrow().getStyleClass().contains("core-tooltip-arrow"));
      assertFalse(tooltip.isShowing());
    });
  }

  @Test
  void defaultsMatchTheProvidedPositionerProps() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = tooltip(new Button(), new TooltipContent());

      assertEquals(Duration.millis(200.0), tooltip.getShowDelay());
      assertEquals(Duration.INDEFINITE, tooltip.getShowDuration());
      assertEquals(Tooltip.Align.CENTER, tooltip.getAlign());
      assertEquals(Tooltip.Side.TOP, tooltip.getSide());
      assertEquals(0.0, tooltip.getAlignOffset());
      assertEquals(4.0, tooltip.getSideOffset());
    });
  }

  @Test
  void exposesTooltipTimingWithoutMutatingTheTarget() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("Add");
      Tooltip tooltip = tooltip(button, new TooltipContent("Add"));
      tooltip.setShowDelay(Duration.millis(250.0));

      assertEquals(Duration.millis(250.0), tooltip.getShowDelay());
      assertFalse(button.getProperties().containsValue(tooltip));

      tooltip.setShowDelay(Duration.millis(500.0));
      assertEquals(Duration.millis(500.0), tooltip.getShowDelay());
    });
  }

  @Test
  void wrapperHoverControlsVisibilityWithoutSuppressingOrReactingToChildHover() throws InterruptedException {
    AtomicInteger childHoverEvents = new AtomicInteger();
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("Anchor");
      button.addEventHandler(MouseEvent.MOUSE_ENTERED_TARGET, event -> childHoverEvents.incrementAndGet());
      button.addEventHandler(MouseEvent.MOUSE_MOVED, event -> childHoverEvents.incrementAndGet());
      button.addEventHandler(MouseEvent.MOUSE_EXITED_TARGET, event -> childHoverEvents.incrementAndGet());
      Tooltip tooltip = tooltip(button, new TooltipContent("Tooltip content"));
      tooltip.setShowDelay(Duration.millis(50.0));
      Stage stage = new Stage();
      stage.setScene(new Scene(new StackPane(tooltip), 400.0, 300.0));
      new ThemeManager(stage.getScene(), AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();
      stage.getScene().getRoot().applyCss();
      stage.getScene().getRoot().layout();
      Bounds bounds = button.localToScreen(button.getBoundsInLocal());
      MouseEvent entered = mouseEvent(MouseEvent.MOUSE_ENTERED_TARGET, bounds.getCenterX(), bounds.getCenterY());
      MouseEvent moved = mouseEvent(MouseEvent.MOUSE_MOVED, bounds.getCenterX(), bounds.getCenterY());
      button.fireEvent(entered);
      button.fireEvent(moved);
      button.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED_TARGET, bounds.getCenterX(), bounds.getCenterY()));
      assertEquals(3, childHoverEvents.get());
      assertFalse(tooltip.isShowing());
      tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_ENTERED, bounds.getCenterX(), bounds.getCenterY()));
      tooltipReference.set(tooltip);
      stageReference.set(stage);
    });

    try {
      Thread.sleep(150L);
      FxTestSupport.runAndWait(() -> {
        Tooltip tooltip = tooltipReference.get();
        assertTrue(tooltip.isShowing());
        Label contentLabel = assertInstanceOf(Label.class, tooltip.getContentNodes().getFirst());
        assertTrue(tooltip.getContent().getWidth() > 0.0);
        assertTrue(tooltip.getContent().getHeight() > 0.0);
        assertNotNull(contentLabel.localToScreen(contentLabel.getBoundsInLocal()));
        assertTrue(contentLabel.isVisible());
        assertEquals(1.0, contentLabel.getOpacity());
        Button button = (Button) tooltip.getTrigger().getTarget();
        Bounds bounds = button.localToScreen(button.getBoundsInLocal());
        button.fireEvent(mouseEvent(MouseEvent.MOUSE_ENTERED_TARGET, bounds.getCenterX(), bounds.getCenterY()));
        button.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED_TARGET, bounds.getCenterX(), bounds.getCenterY()));
        assertTrue(tooltip.isShowing());
        tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED, bounds.getMaxX() + 10.0, bounds.getCenterY()));
        assertFalse(tooltip.isShowing());
        assertEquals(5, childHoverEvents.get());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void rapidExitAndReentryCannotShowAStaleOrBlankPopup() throws InterruptedException {
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("Anchor");
      Tooltip tooltip = tooltip(button, new TooltipContent("Tooltip content"));
      tooltip.setShowDelay(Duration.millis(40.0));
      Stage stage = new Stage();
      stage.setScene(new Scene(new StackPane(tooltip), 400.0, 300.0));
      new ThemeManager(stage.getScene(), AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();
      stage.getScene().getRoot().applyCss();
      stage.getScene().getRoot().layout();
      Bounds bounds = button.localToScreen(button.getBoundsInLocal());

      tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_ENTERED, bounds.getCenterX(), bounds.getCenterY()));
      tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED, bounds.getMaxX() + 1.0, bounds.getCenterY()));
      tooltipReference.set(tooltip);
      stageReference.set(stage);
    });

    try {
      Thread.sleep(100L);
      FxTestSupport.runAndWait(() -> {
        Tooltip tooltip = tooltipReference.get();
        assertFalse(tooltip.isShowing(), "an expired delay must not show after the wrapper was exited");
        Bounds bounds = tooltip.getTrigger().localToScreen(tooltip.getTrigger().getBoundsInLocal());
        tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_ENTERED, bounds.getCenterX(), bounds.getCenterY()));
      });

      Thread.sleep(100L);
      FxTestSupport.runAndWait(() -> {
        Tooltip tooltip = tooltipReference.get();
        assertTrue(tooltip.isShowing());

        Bounds bounds = tooltip.getTrigger().localToScreen(tooltip.getTrigger().getBoundsInLocal());
        tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED, bounds.getMaxX() + 1.0, bounds.getCenterY()));
        tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_ENTERED, bounds.getCenterX(), bounds.getCenterY()));
      });

      Thread.sleep(100L);
      FxTestSupport.runAndWait(() -> {
        Tooltip tooltip = tooltipReference.get();
        Label label = assertInstanceOf(Label.class, tooltip.getContentNodes().getFirst());
        assertTrue(tooltip.isShowing());
        assertTrue(tooltip.getContent().getWidth() > 0.0);
        assertTrue(tooltip.getContent().getHeight() > 0.0);
        assertNotNull(tooltip.getContent().getBackground());
        assertFalse(tooltip.getContent().getBackground().getFills().isEmpty());
        assertNotNull(label.localToScreen(label.getBoundsInLocal()));
        assertEquals(1.0, label.getOpacity());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void dismissCancelsAPendingKeyboardFocusDisplay() throws InterruptedException {
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button anchor = new Button("Anchor");
      Stage stage = new Stage();
      stage.setScene(new Scene(new StackPane(anchor), 400.0, 300.0));
      Platform.setImplicitExit(false);
      stage.show();

      Tooltip tooltip = tooltip(anchor, new TooltipContent("Tooltip content"));
      tooltip.setShowDelay(Duration.millis(50.0));
      anchor.requestFocus();
      tooltip.showFromFocus();
      tooltip.dismiss();
      tooltipReference.set(tooltip);
      stageReference.set(stage);
    });

    try {
      Thread.sleep(150L);
      FxTestSupport.runAndWait(() -> assertFalse(tooltipReference.get().isShowing()));
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void keyboardFocusedTargetStillShowsTheTooltip() throws InterruptedException {
    AtomicReference<Button> beforeReference = new AtomicReference<>();
    AtomicReference<Button> anchorReference = new AtomicReference<>();
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button before = new Button("Before");
      Button anchor = new Button("Anchor");
      Stage stage = new Stage();
      Tooltip tooltip = tooltip(anchor, new TooltipContent("Tooltip content"));
      stage.setScene(new Scene(new VBox(before, tooltip), 400.0, 300.0));
      Platform.setImplicitExit(false);
      stage.show();

      tooltip.setShowDelay(Duration.millis(50.0));
      before.requestFocus();
      beforeReference.set(before);
      anchorReference.set(anchor);
      tooltipReference.set(tooltip);
      stageReference.set(stage);
    });

    FxTestSupport.runAndWait(() -> {
      assertTrue(beforeReference.get().requestFocusTraversal(TraversalDirection.NEXT));
      assertSame(anchorReference.get(), stageReference.get().getScene().getFocusOwner());
      tooltipReference.get().showFromFocus();
    });

    try {
      Thread.sleep(150L);
      FxTestSupport.runAndWait(() -> assertTrue(tooltipReference.get().isShowing()));
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void pointerExitDismissesATooltipShownAfterFocusReturns() throws InterruptedException {
    AtomicReference<Tooltip> tooltipReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button anchor = new Button("Anchor");
      Stage stage = new Stage();
      stage.setX(200.0);
      stage.setY(200.0);
      Tooltip tooltip = tooltip(anchor, new TooltipContent("Tooltip content"));
      stage.setScene(new Scene(new StackPane(tooltip), 400.0, 300.0));
      Platform.setImplicitExit(false);
      stage.show();

      tooltip.setShowDelay(Duration.millis(50.0));
      stage.requestFocus();
      anchor.requestFocus();
      tooltip.showFromFocus();
      tooltipReference.set(tooltip);
      stageReference.set(stage);
    });

    try {
      Thread.sleep(150L);
      FxTestSupport.runAndWait(() -> {
        Tooltip tooltip = tooltipReference.get();
        assertTrue(tooltip.isShowing());
        tooltip.fireEvent(mouseEvent(MouseEvent.MOUSE_EXITED, 0.0, 0.0));
        assertFalse(tooltip.isShowing());
        assertSame(tooltip.getTrigger(), tooltip.getChildren().getFirst());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void disablingAnAnchorDismissesItsVisibleTooltip() {
    FxTestSupport.runAndWait(() -> {
      Button anchor = new Button("Anchor");
      Tooltip tooltip = tooltip(anchor, new TooltipContent("Tooltip content"));
      StackPane root = new StackPane(tooltip);
      Stage stage = new Stage();
      stage.setX(200.0);
      stage.setY(200.0);
      stage.setScene(new Scene(root, 400.0, 300.0));
      Platform.setImplicitExit(false);
      stage.show();

      try {
        tooltip.show();
        assertTrue(tooltip.isShowing());

        root.setDisable(true);

        assertTrue(anchor.isDisabled());
        assertFalse(tooltip.isShowing());
      } finally {
        stage.close();
      }
    });
  }

  @Test
  void exposesBindablePositionerProperties() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = tooltip(new Button(), new TooltipContent());

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
      Tooltip tooltip = tooltip(new Button(), new TooltipContent());

      assertThrows(NullPointerException.class, () -> new Tooltip(null, new TooltipContent()));
      assertThrows(NullPointerException.class, () -> new Tooltip(new TooltipTrigger(new Button()), null));
      assertThrows(NullPointerException.class, () -> new TooltipContent((String) null));
      assertThrows(NullPointerException.class, () -> tooltip.setAlign(null));
      assertThrows(NullPointerException.class, () -> tooltip.setSide(null));
    });
  }

  @Test
  void mapsThePermittedShadcnSizingSpacingAndColorsToJavaFxCss() {
    FxTestSupport.runAndWait(() -> {
      Tooltip tooltip = tooltip(new Button(), new TooltipContent(new Label("Add to library")));
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
      Tooltip tooltip = tooltip(new Button(), new TooltipContent(label));
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
      Tooltip tooltip = tooltip(anchor, new TooltipContent("Tooltip content"));
      StackPane root = new StackPane(tooltip);
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
        tooltip.show();

        assertTrue(
          tooltip.getGraphic().localToScreen(tooltip.getGraphic().getBoundsInLocal()).getMaxY() <=
            anchorBounds.getMinY() - tooltip.getSideOffset() + 0.01,
          "The popup must be positioned before show() returns"
        );

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

  private static Tooltip tooltip(Node target, TooltipContent content) {
    return new Tooltip(new TooltipTrigger(target), content);
  }

  private static MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type, double screenX, double screenY) {
    return new MouseEvent(
      type,
      0.0,
      0.0,
      screenX,
      screenY,
      MouseButton.NONE,
      0,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      null
    );
  }
}
