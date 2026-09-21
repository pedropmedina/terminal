package com.acteque.terminal.ui.popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.css.TransitionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.PickResult;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class PopoverTest {

  private static final PseudoClass OPENING = PseudoClass.getPseudoClass("opening");
  private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
  private static final PseudoClass CLOSING = PseudoClass.getPseudoClass("closing");
  private static final PseudoClass CLOSED = PseudoClass.getPseudoClass("closed");

  @Test
  void composesThePopoverComponentFamily() {
    FxTestSupport.runAndWait(() -> {
      PopoverTitle title = new PopoverTitle("Dimensions");
      PopoverDescription description = new PopoverDescription("Set the dimensions for the layer.");
      PopoverHeader header = new PopoverHeader(title, description);
      PopoverContent content = new PopoverContent(header);
      Popover popover = new Popover(content);
      PopoverTrigger trigger = new PopoverTrigger("Open", popover);

      assertSame(content, popover.getContent());
      assertSame(content, popover.getPopup().getContent().getFirst());
      assertSame(popover, trigger.getPopover());
      assertTrue(content.getStyleClass().contains("core-popover-content"));
      assertTrue(header.getStyleClass().contains("core-popover-header"));
      assertTrue(title.getStyleClass().contains("core-popover-title"));
      assertTrue(description.getStyleClass().contains("core-popover-description"));
      assertPhase(content, CLOSED);
    });
  }

  @Test
  void defaultsMatchTheProvidedPositionerProps() {
    FxTestSupport.runAndWait(() -> {
      PopoverContent content = new PopoverContent();

      assertEquals(PopoverContent.Align.CENTER, content.getAlign());
      assertEquals(PopoverContent.Side.BOTTOM, content.getSide());
      assertEquals(0.0, content.getAlignOffset());
      assertEquals(4.0, content.getSideOffset());
    });
  }

  @Test
  void triggerTogglesLogicalOpenStateBeforeItsWindowIsShown() {
    FxTestSupport.runAndWait(() -> {
      Popover popover = new Popover(new PopoverContent());
      PopoverTrigger trigger = new PopoverTrigger("Open", popover);
      new Scene(new StackPane(trigger));

      trigger.fire();
      assertTrue(popover.isOpen());

      trigger.fire();
      assertFalse(popover.isOpen());
    });
  }

  @Test
  void openingPaintsShadcnInitialValuesThenTransitionsEveryAnimatedProperty() throws Exception {
    Fixture fixture = createFixture();
    Set<String> transitionedProperties = new HashSet<>();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture
          .content()
          .addEventHandler(TransitionEvent.RUN, event -> transitionedProperties.add(event.getPropertyName()));
        fixture.trigger().fire();

        assertPhase(fixture.content(), OPENING);
        assertEquals(0.0, fixture.content().getOpacity(), 0.001);
        assertEquals(0.95, fixture.content().getScaleX(), 0.001);
        assertEquals(0.95, fixture.content().getScaleY(), 0.001);
        assertEquals(-8.0, fixture.content().getTranslateY(), 0.001);
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> {
        assertPhase(fixture.content(), OPEN);
        assertEquals(1.0, fixture.content().getOpacity(), 0.001);
        assertEquals(1.0, fixture.content().getScaleX(), 0.001);
        assertEquals(1.0, fixture.content().getScaleY(), 0.001);
        assertEquals(0.0, fixture.content().getTranslateY(), 0.001);
        assertTrue(transitionedProperties.contains("-fx-opacity"));
        assertTrue(transitionedProperties.contains("-fx-scale-x"));
        assertTrue(transitionedProperties.contains("-fx-scale-y"));
        assertTrue(transitionedProperties.contains("-fx-translate-y"));
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void showsAtItsFinalPositionWithoutMovingAnAlreadyVisibleWindow() {
    Fixture fixture = createFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        AtomicReference<Point2D> firstVisiblePosition = new AtomicReference<>();
        fixture
          .popover()
          .getPopup()
          .showingProperty()
          .addListener((ignored, wasShowing, isShowing) -> {
            if (isShowing) {
              firstVisiblePosition.set(
                new Point2D(fixture.popover().getPopup().getX(), fixture.popover().getPopup().getY())
              );
            }
          });

        fixture.trigger().fire();

        assertTrue(fixture.popover().getPopup().isShowing());
        assertEquals(fixture.popover().getPopup().getX(), firstVisiblePosition.get().getX(), 0.01);
        assertEquals(fixture.popover().getPopup().getY(), firstVisiblePosition.get().getY(), 0.01);
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void programmaticCloseKeepsTheNativeWindowVisibleUntilOpacityFinishes() throws Exception {
    Fixture fixture = openFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.popover().setOpen(false);

        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
        assertPhase(fixture.content(), CLOSING);
        assertEquals(0.0, fixture.content().getTranslateX(), 0.001);
        assertEquals(0.0, fixture.content().getTranslateY(), 0.001);
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> {
        assertFalse(fixture.popover().getPopup().isShowing());
        assertPhase(fixture.content(), CLOSED);
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void triggerAndContentActionUseTheExitLifecycle() throws Exception {
    AtomicReference<Popover> actionPopover = new AtomicReference<>();
    com.acteque.terminal.ui.Button action = new com.acteque.terminal.ui.Button("Close");
    action.setOnAction(event -> actionPopover.get().close());
    Fixture fixture = createFixture(new PopoverContent(action));
    actionPopover.set(fixture.popover());
    try {
      openAndAwait(fixture);
      FxTestSupport.runAndWait(() -> {
        action.fire();
        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });
      awaitMotion();

      openAndAwait(fixture);
      FxTestSupport.runAndWait(() -> {
        fixture.trigger().fire();
        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void escapeIsConsumedAndRestoresTriggerFocusAfterTheExit() throws Exception {
    Fixture fixture = openFixture();
    KeyEvent escape = new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false);
    AtomicBoolean reachedEventHandler = new AtomicBoolean();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.content().addEventHandler(KeyEvent.KEY_PRESSED, event -> reachedEventHandler.set(true));
        fixture.content().fireEvent(escape);
        assertFalse(reachedEventHandler.get());
        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> assertSame(fixture.trigger(), fixture.scene().getFocusOwner()));
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void outsideMousePressAndScrollRemainUnconsumedAndDoNotRestoreTriggerFocus() throws Exception {
    Fixture fixture = openFixture();
    MouseEvent press = mousePress();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.outside().requestFocus();
        fixture.outside().fireEvent(press);
        assertFalse(press.isConsumed());
        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });
      awaitMotion();
      FxTestSupport.runAndWait(() -> assertFalse(fixture.scene().getFocusOwner() == fixture.trigger()));

      openAndAwait(fixture);
      ScrollEvent scroll = scroll();
      FxTestSupport.runAndWait(() -> {
        fixture.outside().fireEvent(scroll);
        assertFalse(scroll.isConsumed());
        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void focusLossUsesTheAnimatedDismissalWithoutRestoringTheTrigger() throws Exception {
    Fixture fixture = openFixture();
    AtomicReference<Stage> otherStage = new AtomicReference<>();
    try {
      FxTestSupport.runAndWait(() -> {
        Stage stage = new Stage();
        stage.setScene(new Scene(new StackPane(), 100.0, 100.0));
        stage.show();
        stage.requestFocus();
        otherStage.set(stage);
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> {
        assertFalse(fixture.popover().isOpen());
        assertFalse(fixture.popover().getPopup().isShowing());
        assertFalse(fixture.scene().getFocusOwner() == fixture.trigger());
      });
    } finally {
      FxTestSupport.runAndWait(() -> {
        if (otherStage.get() != null) {
          otherStage.get().hide();
        }
      });
      dispose(fixture);
    }
  }

  @Test
  void missingCssTransitionHidesImmediately() throws Exception {
    Fixture fixture = openFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.content().setStyle("transition-duration: 0ms;");
        fixture.content().applyCss();
        fixture.popover().setOpen(false);

        assertFalse(fixture.popover().isOpen());
        assertFalse(fixture.popover().getPopup().isShowing());
        assertPhase(fixture.content(), CLOSED);
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void closeThenReopenCannotBeHiddenByTheStaleExitCompletion() throws Exception {
    Fixture fixture = openFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.popover().setOpen(false);
        assertTrue(fixture.popover().getPopup().isShowing());

        fixture.popover().show(fixture.trigger());
        assertTrue(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
        assertPhase(fixture.content(), OPEN);
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> {
        assertTrue(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
        assertPhase(fixture.content(), OPEN);
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void ownerKeepsTheExitAliveAfterTheTriggerIsReplaced() throws Exception {
    Fixture fixture = openFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.root().getChildren().remove(fixture.trigger());
        fixture.popover().setOpen(false);

        assertFalse(fixture.popover().isOpen());
        assertTrue(fixture.popover().getPopup().isShowing());
      });
      awaitMotion();
      FxTestSupport.runAndWait(() -> assertFalse(fixture.popover().getPopup().isShowing()));
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void showingAnOpenPopoverDoesNotRestartItsOpeningTransition() throws Exception {
    Fixture fixture = openFixture();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.popover().show(fixture.trigger());

        assertPhase(fixture.content(), OPEN);
        assertEquals(1.0, fixture.content().getOpacity(), 0.001);
        assertEquals(1.0, fixture.content().getScaleX(), 0.001);
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void replacingContentCancelsTheOldOpeningCallback() throws Exception {
    Fixture fixture = createFixture();
    PopoverContent previousContent = fixture.content();
    PopoverContent currentContent = new PopoverContent();
    try {
      FxTestSupport.runAndWait(() -> {
        fixture.trigger().fire();
        fixture.popover().setContent(currentContent);
      });

      awaitMotion();
      FxTestSupport.runAndWait(() -> {
        assertPhase(previousContent, CLOSED);
        assertPhase(currentContent, OPEN);
        assertSame(currentContent, fixture.popover().getPopup().getContent().getFirst());
      });
    } finally {
      dispose(fixture);
    }
  }

  @Test
  void mapsTheProvidedSizingSpacingAndColorsToJavaFxCss() {
    FxTestSupport.runAndWait(() -> {
      PopoverTitle title = new PopoverTitle("Title");
      PopoverDescription description = new PopoverDescription("Description");
      PopoverHeader header = new PopoverHeader(title, description);
      PopoverContent content = new PopoverContent(header);
      StackPane root = new StackPane(content);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      root.applyCss();

      assertEquals(288.0, content.getPrefWidth());
      assertEquals(10.0, content.getSpacing());
      assertEquals(new Insets(10.0), content.getPadding());
      assertEquals(8.0, content.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.web("#ffffff"), content.getBackground().getFills().getFirst().getFill());
      assertEquals(2.0, header.getSpacing());
      assertEquals(14.0, title.getFont().getSize());
      assertEquals(Color.web("#737373"), description.getTextFill());
    });
  }

  private static Fixture createFixture() {
    return createFixture(new PopoverContent());
  }

  private static Fixture createFixture(PopoverContent content) {
    AtomicReference<Fixture> reference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Popover popover = new Popover(content);
      PopoverTrigger trigger = new PopoverTrigger("Open", popover);
      StackPane outside = new StackPane();
      outside.setFocusTraversable(true);
      StackPane root = new StackPane(outside, trigger);
      Stage stage = new Stage();
      Scene scene = new Scene(root, 400.0, 300.0);
      stage.setScene(scene);
      new ThemeManager(scene, AppTheme.LIGHT);
      Platform.setImplicitExit(false);
      stage.show();
      root.applyCss();
      root.layout();
      reference.set(new Fixture(popover, content, trigger, outside, root, scene, stage));
    });
    return reference.get();
  }

  private static Fixture openFixture() throws Exception {
    Fixture fixture = createFixture();
    openAndAwait(fixture);
    return fixture;
  }

  private static void openAndAwait(Fixture fixture) throws Exception {
    FxTestSupport.runAndWait(() -> fixture.popover().show(fixture.trigger()));
    awaitMotion();
    FxTestSupport.runAndWait(() -> assertPhase(fixture.popover().getContent(), OPEN));
  }

  private static void awaitMotion() throws InterruptedException {
    Thread.sleep(250L);
    FxTestSupport.runAndWait(() -> {});
  }

  private static void dispose(Fixture fixture) {
    FxTestSupport.runAndWait(() -> {
      fixture.popover().getPopup().hide();
      fixture.stage().hide();
    });
  }

  private static void assertPhase(PopoverContent content, PseudoClass expected) {
    assertTrue(content.getPseudoClassStates().contains(expected));
    for (PseudoClass phase : Set.of(OPENING, OPEN, CLOSING, CLOSED)) {
      if (phase != expected) {
        assertFalse(content.getPseudoClassStates().contains(phase));
      }
    }
  }

  private static MouseEvent mousePress() {
    return new MouseEvent(
      MouseEvent.MOUSE_PRESSED,
      0.0,
      0.0,
      0.0,
      0.0,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      true,
      false,
      false,
      false,
      false,
      false,
      null
    );
  }

  private static ScrollEvent scroll() {
    return new ScrollEvent(
      ScrollEvent.SCROLL,
      0.0,
      0.0,
      0.0,
      0.0,
      false,
      false,
      false,
      false,
      false,
      false,
      0.0,
      10.0,
      0.0,
      10.0,
      ScrollEvent.HorizontalTextScrollUnits.NONE,
      0.0,
      ScrollEvent.VerticalTextScrollUnits.NONE,
      0.0,
      0,
      new PickResult(null, 0.0, 0.0)
    );
  }

  private record Fixture(
    Popover popover,
    PopoverContent content,
    PopoverTrigger trigger,
    StackPane outside,
    StackPane root,
    Scene scene,
    Stage stage
  ) {}
}
