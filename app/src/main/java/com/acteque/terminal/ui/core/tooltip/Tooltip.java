package com.acteque.terminal.ui.core.tooltip;

import java.util.Objects;
import javafx.animation.PauseTransition;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Popup;
import javafx.stage.PopupWindow;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

/** A composed tooltip containing an explicit trigger and popup content. */
public final class Tooltip extends StackPane {

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

  private static final Duration DEFAULT_SHOW_DELAY = Duration.millis(200.0);
  private static final double SCREEN_PADDING = 8.0;
  private final PauseTransition hoverDelay = new PauseTransition();
  private final PauseTransition focusDelay = new PauseTransition();
  private final TooltipTrigger trigger;
  private final TooltipContent tooltipContent;
  private final Popup popupWindow = new Popup();
  private final TooltipPopup popupContent;
  private final ObjectProperty<Align> align = requiredProperty("align", Align.CENTER);
  private final ObjectProperty<Side> side = requiredProperty("side", Side.TOP);
  private final DoubleProperty alignOffset = new SimpleDoubleProperty(this, "alignOffset", 0.0);
  private final DoubleProperty sideOffset = new SimpleDoubleProperty(this, "sideOffset", 4.0);
  private final ChangeListener<Boolean> disabledListener = this::disabledChanged;
  private final ChangeListener<Boolean> focusVisibleListener = this::focusVisibleChanged;
  private Duration showDelay = DEFAULT_SHOW_DELAY;
  private final Duration showDuration = Duration.INDEFINITE;
  private boolean pointerInside;

  public Tooltip(TooltipTrigger trigger, TooltipContent content) {
    this.trigger = Objects.requireNonNull(trigger, "trigger cannot be null");
    tooltipContent = Objects.requireNonNull(content, "content cannot be null");
    popupContent = new TooltipPopup(tooltipContent);
    getStyleClass().add("core-tooltip");
    getChildren().setAll(trigger);
    setPickOnBounds(false);

    popupWindow.setAutoFix(false);
    popupWindow.setAutoHide(false);
    popupWindow.setAnchorLocation(PopupWindow.AnchorLocation.WINDOW_TOP_LEFT);
    popupWindow.addEventHandler(WindowEvent.WINDOW_HIDING, event -> popupContent.setOpen(false));
    popupWindow.getContent().setAll(popupContent);

    addEventHandler(MouseEvent.MOUSE_ENTERED, event -> {
      pointerInside = true;
      showFromHover();
    });
    addEventHandler(MouseEvent.MOUSE_EXITED, event -> {
      pointerInside = false;
      dismiss();
    });
    trigger.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> dismiss());
    trigger.targetProperty().addListener((ignored, oldTarget, newTarget) -> dismiss());
    trigger.targetDisabledProperty().addListener(disabledListener);
    trigger.targetFocusVisibleProperty().addListener(focusVisibleListener);
    align.addListener(ignored -> reposition());
    side.addListener(ignored -> reposition());
    alignOffset.addListener(ignored -> reposition());
    sideOffset.addListener(ignored -> reposition());
    hoverDelay.setOnFinished(event -> showForHoveredTrigger());
    focusDelay.setOnFinished(event -> showForFocusedAnchor());
  }

  public ObservableList<Node> getContentNodes() {
    return tooltipContent.getChildren();
  }

  public TooltipTrigger getTrigger() {
    return trigger;
  }

  public TooltipContent getContent() {
    return tooltipContent;
  }

  public Align getAlign() {
    return align.get();
  }

  public void setAlign(Align value) {
    align.set(value);
  }

  public ObjectProperty<Align> alignProperty() {
    return align;
  }

  public Side getSide() {
    return side.get();
  }

  public void setSide(Side value) {
    side.set(value);
  }

  public ObjectProperty<Side> sideProperty() {
    return side;
  }

  public double getAlignOffset() {
    return alignOffset.get();
  }

  public void setAlignOffset(double value) {
    alignOffset.set(value);
  }

  public DoubleProperty alignOffsetProperty() {
    return alignOffset;
  }

  public double getSideOffset() {
    return sideOffset.get();
  }

  public void setSideOffset(double value) {
    sideOffset.set(value);
  }

  public DoubleProperty sideOffsetProperty() {
    return sideOffset;
  }

  /** Cancels a pending keyboard-focus display and hides this tooltip immediately. */
  public void dismiss() {
    hoverDelay.stop();
    focusDelay.stop();
    popupWindow.hide();
  }

  public Duration getShowDelay() {
    return showDelay;
  }

  public void setShowDelay(Duration value) {
    showDelay = Objects.requireNonNull(value, "showDelay cannot be null");
  }

  public Duration getShowDuration() {
    return showDuration;
  }

  public boolean isShowing() {
    return popupWindow.isShowing();
  }

  public void show() {
    applyAnchorTheme();
    Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
    if (bounds == null) {
      return;
    }
    popupContent.setOpen(true);
    if (!popupWindow.isShowing()) {
      Rectangle2D screen = screenFor(bounds).getVisualBounds();
      popupWindow.show(trigger, screen.getMaxX() + SCREEN_PADDING, screen.getMaxY() + SCREEN_PADDING);
    }
    popupWindow.getScene().getRoot().applyCss();
    popupContent.applyCss();
    popupContent.autosize();
    popupWindow.sizeToScene();
    positionPopup();
  }

  void hide() {
    popupWindow.hide();
  }

  void showFromFocus() {
    applyAnchorTheme();
    focusDelay.stop();
    focusDelay.setDuration(getShowDelay());
    focusDelay.playFromStart();
  }

  private void showFromHover() {
    applyAnchorTheme();
    hoverDelay.stop();
    hoverDelay.setDuration(getShowDelay());
    hoverDelay.playFromStart();
  }

  void hideFromFocus() {
    dismiss();
  }

  void reposition() {
    if (!isShowing()) {
      return;
    }
    positionPopup();
  }

  private void positionPopup() {
    Bounds anchorBounds = trigger.localToScreen(trigger.getBoundsInLocal());
    if (anchorBounds == null) {
      return;
    }

    Side side = physicalSide(getSide());
    popupContent.setResolvedSide(side);
    popupContent.applyCss();
    popupContent.autosize();
    popupWindow.sizeToScene();
    double width = popupWindow.getWidth();
    double height = popupWindow.getHeight();
    Rectangle2D screen = screenFor(anchorBounds).getVisualBounds();
    if (!fits(side, anchorBounds, width, height, getSideOffset(), screen)) {
      Side opposite = opposite(side);
      if (fits(opposite, anchorBounds, width, height, getSideOffset(), screen)) {
        side = opposite;
        popupContent.setResolvedSide(side);
        popupContent.autosize();
        popupWindow.sizeToScene();
        width = popupWindow.getWidth();
        height = popupWindow.getHeight();
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
    popupWindow.setAnchorX(x);
    popupWindow.setAnchorY(y);
  }

  private void showForFocusedAnchor() {
    Scene scene = trigger.getScene();
    Window owner = scene == null ? null : scene.getWindow();
    Bounds bounds = trigger.localToScreen(trigger.getBoundsInLocal());
    if (owner != null && owner.isShowing() && bounds != null && trigger.getTarget().isFocused()) {
      show();
    }
  }

  private void showForHoveredTrigger() {
    Scene scene = trigger.getScene();
    Window owner = scene == null ? null : scene.getWindow();
    if (owner != null && owner.isShowing() && pointerInside && !trigger.isTargetDisabled()) {
      show();
    }
  }

  private Side physicalSide(Side side) {
    if (side == Side.INLINE_START) {
      return trigger.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT ? Side.RIGHT : Side.LEFT;
    }
    if (side == Side.INLINE_END) {
      return trigger.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT ? Side.LEFT : Side.RIGHT;
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

  private void applyAnchorTheme() {
    Scene scene = trigger.getScene();
    Parent root = scene == null ? null : scene.getRoot();
    popupContent.getStylesheets().setAll(scene == null ? java.util.List.of() : scene.getStylesheets());
    popupContent.getStyleClass().removeAll("theme-light", "theme-dark");
    if (root != null && root.getStyleClass().contains("theme-dark")) {
      popupContent.getStyleClass().add("theme-dark");
    } else if (root != null && root.getStyleClass().contains("theme-light")) {
      popupContent.getStyleClass().add("theme-light");
    }
  }

  TooltipContent getPopup() {
    return tooltipContent;
  }

  Region getArrow() {
    return popupContent.getArrow();
  }

  void setOpenState(boolean open) {
    popupContent.setOpen(open);
  }

  Region getGraphic() {
    return popupContent;
  }

  private void disabledChanged(
    javafx.beans.value.ObservableValue<? extends Boolean> ignored,
    Boolean wasDisabled,
    Boolean isDisabled
  ) {
    if (isDisabled) {
      dismiss();
    }
  }

  private void focusVisibleChanged(
    javafx.beans.value.ObservableValue<? extends Boolean> ignored,
    Boolean wasFocusVisible,
    Boolean isFocusVisible
  ) {
    if (isFocusVisible) {
      showFromFocus();
    } else if (!pointerInside) {
      hideFromFocus();
    }
  }

  private <T> ObjectProperty<T> requiredProperty(String name, T initialValue) {
    return new ObjectPropertyBase<>(initialValue) {
      @Override
      public void set(T value) {
        super.set(Objects.requireNonNull(value, name + " cannot be null"));
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
