package com.acteque.terminal.ui.drawer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.LongSupplier;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.TextInputControl;
import javafx.scene.effect.Effect;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TouchEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import javafx.util.Duration;

/** A composable drawer with edge placement, swipe gestures, snap points, and focus modes. */
public final class Drawer extends StackPane {

  private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
  private static final PseudoClass SWIPING = PseudoClass.getPseudoClass("swiping");
  private static final PseudoClass SNAP_POINTS = PseudoClass.getPseudoClass("snap-points");
  private static final PseudoClass NESTED_OPEN = PseudoClass.getPseudoClass("nested-open");
  private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");
  private static final Interpolator MOTION = Interpolator.SPLINE(0.22, 1.0, 0.36, 1.0);
  private static final Duration MOTION_DURATION = Duration.millis(450);
  private static final double INSET = 8.0;
  private static final double SIDE_BREAKPOINT = 640.0;
  private static final double HEADER_BREAKPOINT = 768.0;
  private static final double DRAG_THRESHOLD = 8.0;
  private static final double SWIPE_PROJECTION_SECONDS = 0.2;

  private final DrawerPortal portal = new DrawerPortal();
  private final DrawerOverlay overlay = new DrawerOverlay();
  private final GaussianBlur backdropBlur = new GaussianBlur(4.0);
  private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
  private final BooleanProperty showSwipeHandle = new SimpleBooleanProperty(this, "showSwipeHandle", false);
  private final BooleanProperty dismissOnOutsidePress = new SimpleBooleanProperty(this, "dismissOnOutsidePress", true);
  private final ObjectProperty<DrawerDirection> direction = new SimpleObjectProperty<>(
    this,
    "direction",
    DrawerDirection.DOWN
  );
  private final ObjectProperty<DrawerMode> mode = new SimpleObjectProperty<>(this, "mode", DrawerMode.MODAL);
  private final ObjectProperty<DrawerContent> content = new SimpleObjectProperty<>(this, "content");
  private final ObjectProperty<Node> backdrop = new SimpleObjectProperty<>(this, "backdrop");
  private final ObjectProperty<DrawerSnapPoint> activeSnapPoint = new SimpleObjectProperty<>(this, "activeSnapPoint");
  private final ObservableList<DrawerSnapPoint> snapPoints = FXCollections.observableArrayList();
  private final ObservableList<DrawerSnapPoint> exposedSnapPoints = FXCollections.unmodifiableObservableList(
    snapPoints
  );
  private final DoubleProperty visibleExtent = new SimpleDoubleProperty(this, "visibleExtent", 0.0);
  private final DoubleProperty nestedOffset = new SimpleDoubleProperty(this, "nestedOffset", 0.0);
  private final List<Drawer> nestedDrawers = new ArrayList<>();
  private final EventHandler<KeyEvent> sceneKeyHandler = this::handleSceneKey;
  private final EventHandler<MouseEvent> sceneMouseHandler = this::handleSceneMouse;
  private final EventHandler<MouseEvent> popupMousePressedHandler = this::handleMousePressed;
  private final EventHandler<MouseEvent> popupMouseDraggedHandler = this::handleMouseDragged;
  private final EventHandler<MouseEvent> popupMouseReleasedHandler = this::handleMouseReleased;
  private final EventHandler<TouchEvent> popupTouchPressedHandler = this::handleTouchPressed;
  private final EventHandler<TouchEvent> popupTouchMovedHandler = this::handleTouchMoved;
  private final EventHandler<TouchEvent> popupTouchReleasedHandler = this::handleTouchReleased;
  private final LongSupplier nanoTimeSource;
  private final ChangeListener<Number> popupSizeListener = (ignored, previous, next) -> popupSizeChanged();
  private final ChangeListener<Node> focusListener = (ignored, previous, next) -> {
    if (
      isOpen() &&
      isTopmostOpen() &&
      getMode() != DrawerMode.NON_MODAL &&
      next != null &&
      !isDescendant(next, getContent())
    ) {
      Platform.runLater(this::focusFirst);
    }
  };
  private Drawer parentDrawer;
  private Scene attachedScene;
  private Node focusBeforeOpen;
  private Effect previousBackdropEffect;
  private Timeline extentAnimation;
  private Timeline stackAnimation;
  private boolean pendingOpen;
  private boolean closing;
  private boolean dragging;
  private boolean touchActive;
  private int activeTouchId = -1;
  private long lastTouchEndNanos;
  private double dragStartCoordinate;
  private double dragStartExtent = Double.NaN;
  private double lastCoordinate;
  private long lastDragNanos;
  private double swipeVelocity;
  private ScrollPane dragScrollPane;

  public Drawer() {
    this(null);
  }

  public Drawer(DrawerContent content) {
    this(content, System::nanoTime);
  }

  Drawer(DrawerContent content, LongSupplier nanoTimeSource) {
    this.nanoTimeSource = Objects.requireNonNull(nanoTimeSource, "nanoTimeSource cannot be null");
    getStyleClass().add("core-drawer");
    setPickOnBounds(false);
    setFocusTraversable(true);
    portal.prefWidthProperty().bind(widthProperty());
    portal.prefHeightProperty().bind(heightProperty());
    overlay.widthProperty().bind(portal.widthProperty());
    overlay.heightProperty().bind(portal.heightProperty());
    overlay.setOnMousePressed(event -> {
      if (isTopmostOpen() && isDismissOnOutsidePress()) {
        close();
      }
      event.consume();
    });
    portal.getChildren().add(overlay);
    getChildren().add(portal);

    open.addListener((ignored, wasOpen, isOpen) -> applyOpenState(isOpen));
    direction.addListener((ignored, previous, next) -> {
      Objects.requireNonNull(next, "direction cannot be null");
      if (!next.isVertical() && !snapPoints.isEmpty()) {
        direction.set(previous);
        throw new IllegalArgumentException("Snap points require an up or down drawer");
      }
      configureDirection();
    });
    mode.addListener((ignored, previous, next) -> {
      Objects.requireNonNull(next, "mode cannot be null");
      if (isOpen()) {
        if (previous == DrawerMode.MODAL) {
          restoreBackdrop(getBackdrop());
        }
        if (next == DrawerMode.MODAL) {
          applyBackdrop(getBackdrop());
        }
      }
      updateOverlay();
    });
    showSwipeHandle.addListener((ignored, previous, next) -> configureDirection());
    this.content.addListener((ignored, previous, next) -> replaceContent(previous, next));
    backdrop.addListener((ignored, previous, next) -> {
      if (isOpen()) {
        restoreBackdrop(previous);
        applyBackdrop(next);
      }
    });
    snapPoints.addListener(
      (javafx.collections.ListChangeListener<DrawerSnapPoint>) change -> {
        if (!snapPoints.isEmpty() && !getDirection().isVertical()) {
          throw new IllegalArgumentException("Snap points require an up or down drawer");
        }
        if (snapPoints.stream().anyMatch(Objects::isNull)) {
          throw new IllegalArgumentException("Snap points cannot contain null");
        }
        activeSnapPoint.set(snapPoints.isEmpty() ? null : snapPoints.getFirst());
        updatePseudoClasses();
        requestLayout();
      }
    );
    visibleExtent.addListener((ignored, previous, next) -> updateTranslation());
    nestedOffset.addListener((ignored, previous, next) -> updateTranslation());
    sceneProperty().addListener((ignored, previous, next) -> {
      detachScene();
      if (isOpen()) {
        attachScene(next);
      }
    });
    setVisible(false);
    setManaged(false);
    setContent(content);
    configureDirection();
    updateOverlay();
  }

  public boolean isOpen() {
    return open.get();
  }

  public void setOpen(boolean value) {
    open.set(value);
  }

  public BooleanProperty openProperty() {
    return open;
  }

  public void show() {
    setOpen(true);
  }

  public void close() {
    setOpen(false);
  }

  public DrawerDirection getDirection() {
    return direction.get();
  }

  public void setDirection(DrawerDirection value) {
    Objects.requireNonNull(value, "direction cannot be null");
    if (!value.isVertical() && !snapPoints.isEmpty()) {
      throw new IllegalArgumentException("Snap points require an up or down drawer");
    }
    direction.set(value);
  }

  public ObjectProperty<DrawerDirection> directionProperty() {
    return direction;
  }

  public DrawerMode getMode() {
    return mode.get();
  }

  public void setMode(DrawerMode value) {
    mode.set(Objects.requireNonNull(value, "mode cannot be null"));
  }

  public ObjectProperty<DrawerMode> modeProperty() {
    return mode;
  }

  public boolean isShowSwipeHandle() {
    return showSwipeHandle.get();
  }

  public void setShowSwipeHandle(boolean value) {
    showSwipeHandle.set(value);
  }

  public BooleanProperty showSwipeHandleProperty() {
    return showSwipeHandle;
  }

  public boolean isDismissOnOutsidePress() {
    return dismissOnOutsidePress.get();
  }

  public void setDismissOnOutsidePress(boolean value) {
    dismissOnOutsidePress.set(value);
  }

  public BooleanProperty dismissOnOutsidePressProperty() {
    return dismissOnOutsidePress;
  }

  public DrawerContent getContent() {
    return content.get();
  }

  public void setContent(DrawerContent value) {
    content.set(value);
  }

  public ObjectProperty<DrawerContent> contentProperty() {
    return content;
  }

  public Node getBackdrop() {
    return backdrop.get();
  }

  public void setBackdrop(Node value) {
    backdrop.set(value);
  }

  public ObjectProperty<Node> backdropProperty() {
    return backdrop;
  }

  public DrawerPortal getPortal() {
    return portal;
  }

  public DrawerOverlay getOverlay() {
    return overlay;
  }

  public ObservableList<DrawerSnapPoint> getSnapPoints() {
    return exposedSnapPoints;
  }

  public void setSnapPoints(List<DrawerSnapPoint> points) {
    Objects.requireNonNull(points, "points cannot be null");
    if (!points.isEmpty() && !getDirection().isVertical()) {
      throw new IllegalArgumentException("Snap points require an up or down drawer");
    }
    if (points.stream().anyMatch(Objects::isNull)) {
      throw new IllegalArgumentException("Snap points cannot contain null");
    }
    snapPoints.setAll(points);
  }

  public DrawerSnapPoint getActiveSnapPoint() {
    return activeSnapPoint.get();
  }

  public void setActiveSnapPoint(DrawerSnapPoint value) {
    if (value == null && !snapPoints.isEmpty()) {
      throw new IllegalArgumentException("An open drawer with snap points needs an active snap point");
    }
    if (value != null && !snapPoints.contains(value)) {
      throw new IllegalArgumentException("Active snap point must be configured on this drawer");
    }
    activeSnapPoint.set(value);
    if (isOpen()) {
      animateExtent(targetExtent(), false);
    }
  }

  public ReadOnlyObjectProperty<DrawerSnapPoint> activeSnapPointProperty() {
    return activeSnapPoint;
  }

  double visibleExtent() {
    return visibleExtent.get();
  }

  /** Adds a child whose portal shares this drawer's full-size host. */
  public void addNestedDrawer(Drawer child) {
    Objects.requireNonNull(child, "child cannot be null");
    if (child == this || child.parentDrawer != null || isAncestorOf(child)) {
      throw new IllegalArgumentException("Drawer is already nested or would create a cycle");
    }
    child.parentDrawer = this;
    nestedDrawers.add(child);
    portal.getChildren().add(child);
  }

  @Override
  protected void layoutChildren() {
    DrawerContent popup = getContent();
    if (popup != null && getWidth() > 0 && getHeight() > 0) {
      boolean vertical = getDirection().isVertical();
      if (vertical) {
        double height = snapPoints.isEmpty()
          ? Math.max(0.0, getHeight() - 96.0)
          : Math.max(0.0, getHeight() - 2 * INSET);
        popup.setPrefWidth(Math.max(0.0, getWidth() - 2 * INSET));
        popup.setMaxWidth(Math.max(0.0, getWidth() - 2 * INSET));
        popup.setPrefHeight(snapPoints.isEmpty() ? USE_COMPUTED_SIZE : height);
        popup.setMaxHeight(height);
      } else {
        double width = getWidth() < SIDE_BREAKPOINT ? getWidth() * 0.75 : 384.0;
        popup.setPrefWidth(Math.min(width, Math.max(0.0, getWidth() - 2 * INSET)));
        popup.setMaxWidth(popup.getPrefWidth());
        popup.setPrefHeight(Math.max(0.0, getHeight() - 2 * INSET));
        popup.setMaxHeight(popup.getPrefHeight());
      }
      popup.setMinWidth(0.0);
      popup.setMinHeight(0.0);
    }
    super.layoutChildren();
    if (popup != null) {
      for (Node node : popup.getBody().getChildren()) {
        if (node instanceof DrawerHeader header) {
          header.pseudoClassStateChanged(PseudoClass.getPseudoClass("wide"), getWidth() >= HEADER_BREAKPOINT);
        }
      }
      if (pendingOpen && popup.getWidth() > 0 && popup.getHeight() > 0) {
        pendingOpen = false;
        animateExtent(targetExtent(), false);
      } else if (isOpen() && !dragging && extentAnimation == null) {
        visibleExtent.set(targetExtent());
      }
      updateTranslation();
      updatePseudoClasses();
    }
  }

  private void replaceContent(DrawerContent previous, DrawerContent next) {
    if (previous != null) {
      portal.getChildren().remove(previous);
      previous.widthProperty().removeListener(popupSizeListener);
      previous.heightProperty().removeListener(popupSizeListener);
      previous.removeEventFilter(MouseEvent.MOUSE_PRESSED, popupMousePressedHandler);
      previous.removeEventFilter(MouseEvent.MOUSE_DRAGGED, popupMouseDraggedHandler);
      previous.removeEventFilter(MouseEvent.MOUSE_RELEASED, popupMouseReleasedHandler);
      previous.removeEventFilter(TouchEvent.TOUCH_PRESSED, popupTouchPressedHandler);
      previous.removeEventFilter(TouchEvent.TOUCH_MOVED, popupTouchMovedHandler);
      previous.removeEventFilter(TouchEvent.TOUCH_RELEASED, popupTouchReleasedHandler);
    }
    if (next != null) {
      portal.getChildren().add(1, next);
      next.addEventFilter(MouseEvent.MOUSE_PRESSED, popupMousePressedHandler);
      next.addEventFilter(MouseEvent.MOUSE_DRAGGED, popupMouseDraggedHandler);
      next.addEventFilter(MouseEvent.MOUSE_RELEASED, popupMouseReleasedHandler);
      next.addEventFilter(TouchEvent.TOUCH_PRESSED, popupTouchPressedHandler);
      next.addEventFilter(TouchEvent.TOUCH_MOVED, popupTouchMovedHandler);
      next.addEventFilter(TouchEvent.TOUCH_RELEASED, popupTouchReleasedHandler);
      next.widthProperty().addListener(popupSizeListener);
      next.heightProperty().addListener(popupSizeListener);
      next.configureHandle(getDirection(), isShowSwipeHandle());
      configureDirection();
      requestLayout();
    }
  }

  private void popupSizeChanged() {
    DrawerContent popup = getContent();
    if (popup == null || popup.getWidth() <= 0 || popup.getHeight() <= 0) {
      return;
    }
    if (pendingOpen) {
      pendingOpen = false;
      animateExtent(targetExtent(), false);
    } else if (isOpen() && !dragging && extentAnimation == null) {
      visibleExtent.set(targetExtent());
    }
    updateTranslation();
    updatePseudoClasses();
  }

  private void configureDirection() {
    DrawerContent popup = getContent();
    if (popup == null) {
      return;
    }
    DrawerDirection selected = getDirection();
    for (DrawerDirection candidate : DrawerDirection.values()) {
      popup.pseudoClassStateChanged(PseudoClass.getPseudoClass(candidate.name().toLowerCase()), candidate == selected);
    }
    popup.pseudoClassStateChanged(PseudoClass.getPseudoClass("vertical"), selected.isVertical());
    popup.pseudoClassStateChanged(PseudoClass.getPseudoClass("horizontal"), !selected.isVertical());
    popup.configureHandle(selected, isShowSwipeHandle());
    Pos alignment = switch (selected) {
      case DOWN -> Pos.BOTTOM_CENTER;
      case UP -> Pos.TOP_CENTER;
      case LEFT -> Pos.CENTER_LEFT;
      case RIGHT -> Pos.CENTER_RIGHT;
    };
    StackPane.setAlignment(popup, alignment);
    StackPane.setMargin(popup, new Insets(INSET));
    updatePseudoClasses();
    requestLayout();
  }

  private void applyOpenState(boolean opening) {
    updatePseudoClasses();
    if (opening) {
      if (parentDrawer != null && !parentDrawer.isOpen()) {
        open.set(false);
        throw new IllegalStateException("A parent drawer must be open before its nested drawer");
      }
      if (parentDrawer != null) {
        for (Drawer sibling : parentDrawer.nestedDrawers) {
          if (sibling != this && sibling.isOpen()) {
            sibling.close();
          }
        }
        toFront();
        parentDrawer.updateNestedStack();
      }
      focusBeforeOpen = getScene() == null ? null : getScene().getFocusOwner();
      closing = false;
      setManaged(true);
      setVisible(true);
      attachScene(getScene());
      applyBackdrop(getBackdrop());
      if (!snapPoints.isEmpty()) {
        activeSnapPoint.set(snapPoints.getFirst());
      }
      pendingOpen = true;
      requestLayout();
      Platform.runLater(() -> {
        if (isOpen()) {
          focusFirst();
        }
      });
    } else {
      dragging = false;
      touchActive = false;
      activeTouchId = -1;
      dragStartExtent = Double.NaN;
      dragScrollPane = null;
      if (getContent() != null) {
        getContent().pseudoClassStateChanged(SWIPING, false);
      }
      for (Drawer child : nestedDrawers) {
        if (child.isOpen()) {
          child.close();
        }
      }
      pendingOpen = false;
      closing = true;
      detachScene();
      restoreBackdrop(getBackdrop());
      if (parentDrawer != null) {
        parentDrawer.updateNestedStack();
      }
      animateExtent(0.0, true);
      Node returnFocus = focusBeforeOpen;
      focusBeforeOpen = null;
      if (returnFocus != null) {
        Platform.runLater(returnFocus::requestFocus);
      }
    }
    updateOverlay();
  }

  private void updatePseudoClasses() {
    pseudoClassStateChanged(OPEN, isOpen());
    DrawerContent popup = getContent();
    if (popup != null) {
      popup.pseudoClassStateChanged(OPEN, isOpen());
      popup.pseudoClassStateChanged(SNAP_POINTS, !snapPoints.isEmpty());
      popup.pseudoClassStateChanged(
        EXPANDED,
        !snapPoints.isEmpty() && getActiveSnapPoint() != null && Math.abs(targetExtent() - popup.getHeight()) < 1.0
      );
    }
  }

  private double targetExtent() {
    DrawerContent popup = getContent();
    if (popup == null) {
      return 0.0;
    }
    double full = getDirection().isVertical() ? popup.getHeight() : popup.getWidth();
    if (snapPoints.isEmpty() || getActiveSnapPoint() == null) {
      return full;
    }
    return Math.min(full, getActiveSnapPoint().resolve(getHeight()));
  }

  private void animateExtent(double target, boolean hideAfter) {
    if (extentAnimation != null) {
      extentAnimation.stop();
      extentAnimation = null;
    }
    if (getContent() == null || !isShowing()) {
      visibleExtent.set(target);
      if (hideAfter) {
        finishClosing();
      }
      return;
    }
    Timeline animation = new Timeline(new KeyFrame(MOTION_DURATION, new KeyValue(visibleExtent, target, MOTION)));
    extentAnimation = animation;
    animation.setOnFinished(event -> {
      if (extentAnimation == animation) {
        extentAnimation = null;
      }
      if (hideAfter && !isOpen()) {
        finishClosing();
      }
    });
    animation.play();
  }

  private boolean isShowing() {
    Scene scene = getScene();
    Window window = scene == null ? null : scene.getWindow();
    return window != null && window.isShowing();
  }

  private void finishClosing() {
    if (isOpen()) {
      return;
    }
    closing = false;
    setVisible(false);
    setManaged(false);
    visibleExtent.set(0.0);
  }

  private void updateTranslation() {
    DrawerContent popup = getContent();
    if (popup == null) {
      return;
    }
    double full = getDirection().isVertical() ? popup.getHeight() : popup.getWidth();
    double hidden = Math.max(0.0, full - visibleExtent.get());
    if (visibleExtent.get() == 0.0) {
      hidden += 2.0;
    }
    double stack = nestedOffset.get();
    double scaleCorrection = ((1.0 - popup.getScaleX()) * full) / 2.0;
    if (getDirection().isVertical()) {
      popup.setTranslateX(0.0);
      popup.setTranslateY(getDirection().dismissSign() * (hidden - stack + scaleCorrection));
    } else {
      popup.setTranslateY(0.0);
      popup.setTranslateX(getDirection().dismissSign() * (hidden - stack + scaleCorrection));
    }
    updateOverlay();
  }

  private void updateOverlay() {
    boolean modal = getMode() == DrawerMode.MODAL;
    overlay.setVisible(modal && (isOpen() || closing));
    overlay.setMouseTransparent(!modal);
    double full =
      getContent() == null ? 0.0 : getDirection().isVertical() ? getContent().getHeight() : getContent().getWidth();
    double progress = full <= 0 ? 0.0 : Math.min(1.0, visibleExtent.get() / full);
    overlay.setOpacity(modal ? (closing ? progress : snapPoints.isEmpty() ? progress : Math.max(0.5, progress)) : 0.0);
  }

  private void attachScene(Scene scene) {
    if (scene == null || attachedScene == scene) {
      return;
    }
    detachScene();
    attachedScene = scene;
    scene.addEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
    scene.addEventFilter(MouseEvent.MOUSE_PRESSED, sceneMouseHandler);
    scene.focusOwnerProperty().addListener(focusListener);
  }

  private void detachScene() {
    if (attachedScene != null) {
      attachedScene.removeEventFilter(KeyEvent.KEY_PRESSED, sceneKeyHandler);
      attachedScene.removeEventFilter(MouseEvent.MOUSE_PRESSED, sceneMouseHandler);
      attachedScene.focusOwnerProperty().removeListener(focusListener);
      attachedScene = null;
    }
  }

  private void handleSceneKey(KeyEvent event) {
    if (!isOpen() || !isTopmostOpen()) {
      return;
    }
    if (event.getCode() == KeyCode.ESCAPE) {
      close();
      event.consume();
    } else if (event.getCode() == KeyCode.TAB && getMode() != DrawerMode.NON_MODAL) {
      cycleFocus(event);
    }
  }

  private void handleSceneMouse(MouseEvent event) {
    if (
      !isOpen() ||
      !isTopmostOpen() ||
      getMode() == DrawerMode.MODAL ||
      !isDismissOnOutsidePress() ||
      getContent() == null
    ) {
      return;
    }
    if (event.getTarget() instanceof Node target && !isDescendant(target, getContent())) {
      close();
    }
  }

  private void cycleFocus(KeyEvent event) {
    List<Node> focusable = new ArrayList<>();
    collectFocusableChildren(getContent(), focusable);
    if (focusable.isEmpty()) {
      getContent().requestFocus();
      event.consume();
      return;
    }
    Node current = getScene() == null ? null : getScene().getFocusOwner();
    int index = focusable.indexOf(current);
    int next = event.isShiftDown()
      ? index <= 0
        ? focusable.size() - 1
        : index - 1
      : index < 0 || index == focusable.size() - 1
        ? 0
        : index + 1;
    focusable.get(next).requestFocus();
    event.consume();
  }

  private void focusFirst() {
    if (!isOpen() || getContent() == null || !isTopmostOpen()) {
      return;
    }
    List<Node> focusable = new ArrayList<>();
    collectFocusableChildren(getContent(), focusable);
    if (focusable.isEmpty()) {
      getContent().requestFocus();
    } else {
      focusable.getFirst().requestFocus();
    }
  }

  private static void collectFocusable(Node node, List<Node> focusable) {
    if (node.isFocusTraversable() && node.isVisible() && !node.isDisabled()) {
      focusable.add(node);
    }
    if (node instanceof Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) {
        collectFocusable(child, focusable);
      }
    }
  }

  private static void collectFocusableChildren(Parent parent, List<Node> focusable) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      collectFocusable(child, focusable);
    }
  }

  private static boolean isDescendant(Node candidate, Node ancestor) {
    for (Node current = candidate; current != null; current = current.getParent()) {
      if (current == ancestor) {
        return true;
      }
    }
    return false;
  }

  private boolean isTopmostOpen() {
    return isOpen() && nestedDrawers.stream().noneMatch(Drawer::isOpen);
  }

  private boolean isAncestorOf(Drawer candidate) {
    for (Drawer current = this; current != null; current = current.parentDrawer) {
      if (current == candidate) {
        return true;
      }
    }
    return false;
  }

  private void updateNestedStack() {
    int count = nestedDrawers
      .stream()
      .filter(Drawer::isOpen)
      .mapToInt(child -> 1 + child.nestedDepth())
      .max()
      .orElse(0);
    DrawerContent popup = getContent();
    if (popup != null) {
      popup.pseudoClassStateChanged(NESTED_OPEN, count > 0);
      if (stackAnimation != null) {
        stackAnimation.stop();
      }
      double scale = Math.max(0.0, 1.0 - 0.05 * count);
      if (!isShowing()) {
        popup.setScaleX(scale);
        popup.setScaleY(scale);
        nestedOffset.set(16.0 * count);
        if (parentDrawer != null) {
          parentDrawer.updateNestedStack();
        }
        return;
      }
      Timeline animation = new Timeline(
        new KeyFrame(
          MOTION_DURATION,
          new KeyValue(popup.scaleXProperty(), scale, MOTION),
          new KeyValue(popup.scaleYProperty(), scale, MOTION),
          new KeyValue(nestedOffset, 16.0 * count, MOTION)
        )
      );
      stackAnimation = animation;
      animation.play();
    }
    if (parentDrawer != null) {
      parentDrawer.updateNestedStack();
    }
  }

  private int nestedDepth() {
    return nestedDrawers
      .stream()
      .filter(Drawer::isOpen)
      .mapToInt(child -> 1 + child.nestedDepth())
      .max()
      .orElse(0);
  }

  private void applyBackdrop(Node target) {
    if (target == null || getMode() != DrawerMode.MODAL) {
      return;
    }
    previousBackdropEffect = target.getEffect();
    backdropBlur.setInput(previousBackdropEffect);
    target.setEffect(backdropBlur);
  }

  private void restoreBackdrop(Node target) {
    if (target != null && target.getEffect() == backdropBlur) {
      target.setEffect(previousBackdropEffect);
    }
    backdropBlur.setInput(null);
    previousBackdropEffect = null;
  }

  private void handleMousePressed(MouseEvent event) {
    if (touchActive || (event.isSynthesized() && nanoTimeSource.getAsLong() - lastTouchEndNanos < 500_000_000L)) {
      return;
    }
    startGesture(event.getTarget(), axisCoordinate(event.getSceneX(), event.getSceneY()));
  }

  private void handleMouseDragged(MouseEvent event) {
    if (!touchActive && moveGesture(axisCoordinate(event.getSceneX(), event.getSceneY()))) {
      event.consume();
    }
  }

  private void handleMouseReleased(MouseEvent event) {
    if (!touchActive && dragging) {
      finishGesture();
      event.consume();
    }
  }

  private void handleTouchPressed(TouchEvent event) {
    if (activeTouchId != -1) {
      return;
    }
    activeTouchId = event.getTouchPoint().getId();
    touchActive = true;
    startGesture(
      event.getTarget(),
      axisCoordinate(event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY())
    );
  }

  private void handleTouchMoved(TouchEvent event) {
    if (event.getTouchPoint().getId() != activeTouchId) {
      return;
    }
    if (moveGesture(axisCoordinate(event.getTouchPoint().getSceneX(), event.getTouchPoint().getSceneY()))) {
      event.consume();
    }
  }

  private void handleTouchReleased(TouchEvent event) {
    if (event.getTouchPoint().getId() != activeTouchId) {
      return;
    }
    if (dragging) {
      finishGesture();
      event.consume();
    }
    touchActive = false;
    activeTouchId = -1;
    lastTouchEndNanos = nanoTimeSource.getAsLong();
  }

  private double axisCoordinate(double sceneX, double sceneY) {
    return getDirection().isVertical() ? sceneY : sceneX;
  }

  private void startGesture(Object eventTarget, double coordinate) {
    if (!isOpen() || !isTopmostOpen() || getContent() == null || !(eventTarget instanceof Node target)) {
      dragStartExtent = Double.NaN;
      return;
    }
    if (hasInteractiveAncestor(target)) {
      dragStartExtent = Double.NaN;
      return;
    }
    dragScrollPane = enclosingScrollPane(target);
    dragStartCoordinate = coordinate;
    lastCoordinate = coordinate;
    dragStartExtent = visibleExtent.get();
    lastDragNanos = nanoTimeSource.getAsLong();
    swipeVelocity = 0.0;
  }

  private boolean moveGesture(double coordinate) {
    if (Double.isNaN(dragStartExtent) || !isOpen()) {
      return false;
    }
    double displacement = (coordinate - dragStartCoordinate) * getDirection().dismissSign();
    if (!dragging) {
      if (
        Math.abs(displacement) < DRAG_THRESHOLD ||
        (snapPoints.isEmpty() && displacement < 0) ||
        !scrollAtBoundary(displacement)
      ) {
        return false;
      }
      dragging = true;
      if (extentAnimation != null) {
        extentAnimation.stop();
        extentAnimation = null;
      }
      getContent().pseudoClassStateChanged(SWIPING, true);
    }
    long now = nanoTimeSource.getAsLong();
    double seconds = (now - lastDragNanos) / 1_000_000_000.0;
    if (seconds > 0) {
      swipeVelocity = ((coordinate - lastCoordinate) * getDirection().dismissSign()) / seconds;
    }
    lastCoordinate = coordinate;
    lastDragNanos = now;
    double full = getDirection().isVertical() ? getContent().getHeight() : getContent().getWidth();
    visibleExtent.set(Math.max(0.0, Math.min(full, dragStartExtent - displacement)));
    return true;
  }

  private void finishGesture() {
    dragging = false;
    getContent().pseudoClassStateChanged(SWIPING, false);
    double projected = visibleExtent.get() - swipeVelocity * SWIPE_PROJECTION_SECONDS;
    double closest = 0.0;
    DrawerSnapPoint selected = null;
    double distance = Math.abs(projected);
    if (snapPoints.isEmpty()) {
      double full = getDirection().isVertical() ? getContent().getHeight() : getContent().getWidth();
      if (Math.abs(full - projected) < distance) {
        closest = full;
      }
    } else {
      for (DrawerSnapPoint point : snapPoints) {
        double resolved = Math.min(getContent().getHeight(), point.resolve(getHeight()));
        if (Math.abs(resolved - projected) < distance) {
          closest = resolved;
          distance = Math.abs(resolved - projected);
          selected = point;
        }
      }
    }
    dragStartExtent = Double.NaN;
    dragScrollPane = null;
    if (closest == 0.0) {
      close();
    } else {
      activeSnapPoint.set(selected);
      updatePseudoClasses();
      animateExtent(closest, false);
    }
  }

  private boolean hasInteractiveAncestor(Node target) {
    for (Node current = target; current != null && current != getContent(); current = current.getParent()) {
      if (current instanceof ButtonBase || current instanceof TextInputControl || current instanceof Slider) {
        return true;
      }
    }
    return false;
  }

  private ScrollPane enclosingScrollPane(Node target) {
    for (Node current = target; current != null && current != getContent(); current = current.getParent()) {
      if (current instanceof ScrollPane scrollPane) {
        return scrollPane;
      }
    }
    return null;
  }

  private boolean scrollAtBoundary(double displacement) {
    if (dragScrollPane == null) {
      return true;
    }
    if (getDirection().isVertical()) {
      boolean towardMin = (getDirection() == DrawerDirection.DOWN) == displacement >= 0;
      return towardMin
        ? dragScrollPane.getVvalue() <= dragScrollPane.getVmin() + 0.001
        : dragScrollPane.getVvalue() >= dragScrollPane.getVmax() - 0.001;
    }
    boolean towardMin = (getDirection() == DrawerDirection.RIGHT) == displacement >= 0;
    return towardMin
      ? dragScrollPane.getHvalue() <= dragScrollPane.getHmin() + 0.001
      : dragScrollPane.getHvalue() >= dragScrollPane.getHmax() - 0.001;
  }
}
