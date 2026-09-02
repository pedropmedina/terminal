package com.acteque.terminal.ui.core;

import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/** A shadcn-styled facade over JavaFX's native tooltip behavior. */
public final class Tooltip extends javafx.scene.control.Tooltip {

  public enum Align {
    START,
    CENTER,
    END,
  }

  public enum Side {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    INLINE_START,
    INLINE_END,
  }

  private static final Duration DEFAULT_SHOW_DELAY = Duration.millis(350.0);
  private static final double SCREEN_PADDING = 8.0;
  private static final Object TRIGGER_REGISTRATION_KEY = new Object();

  private final PauseTransition focusDelay = new PauseTransition();
  private final Content content;
  private Node anchor;

  public Tooltip() {
    this(new Node[0]);
  }

  public Tooltip(String text) {
    this(new Label(Objects.requireNonNull(text, "text")));
  }

  public Tooltip(Node... children) {
    content = new Content(Objects.requireNonNull(children, "children"));
    getStyleClass().setAll("tooltip", "core-tooltip");
    setShowDelay(DEFAULT_SHOW_DELAY);
    setShowDuration(Duration.INDEFINITE);
    setAutoFix(false);
    setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
    addEventHandler(WindowEvent.WINDOW_SHOWING, event -> content.setOpenState(false));
    addEventHandler(WindowEvent.WINDOW_SHOWN, event ->
      Platform.runLater(() -> {
        if (isShowing()) {
          reposition();
          content.setOpenState(true);
        }
      })
    );
    addEventHandler(WindowEvent.WINDOW_HIDING, event -> content.setOpenState(false));
    focusDelay.setOnFinished(event -> showForFocusedAnchor());
    setGraphic(content);
  }

  public ObservableList<Node> getContentNodes() {
    return content.popup.getChildren();
  }

  public Align getAlign() {
    return content.align.get();
  }

  public void setAlign(Align value) {
    content.align.set(value);
  }

  public ObjectProperty<Align> alignProperty() {
    return content.align;
  }

  public Side getSide() {
    return content.side.get();
  }

  public void setSide(Side value) {
    content.side.set(value);
  }

  public ObjectProperty<Side> sideProperty() {
    return content.side;
  }

  public double getAlignOffset() {
    return content.alignOffset.get();
  }

  public void setAlignOffset(double value) {
    content.alignOffset.set(value);
  }

  public DoubleProperty alignOffsetProperty() {
    return content.alignOffset;
  }

  public double getSideOffset() {
    return content.sideOffset.get();
  }

  public void setSideOffset(double value) {
    content.sideOffset.set(value);
  }

  public DoubleProperty sideOffsetProperty() {
    return content.sideOffset;
  }

  /** Installs native hover behavior plus shadcn positioning and keyboard-focus behavior. */
  public void install(Node target) {
    Node requiredTarget = Objects.requireNonNull(target, "target");
    Object existingRegistration = requiredTarget.getProperties().get(TRIGGER_REGISTRATION_KEY);
    if (existingRegistration instanceof TriggerRegistration existing) {
      if (existing.owner() == this) {
        return;
      }
      existing.owner().uninstall(requiredTarget);
    }

    EventHandler<MouseEvent> anchorRecorder = event -> recordAnchor(requiredTarget);
    ChangeListener<Boolean> focusListener = (ignored, wasFocused, isFocused) -> {
      if (isFocused) {
        showFromFocus(requiredTarget);
      } else if (!requiredTarget.isHover()) {
        hideFromFocus();
      }
    };
    TriggerRegistration registration = new TriggerRegistration(this, anchorRecorder, focusListener);
    requiredTarget.getProperties().put(TRIGGER_REGISTRATION_KEY, registration);
    recordAnchor(requiredTarget);
    requiredTarget.addEventHandler(MouseEvent.MOUSE_ENTERED, anchorRecorder);
    requiredTarget.focusedProperty().addListener(focusListener);
    javafx.scene.control.Tooltip.install(requiredTarget, this);
  }

  /** Removes this tooltip and its shadcn-specific handlers from a target. */
  public void uninstall(Node target) {
    Node requiredTarget = Objects.requireNonNull(target, "target");
    Object storedRegistration = requiredTarget.getProperties().get(TRIGGER_REGISTRATION_KEY);
    if (!(storedRegistration instanceof TriggerRegistration registration) || registration.owner() != this) {
      return;
    }

    requiredTarget.getProperties().remove(TRIGGER_REGISTRATION_KEY);
    javafx.scene.control.Tooltip.uninstall(requiredTarget, this);
    requiredTarget.removeEventHandler(MouseEvent.MOUSE_ENTERED, registration.anchorRecorder());
    requiredTarget.focusedProperty().removeListener(registration.focusListener());
    if (anchor == requiredTarget) {
      focusDelay.stop();
      hide();
      anchor = null;
    }
  }

  void recordAnchor(Node value) {
    anchor = Objects.requireNonNull(value, "anchor");
    applyAnchorTheme(value);
  }

  void showFromFocus(Node value) {
    recordAnchor(value);
    focusDelay.stop();
    focusDelay.setDuration(getShowDelay());
    focusDelay.playFromStart();
  }

  void hideFromFocus() {
    focusDelay.stop();
    hide();
  }

  void reposition() {
    if (!isShowing() || anchor == null) {
      return;
    }
    Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
    if (anchorBounds == null) {
      return;
    }

    Content popupContent = content;
    Side side = physicalSide(getSide());
    popupContent.setResolvedSide(side);
    popupContent.applyCss();
    popupContent.autosize();
    sizeToScene();
    double width = getWidth();
    double height = getHeight();
    Rectangle2D screen = screenFor(anchorBounds).getVisualBounds();
    if (!fits(side, anchorBounds, width, height, getSideOffset(), screen)) {
      Side opposite = opposite(side);
      if (fits(opposite, anchorBounds, width, height, getSideOffset(), screen)) {
        side = opposite;
        popupContent.setResolvedSide(side);
        popupContent.autosize();
        sizeToScene();
        width = getWidth();
        height = getHeight();
      }
    }

    double x;
    double y;
    if (side == Side.TOP || side == Side.BOTTOM) {
      x = alignedStart(anchorBounds.getMinX(), anchorBounds.getWidth(), width, getAlign());
      x += getAlignOffset();
      y =
        side == Side.BOTTOM
          ? anchorBounds.getMaxY() + getSideOffset()
          : anchorBounds.getMinY() - height - getSideOffset();
    } else {
      x =
        side == Side.RIGHT
          ? anchorBounds.getMaxX() + getSideOffset()
          : anchorBounds.getMinX() - width - getSideOffset();
      y = alignedStart(anchorBounds.getMinY(), anchorBounds.getHeight(), height, getAlign());
      y += getAlignOffset();
    }

    x = clamp(x, screen.getMinX() + SCREEN_PADDING, screen.getMaxX() - width - SCREEN_PADDING);
    y = clamp(y, screen.getMinY() + SCREEN_PADDING, screen.getMaxY() - height - SCREEN_PADDING);
    setAnchorX(x);
    setAnchorY(y);
  }

  private void showForFocusedAnchor() {
    Scene scene = anchor == null ? null : anchor.getScene();
    Window owner = scene == null ? null : scene.getWindow();
    Bounds bounds = anchor == null ? null : anchor.localToScreen(anchor.getBoundsInLocal());
    if (owner != null && owner.isShowing() && bounds != null && anchor.isFocused()) {
      show(anchor, bounds.getMinX(), bounds.getMinY());
    }
  }

  private Side physicalSide(Side side) {
    if (side == Side.INLINE_START) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT ? Side.RIGHT : Side.LEFT;
    }
    if (side == Side.INLINE_END) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT ? Side.LEFT : Side.RIGHT;
    }
    return side;
  }

  private static boolean fits(
    Side side,
    Bounds anchorBounds,
    double width,
    double height,
    double offset,
    Rectangle2D screen
  ) {
    return switch (side) {
      case TOP -> anchorBounds.getMinY() - height - offset >= screen.getMinY() + SCREEN_PADDING;
      case BOTTOM -> anchorBounds.getMaxY() + height + offset <= screen.getMaxY() - SCREEN_PADDING;
      case LEFT -> anchorBounds.getMinX() - width - offset >= screen.getMinX() + SCREEN_PADDING;
      case RIGHT -> anchorBounds.getMaxX() + width + offset <= screen.getMaxX() - SCREEN_PADDING;
      case INLINE_START, INLINE_END -> throw new IllegalArgumentException("Expected a physical side");
    };
  }

  private static Side opposite(Side side) {
    return switch (side) {
      case TOP -> Side.BOTTOM;
      case BOTTOM -> Side.TOP;
      case LEFT -> Side.RIGHT;
      case RIGHT -> Side.LEFT;
      case INLINE_START, INLINE_END -> throw new IllegalArgumentException("Expected a physical side");
    };
  }

  private static double alignedStart(double anchorStart, double anchorSize, double popupSize, Align align) {
    return switch (align) {
      case START -> anchorStart;
      case CENTER -> anchorStart + (anchorSize - popupSize) / 2.0;
      case END -> anchorStart + anchorSize - popupSize;
    };
  }

  private static double clamp(double value, double minimum, double maximum) {
    return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
  }

  private static Screen screenFor(Bounds anchorBounds) {
    var screens = Screen.getScreensForRectangle(
      anchorBounds.getMinX(),
      anchorBounds.getMinY(),
      anchorBounds.getWidth(),
      anchorBounds.getHeight()
    );
    return screens.isEmpty() ? Screen.getPrimary() : screens.getFirst();
  }

  private void applyAnchorTheme(Node value) {
    Parent root = value.getScene() == null ? null : value.getScene().getRoot();
    getStyleClass().removeAll("theme-light", "theme-dark");
    content.getStyleClass().removeAll("theme-light", "theme-dark");
    if (root != null && root.getStyleClass().contains("theme-dark")) {
      getStyleClass().add("theme-dark");
      content.getStyleClass().add("theme-dark");
    } else if (root != null && root.getStyleClass().contains("theme-light")) {
      getStyleClass().add("theme-light");
      content.getStyleClass().add("theme-light");
    }
  }

  HBox getPopup() {
    return content.popup;
  }

  Region getArrow() {
    return content.arrow;
  }

  void setOpenState(boolean open) {
    content.setOpenState(open);
  }

  private final class Content extends Region {

    private static final double ARROW_SIZE = 10.0;
    private static final double ARROW_OVERLAP = 1.0;
    private static final double ARROW_RADIUS = ARROW_SIZE / Math.sqrt(2.0);
    private static final double ARROW_EXTENSION = ARROW_RADIUS - ARROW_OVERLAP;
    private static final PseudoClass OPEN = PseudoClass.getPseudoClass("open");
    private static final PseudoClass CLOSED = PseudoClass.getPseudoClass("closed");
    private static final String SIDE_STYLE_PREFIX = "tooltip-side-";

    private final HBox popup = new HBox();
    private final Region arrow = new Region();
    private final ObjectProperty<Align> align = requiredProperty("align", Align.CENTER);
    private final ObjectProperty<Side> side = requiredProperty("side", Side.TOP);
    private final DoubleProperty alignOffset = new SimpleDoubleProperty(Tooltip.this, "alignOffset", 0.0);
    private final DoubleProperty sideOffset = new SimpleDoubleProperty(Tooltip.this, "sideOffset", 4.0);
    private Side resolvedSide = Side.TOP;
    private String appliedSideStyleClass;

    private Content(Node... children) {
      getStyleClass().add("core-tooltip-content");
      popup.getStyleClass().add("core-tooltip-popup");
      popup.getChildren().addAll(children);
      arrow.getStyleClass().add("core-tooltip-arrow");
      arrow.setManaged(false);
      getChildren().addAll(popup, arrow);
      setMouseTransparent(true);
      setResolvedSide(Side.TOP);
      setOpenState(false);

      align.addListener(ignored -> reposition());
      side.addListener(ignored -> reposition());
      alignOffset.addListener(ignored -> reposition());
      sideOffset.addListener(ignored -> reposition());
    }

    private void setOpenState(boolean open) {
      popup.pseudoClassStateChanged(OPEN, open);
      popup.pseudoClassStateChanged(CLOSED, !open);
      arrow.pseudoClassStateChanged(OPEN, open);
      arrow.pseudoClassStateChanged(CLOSED, !open);
    }

    private void setResolvedSide(Side value) {
      resolvedSide = Objects.requireNonNull(value, "resolvedSide");
      if (appliedSideStyleClass != null) {
        getStyleClass().remove(appliedSideStyleClass);
      }
      appliedSideStyleClass = SIDE_STYLE_PREFIX + value.name().toLowerCase(java.util.Locale.ROOT).replace('_', '-');
      getStyleClass().add(appliedSideStyleClass);
      requestLayout();
    }

    @Override
    protected double computeMinWidth(double height) {
      return computePrefWidth(height);
    }

    @Override
    protected double computeMinHeight(double width) {
      return computePrefHeight(width);
    }

    @Override
    protected double computePrefWidth(double height) {
      double popupWidth = popup.prefWidth(-1.0);
      return isVerticalSide() ? Math.max(ARROW_SIZE, popupWidth) : popupWidth + ARROW_EXTENSION;
    }

    @Override
    protected double computePrefHeight(double width) {
      double popupWidth = popup.prefWidth(-1.0);
      double popupHeight = popup.prefHeight(popupWidth);
      return isVerticalSide() ? popupHeight + ARROW_EXTENSION : Math.max(ARROW_SIZE, popupHeight);
    }

    @Override
    protected void layoutChildren() {
      double width = getWidth();
      double height = getHeight();
      double surfaceWidth = isVerticalSide() ? width : Math.max(0.0, width - ARROW_EXTENSION);
      double surfaceHeight = isVerticalSide() ? Math.max(0.0, height - ARROW_EXTENSION) : height;
      double surfaceX = resolvedSide == Side.RIGHT ? ARROW_EXTENSION : 0.0;
      double surfaceY = resolvedSide == Side.BOTTOM ? ARROW_EXTENSION : 0.0;
      popup.resizeRelocate(surfaceX, surfaceY, surfaceWidth, surfaceHeight);

      double crossCenter = isVerticalSide()
        ? popup.getLayoutX() + popup.getWidth() / 2.0
        : popup.getLayoutY() + popup.getHeight() / 2.0;
      if (isVerticalSide()) {
        double arrowX = clamp(crossCenter - ARROW_SIZE / 2.0, 2.0, width - ARROW_SIZE - 2.0);
        double arrowCenterY = resolvedSide == Side.TOP ? surfaceHeight - ARROW_OVERLAP : surfaceY + ARROW_OVERLAP;
        double arrowY = arrowCenterY - ARROW_SIZE / 2.0;
        arrow.resizeRelocate(arrowX, arrowY, ARROW_SIZE, ARROW_SIZE);
      } else {
        double arrowCenterX = resolvedSide == Side.LEFT ? surfaceWidth - ARROW_OVERLAP : surfaceX + ARROW_OVERLAP;
        double arrowX = arrowCenterX - ARROW_SIZE / 2.0;
        double arrowY = clamp(crossCenter - ARROW_SIZE / 2.0, 2.0, height - ARROW_SIZE - 2.0);
        arrow.resizeRelocate(arrowX, arrowY, ARROW_SIZE, ARROW_SIZE);
      }
    }

    private boolean isVerticalSide() {
      return resolvedSide == Side.TOP || resolvedSide == Side.BOTTOM;
    }

    private <T> ObjectProperty<T> requiredProperty(String name, T initialValue) {
      return new ObjectPropertyBase<>(initialValue) {
        @Override
        public void set(T value) {
          super.set(Objects.requireNonNull(value, name));
        }

        @Override
        public Object getBean() {
          return Tooltip.this;
        }

        @Override
        public String getName() {
          return name;
        }
      };
    }
  }

  private record TriggerRegistration(
    Tooltip owner,
    EventHandler<MouseEvent> anchorRecorder,
    ChangeListener<Boolean> focusListener
  ) {}
}
