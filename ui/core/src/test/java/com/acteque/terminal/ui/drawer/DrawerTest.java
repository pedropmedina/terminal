package com.acteque.terminal.ui.drawer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.ListView;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.input.TouchPoint;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class DrawerTest {

  @Test
  void composesAndStylesTheDrawerFamily() {
    FxTestSupport.runAndWait(() -> {
      DrawerHeader header = new DrawerHeader(new DrawerTitle("Edit profile"), new DrawerDescription("Make changes."));
      DrawerFooter footer = new DrawerFooter(new DrawerClose("Cancel"));
      DrawerContent content = new DrawerContent(header, footer);
      Drawer drawer = new Drawer(content);
      drawer.setShowSwipeHandle(true);
      StackPane root = new StackPane(drawer);
      new ThemeManager(new Scene(root, 800, 600), AppTheme.LIGHT);

      drawer.show();
      root.applyCss();
      root.layout();
      root.applyCss();

      assertSame(content, drawer.getContent());
      assertSame(drawer.getOverlay(), drawer.getPortal().getChildren().getFirst());
      assertSame(content.getSwipeHandle(), content.getTop());
      assertEquals(new Insets(16, 16, 0, 16), header.getPadding());
      assertEquals(6.0, header.getSpacing());
      assertEquals(new Insets(0, 16, 16, 16), footer.getPadding());
      assertEquals(8.0, footer.getSpacing());
      assertEquals(16.0, ((DrawerTitle) header.getChildren().getFirst()).getFont().getSize());
      assertEquals(Color.web("#737373"), ((DrawerDescription) header.getChildren().get(1)).getTextFill());
      assertEquals(Color.web("#ffffff"), content.getBackground().getFills().getFirst().getFill());
      assertEquals(24.0, content.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.color(0, 0, 0, 0.3), drawer.getOverlay().getFill());
      assertEquals(784.0, content.getWidth());
      assertTrue(content.getHeight() <= 504.0);
    });
  }

  @Test
  void handlesTriggerCloseEscapeAndOutsidePress() {
    FxTestSupport.runAndWait(() -> {
      DrawerClose close = new DrawerClose("Close");
      Drawer drawer = new Drawer(new DrawerContent(close));
      DrawerTrigger trigger = new DrawerTrigger("Open", drawer);
      Button outside = new Button("Outside");
      StackPane root = new StackPane(outside, trigger, drawer);
      new Scene(root, 800, 600);

      trigger.fire();
      root.layout();
      assertTrue(drawer.isOpen());
      assertTrue(drawer.isVisible());
      assertTrue(drawer.getOverlay().isVisible());

      close.fire();
      assertFalse(drawer.isOpen());
      assertFalse(drawer.isVisible());

      drawer.show();
      root.layout();
      drawer
        .getContent()
        .fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
      assertFalse(drawer.isOpen());

      drawer.show();
      root.layout();
      drawer.getOverlay().fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 10, 10));
      assertFalse(drawer.isOpen());

      drawer.setMode(DrawerMode.NON_MODAL);
      drawer.show();
      root.layout();
      assertFalse(drawer.getOverlay().isVisible());
      outside.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 10, 10));
      assertFalse(drawer.isOpen());
    });
  }

  @Test
  void placesSideDrawersAndKeepsOutsideInteractionAvailable() {
    FxTestSupport.runAndWait(() -> {
      DrawerContent content = new DrawerContent(new DrawerHeader(new DrawerTitle("Details")));
      Drawer drawer = new Drawer(content);
      drawer.setDirection(DrawerDirection.RIGHT);
      drawer.setMode(DrawerMode.TRAP_FOCUS);
      StackPane root = new StackPane(drawer);
      new ThemeManager(new Scene(root, 800, 600), AppTheme.DARK);

      drawer.show();
      root.applyCss();
      root.layout();

      assertEquals(384.0, content.getWidth());
      assertEquals(584.0, content.getHeight());
      assertEquals(800.0, drawer.getOverlay().getWidth());
      assertEquals(600.0, drawer.getOverlay().getHeight());
      assertFalse(drawer.getOverlay().isVisible());
      assertEquals(Color.web("#171717"), content.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.color(1, 1, 1, 0.1), content.getBorder().getStrokes().getFirst().getTopStroke());

      root.resize(500, 600);
      root.layout();
      assertEquals(375.0, content.getWidth());
    });
  }

  @Test
  void swipeChoosesSnapPointsAndCanDismiss() {
    FxTestSupport.runAndWait(() -> {
      AtomicLong time = new AtomicLong(1_000_000_000L);
      DrawerContent content = new DrawerContent(new DrawerHeader(new DrawerTitle("Snap")));
      Drawer drawer = new Drawer(content, time::get);
      DrawerSnapPoint shortPoint = DrawerSnapPoint.fraction(0.3);
      DrawerSnapPoint fullPoint = DrawerSnapPoint.fraction(1.0);
      drawer.setSnapPoints(List.of(shortPoint, fullPoint));
      StackPane root = new StackPane(drawer);
      new Scene(root, 800, 600);

      drawer.show();
      root.applyCss();
      root.resize(800, 600);
      root.layout();
      assertSame(shortPoint, drawer.getActiveSnapPoint());
      assertEquals(800.0, drawer.getWidth());
      assertEquals(584.0, content.getHeight());
      assertEquals(180.0, drawer.visibleExtent());
      assertEquals(404.0, content.getTranslateY());

      content.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 400, 400));
      time.addAndGet(100_000_000L);
      content.fireEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, 400, 200));
      content.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, 400, 200));
      assertSame(fullPoint, drawer.getActiveSnapPoint());
      assertEquals(0.0, content.getTranslateY());

      content.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 400, 200));
      time.addAndGet(100_000_000L);
      content.fireEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, 400, 700));
      content.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, 400, 700));
      assertFalse(drawer.isOpen());
    });
  }

  @Test
  void nestedDrawerStacksAndRestoresItsParent() {
    FxTestSupport.runAndWait(() -> {
      Drawer parent = new Drawer(new DrawerContent(new DrawerTitle("Parent")));
      Drawer child = new Drawer(new DrawerContent(new DrawerTitle("Child")));
      parent.addNestedDrawer(child);
      StackPane root = new StackPane(parent);
      new Scene(root, 800, 600);

      parent.show();
      root.layout();
      child.show();
      root.layout();
      assertEquals(0.95, parent.getContent().getScaleX());
      assertTrue(child.isOpen());

      child
        .getContent()
        .fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
      assertFalse(child.isOpen());
      assertTrue(parent.isOpen());
      assertEquals(1.0, parent.getContent().getScaleX());
    });
  }

  @Test
  void rejectsHorizontalSnapPoints() {
    FxTestSupport.runAndWait(() -> {
      Drawer drawer = new Drawer(new DrawerContent());
      drawer.setDirection(DrawerDirection.LEFT);
      assertThrows(IllegalArgumentException.class, () -> drawer.setSnapPoints(List.of(DrawerSnapPoint.pixels(200))));
      assertThrows(IllegalArgumentException.class, () -> DrawerSnapPoint.fraction(1.5));
    });
  }

  @Test
  void placesUpAndLeftDrawersAndMovesTheHandleToTheirDismissalEdges() {
    FxTestSupport.runAndWait(() -> {
      DrawerContent upContent = new DrawerContent(new DrawerHeader(new DrawerTitle("Up")));
      Drawer up = new Drawer(upContent);
      up.setDirection(DrawerDirection.UP);
      up.setShowSwipeHandle(true);
      StackPane upRoot = new StackPane(up);
      new ThemeManager(new Scene(upRoot, 800, 600), AppTheme.LIGHT);
      up.show();
      upRoot.applyCss();
      upRoot.layout();

      assertSame(upContent.getSwipeHandle(), upContent.getBottom());
      assertEquals(8.0, upContent.getLayoutY());

      DrawerContent leftContent = new DrawerContent(new DrawerHeader(new DrawerTitle("Left")));
      Drawer left = new Drawer(leftContent);
      left.setDirection(DrawerDirection.LEFT);
      left.setShowSwipeHandle(true);
      StackPane leftRoot = new StackPane(left);
      new ThemeManager(new Scene(leftRoot, 800, 600), AppTheme.LIGHT);
      left.show();
      leftRoot.applyCss();
      leftRoot.layout();

      assertSame(leftContent.getSwipeHandle(), leftContent.getRight());
      assertEquals(8.0, leftContent.getLayoutX());
      assertEquals(384.0, leftContent.getWidth());
    });
  }

  @Test
  void touchSwipeDismissesAndScrollingKeepsItsGestureUntilTheBoundary() {
    FxTestSupport.runAndWait(() -> {
      AtomicLong time = new AtomicLong(1_000_000_000L);
      StackPane longContent = new StackPane();
      longContent.setPrefHeight(1_200.0);
      ScrollPane scroll = new ScrollPane(longContent);
      ListView<String> list = new ListView<>();
      for (int index = 0; index < 100; index++) {
        list.getItems().add("Item " + index);
      }
      DrawerContent content = new DrawerContent(scroll);
      Drawer drawer = new Drawer(content, time::get);
      drawer.setSnapPoints(List.of(DrawerSnapPoint.fraction(1.0)));
      StackPane root = new StackPane(list, drawer);
      new ThemeManager(new Scene(root, 800, 600), AppTheme.LIGHT);
      drawer.show();
      root.applyCss();
      root.resize(800, 600);
      root.layout();

      ScrollBar listScrollBar = verticalScrollBar(list);
      ScrollBar drawerScrollBar = verticalScrollBar(scroll);
      Region listThumb = assertInstanceOf(Region.class, listScrollBar.lookup(".thumb"));
      Region drawerThumb = assertInstanceOf(Region.class, drawerScrollBar.lookup(".thumb"));
      assertEquals(listScrollBar.prefWidth(-1), drawerScrollBar.prefWidth(-1));
      assertEquals(10.0, drawerScrollBar.prefWidth(-1));
      assertEquals(listThumb.getBackground(), drawerThumb.getBackground());

      scroll.fireEvent(scrollEvent(-120.0));
      assertTrue(scroll.getVvalue() > scroll.getVmin());

      scroll.setVvalue(scroll.getVmax());
      scroll.fireEvent(mouseEvent(MouseEvent.MOUSE_PRESSED, 400, 200));
      time.addAndGet(100_000_000L);
      scroll.fireEvent(mouseEvent(MouseEvent.MOUSE_DRAGGED, 400, 500));
      scroll.fireEvent(mouseEvent(MouseEvent.MOUSE_RELEASED, 400, 500));
      assertTrue(drawer.isOpen());
      assertEquals(584.0, drawer.visibleExtent());

      scroll.setVvalue(scroll.getVmin());
      scroll.fireEvent(touchEvent(TouchEvent.TOUCH_PRESSED, TouchPoint.State.PRESSED, scroll, 400, 200));
      time.addAndGet(100_000_000L);
      scroll.fireEvent(touchEvent(TouchEvent.TOUCH_MOVED, TouchPoint.State.MOVED, scroll, 400, 500));
      scroll.fireEvent(touchEvent(TouchEvent.TOUCH_RELEASED, TouchPoint.State.RELEASED, scroll, 400, 500));
      assertFalse(drawer.isOpen());
    });
  }

  @Test
  void trapsTabInModalModesAndAllowsOutsideFocusWhenNonModal() {
    FxTestSupport.runAndWait(() -> {
      Button first = new Button("First");
      Button second = new Button("Second");
      Button outside = new Button("Outside");
      Drawer drawer = new Drawer(new DrawerContent(first, second));
      StackPane root = new StackPane(outside, drawer);
      Scene scene = new Scene(root, 800, 600);
      drawer.show();
      root.resize(800, 600);
      root.layout();

      first.requestFocus();
      first.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, false, false, false));
      assertSame(second, scene.getFocusOwner());

      drawer.setMode(DrawerMode.TRAP_FOCUS);
      second.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, false, false, false, false));
      assertSame(first, scene.getFocusOwner());

      drawer.setMode(DrawerMode.NON_MODAL);
      outside.requestFocus();
      assertSame(outside, scene.getFocusOwner());
    });
  }

  @Test
  void restoresTriggerFocusAfterClosing() {
    AtomicReference<Drawer> drawerReference = new AtomicReference<>();
    AtomicReference<Button> triggerReference = new AtomicReference<>();
    AtomicReference<Scene> sceneReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Drawer drawer = new Drawer(new DrawerContent(new Button("Inside")));
      DrawerTrigger trigger = new DrawerTrigger("Open", drawer);
      StackPane root = new StackPane(trigger, drawer);
      Scene scene = new Scene(root, 800, 600);
      trigger.requestFocus();
      trigger.fire();
      root.resize(800, 600);
      root.layout();
      drawerReference.set(drawer);
      triggerReference.set(trigger);
      sceneReference.set(scene);
    });
    FxTestSupport.runAndWait(() -> {
      assertTrue(sceneReference.get().getFocusOwner() != triggerReference.get());
      drawerReference.get().close();
    });
    FxTestSupport.runAndWait(() -> assertSame(triggerReference.get(), sceneReference.get().getFocusOwner()));
  }

  @Test
  void keepsThePopupMountedUntilItsCloseAnimationFinishes() throws InterruptedException {
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    AtomicReference<Drawer> drawerReference = new AtomicReference<>();
    try {
      FxTestSupport.runAndWait(() -> {
        Drawer drawer = new Drawer(new DrawerContent(new DrawerTitle("Animated")));
        Stage stage = new Stage();
        stage.setScene(new Scene(new StackPane(drawer), 800, 600));
        stage.show();
        drawer.show();
        stageReference.set(stage);
        drawerReference.set(drawer);
      });
      Thread.sleep(700);
      FxTestSupport.runAndWait(() -> {
        Drawer drawer = drawerReference.get();
        assertTrue(drawer.visibleExtent() > 0);
        drawer.close();
        assertTrue(drawer.isVisible());
      });
      Thread.sleep(700);
      FxTestSupport.runAndWait(() -> assertFalse(drawerReference.get().isVisible()));
    } finally {
      FxTestSupport.runAndWait(() -> {
        if (stageReference.get() != null) {
          stageReference.get().hide();
        }
      });
    }
  }

  @Test
  void openingAnimationTracksPopupSizeChanges() throws InterruptedException {
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    AtomicReference<Drawer> drawerReference = new AtomicReference<>();
    try {
      FxTestSupport.runAndWait(() -> {
        Platform.setImplicitExit(false);
        Drawer drawer = new Drawer(new DrawerContent(new DrawerTitle("Responsive")));
        drawer.setDirection(DrawerDirection.LEFT);
        Stage stage = new Stage();
        stage.setScene(new Scene(new StackPane(drawer), 400, 600));
        stage.show();
        drawer.show();
        stageReference.set(stage);
        drawerReference.set(drawer);
      });
      Thread.sleep(100);
      FxTestSupport.runAndWait(() -> stageReference.get().setWidth(800));
      Thread.sleep(700);
      FxTestSupport.runAndWait(() -> {
        Drawer drawer = drawerReference.get();
        assertEquals(drawer.getContent().getWidth(), drawer.visibleExtent(), 0.01);
      });
    } finally {
      FxTestSupport.runAndWait(() -> {
        if (stageReference.get() != null) {
          stageReference.get().hide();
        }
      });
    }
  }

  private static MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> type, double x, double y) {
    return new MouseEvent(
      type,
      x,
      y,
      x,
      y,
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

  private static TouchEvent touchEvent(
    javafx.event.EventType<TouchEvent> type,
    TouchPoint.State state,
    ScrollPane target,
    double x,
    double y
  ) {
    TouchPoint point = new TouchPoint(1, state, x, y, x, y, target, null);
    return new TouchEvent(type, point, List.of(point), 1, false, false, false, false);
  }

  private static ScrollEvent scrollEvent(double deltaY) {
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
      deltaY,
      0.0,
      deltaY,
      ScrollEvent.HorizontalTextScrollUnits.NONE,
      0.0,
      ScrollEvent.VerticalTextScrollUnits.NONE,
      0.0,
      0,
      null
    );
  }

  private static ScrollBar verticalScrollBar(Node node) {
    return node
      .lookupAll(".scroll-bar")
      .stream()
      .map(ScrollBar.class::cast)
      .filter(scrollBar -> scrollBar.getOrientation() == Orientation.VERTICAL)
      .findFirst()
      .orElseThrow();
  }
}
