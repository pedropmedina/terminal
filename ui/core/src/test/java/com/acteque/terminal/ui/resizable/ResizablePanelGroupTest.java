package com.acteque.terminal.ui.resizable;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ResizablePanelGroupTest {

  private static final double TOLERANCE = 0.01;

  @Test
  void composesPanelsAndHandlesWithEqualHorizontalSizes() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);
      StackPane root = themedRoot(group, 300.0, 120.0);

      layout(root);

      assertEquals(Orientation.HORIZONTAL, group.getOrientation());
      assertEquals(AccessibleRole.PARENT, group.getAccessibleRole());
      assertEquals(AccessibleRole.PARENT, first.getAccessibleRole());
      assertEquals(AccessibleRole.SLIDER, handle.getAccessibleRole());
      assertTrue(group.getStyleClass().contains("core-resizable-panel-group"));
      assertTrue(first.getStyleClass().contains("core-resizable-panel"));
      assertTrue(handle.getStyleClass().contains("core-resizable-handle"));
      assertEquals(149.5, first.getWidth(), TOLERANCE);
      assertEquals(149.5, second.getWidth(), TOLERANCE);
      assertEquals(150.5, second.getLayoutX(), TOLERANCE);
      assertEquals(0.5, handle.getPosition(), TOLERANCE);
      assertArrayEquals(new double[] { 0.5 }, group.getDividerPositions(), TOLERANCE);
    });
  }

  @Test
  void supportsExplicitPositionsAndVerticalOrientation() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle(true);
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(Orientation.VERTICAL, first, handle, second);
      group.setDividerPositions(0.25);
      StackPane root = themedRoot(group, 200.0, 400.0);

      layout(root);

      assertEquals(99.5, first.getHeight(), TOLERANCE);
      assertEquals(100.5, second.getLayoutY(), TOLERANCE);
      assertEquals(299.5, second.getHeight(), TOLERANCE);
      assertEquals(0.25, handle.getPosition(), TOLERANCE);
      assertTrue(handle.getPseudoClassStates().contains(PseudoClass.getPseudoClass("vertical")));

      group.setOrientation(Orientation.HORIZONTAL);
      layout(root);

      assertEquals(0.25, handle.getPosition(), TOLERANCE);
      assertTrue(handle.getPseudoClassStates().contains(PseudoClass.getPseudoClass("horizontal")));
    });
  }

  @Test
  void clampsProgrammaticAndKeyboardResizingToPanelConstraints() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      first.setMinWidth(100.0);
      first.setMaxWidth(180.0);
      ResizableHandle handle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      second.setMinWidth(80.0);
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);
      group.setDividerPositions(0.1);
      StackPane root = themedRoot(group, 300.0, 120.0);

      layout(root);

      assertEquals(100.0, first.getWidth(), TOLERANCE);
      assertEquals(199.0, second.getWidth(), TOLERANCE);

      handle.fireEvent(key(KeyCode.END));
      layout(root);

      assertEquals(180.0, first.getWidth(), TOLERANCE);
      assertEquals(119.0, second.getWidth(), TOLERANCE);

      handle.fireEvent(key(KeyCode.HOME));
      layout(root);

      assertEquals(100.0, first.getWidth(), TOLERANCE);
      assertEquals(199.0, second.getWidth(), TOLERANCE);
    });
  }

  @Test
  void resizesAdjacentPanelsWithArrowKeysAndPublishesPositionChanges() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);
      StackPane root = themedRoot(group, 300.0, 120.0);
      AtomicInteger changes = new AtomicInteger();
      handle.positionProperty().addListener(ignored -> changes.incrementAndGet());
      layout(root);

      handle.fireEvent(key(KeyCode.RIGHT));
      layout(root);

      assertEquals(159.5, first.getWidth(), TOLERANCE);
      assertEquals(139.5, second.getWidth(), TOLERANCE);
      assertEquals(160.0 / 300.0, handle.getPosition(), TOLERANCE);
      assertTrue(changes.get() > 0);

      group.setDisable(true);
      handle.fireEvent(key(KeyCode.RIGHT));
      layout(root);

      assertEquals(159.5, first.getWidth(), TOLERANCE);
    });
  }

  @Test
  void pointerDragResizesOnlyThePanelsAdjacentToTheHandle() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle firstHandle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      ResizableHandle secondHandle = new ResizableHandle();
      ResizablePanel third = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, firstHandle, second, secondHandle, third);
      StackPane root = themedRoot(group, 302.0, 120.0);
      layout(root);
      double thirdWidth = third.getWidth();

      firstHandle.fireEvent(mouse(MouseEvent.MOUSE_PRESSED, 5.0));
      firstHandle.fireEvent(mouse(MouseEvent.MOUSE_DRAGGED, 25.0));
      firstHandle.fireEvent(mouse(MouseEvent.MOUSE_RELEASED, 25.0));
      layout(root);

      assertEquals(120.0, first.getWidth(), TOLERANCE);
      assertEquals(80.0, second.getWidth(), TOLERANCE);
      assertEquals(thirdWidth, third.getWidth(), TOLERANCE);
    });
  }

  @Test
  void exposesSliderAccessibilityAndAccessibleActions() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);
      StackPane root = themedRoot(group, 300.0, 120.0);
      layout(root);

      assertEquals(50.0, (double) handle.queryAccessibleAttribute(AccessibleAttribute.VALUE), TOLERANCE);
      assertEquals(Orientation.HORIZONTAL, handle.queryAccessibleAttribute(AccessibleAttribute.ORIENTATION));
      assertEquals(
        (100.0 * 0.5) / 300.0,
        (double) handle.queryAccessibleAttribute(AccessibleAttribute.MIN_VALUE),
        TOLERANCE
      );
      assertEquals(
        (100.0 * 299.5) / 300.0,
        (double) handle.queryAccessibleAttribute(AccessibleAttribute.MAX_VALUE),
        TOLERANCE
      );

      handle.executeAccessibleAction(AccessibleAction.INCREMENT);
      layout(root);
      assertEquals(160.0 / 300.0, handle.getPosition(), TOLERANCE);

      handle.executeAccessibleAction(AccessibleAction.SET_VALUE, 25.0);
      layout(root);
      assertEquals(0.25, handle.getPosition(), TOLERANCE);
    });
  }

  @Test
  void mapsDividerGripAndFocusRingFromCss() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle(true);
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);
      StackPane root = themedRoot(group, 300.0, 120.0);
      layout(root);

      Region line = (Region) handle.lookup(".resizable-handle-line");
      Region grip = (Region) handle.lookup(".resizable-handle-grip");
      assertEquals(1.0, line.getWidth(), TOLERANCE);
      assertEquals(120.0, line.getHeight(), TOLERANCE);
      assertEquals(Color.web("#e5e5e5"), line.getBackground().getFills().getFirst().getFill());
      assertEquals(4.0, grip.getWidth(), TOLERANCE);
      assertEquals(24.0, grip.getHeight(), TOLERANCE);
      assertEquals(8.0, grip.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());

      handle.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-visible"), true);
      root.applyCss();

      assertEquals(Color.web("#a1a1a1"), handle.getBorder().getStrokes().getFirst().getTopStroke());

      ResizableHandle darkHandle = new ResizableHandle();
      StackPane darkRoot = themedRoot(
        new ResizablePanelGroup(new ResizablePanel(), darkHandle, new ResizablePanel()),
        AppTheme.DARK,
        300.0,
        120.0
      );
      layout(darkRoot);
      Region darkLine = (Region) darkHandle.lookup(".resizable-handle-line");
      assertEquals(Color.web("rgba(255, 255, 255, 0.1)"), darkLine.getBackground().getFills().getFirst().getFill());
    });
  }

  @Test
  void validatesCompositionOrientationAndDividerPositions() {
    FxTestSupport.runAndWait(() -> {
      ResizablePanel first = new ResizablePanel();
      ResizableHandle handle = new ResizableHandle();
      ResizablePanel second = new ResizablePanel();
      ResizablePanelGroup group = new ResizablePanelGroup(first, handle, second);

      assertThrows(NullPointerException.class, () -> group.setOrientation(null));
      assertThrows(IllegalArgumentException.class, () -> group.setDividerPositions());
      assertThrows(IllegalArgumentException.class, () -> group.setDividerPositions(Double.NaN));
      assertThrows(IllegalArgumentException.class, () -> group.setDividerPositions(1.1));

      ResizablePanelGroup invalid = new ResizablePanelGroup(new ResizablePanel(), new ResizablePanel());
      assertThrows(IllegalStateException.class, invalid::getDividerPositions);

      assertFalse(handle.isWithHandle());
      Region grip = (Region) handle.lookup(".resizable-handle-grip");
      assertFalse(grip.isManaged());
      assertFalse(grip.isVisible());
      handle.setWithHandle(true);
      assertTrue(handle.isWithHandle());
      assertTrue(grip.isManaged());
      assertTrue(grip.isVisible());
    });
  }

  private static KeyEvent key(KeyCode code) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
  }

  private static MouseEvent mouse(javafx.event.EventType<MouseEvent> type, double x) {
    return new MouseEvent(
      type,
      x,
      5.0,
      x,
      5.0,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      true,
      false,
      false,
      true,
      false,
      true,
      null
    );
  }

  private static StackPane themedRoot(ResizablePanelGroup group, double width, double height) {
    return themedRoot(group, AppTheme.LIGHT, width, height);
  }

  private static StackPane themedRoot(ResizablePanelGroup group, AppTheme theme, double width, double height) {
    StackPane root = new StackPane(group);
    new ThemeManager(new Scene(root, width, height), theme);
    root.resize(width, height);
    return root;
  }

  private static void layout(StackPane root) {
    root.applyCss();
    root.layout();
  }
}
