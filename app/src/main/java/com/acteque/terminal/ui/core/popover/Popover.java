package com.acteque.terminal.ui.core.popover;

import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;

/** A non-modal popup root with anchored positioning, dismissal, and focus restoration. */
public final class Popover {

  private static final double SCREEN_PADDING = 8.0;

  private final Popup popup = new Popup();
  private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
  private final ObjectProperty<PopoverContent> content = new SimpleObjectProperty<>(this, "content") {
    @Override
    protected void invalidated() {
      replaceContent(get());
    }
  };
  private Node anchor;
  private boolean synchronizingPopup;
  private boolean restoreFocusOnHide;

  public Popover() {
    this(null);
  }

  public Popover(PopoverContent content) {
    popup.setAutoFix(false);
    popup.setAutoHide(true);
    popup.setHideOnEscape(true);
    popup.setConsumeAutoHidingEvents(false);
    popup.setOnAutoHide(event -> restoreFocusOnHide = false);
    popup.setOnHidden(event -> popupWasHidden());
    open.addListener((ignored, wasOpen, isOpen) -> applyOpenState(isOpen));
    setContent(content);
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

  public PopoverContent getContent() {
    return content.get();
  }

  public void setContent(PopoverContent value) {
    content.set(value);
  }

  public ObjectProperty<PopoverContent> contentProperty() {
    return content;
  }

  public void show(Node anchor) {
    this.anchor = Objects.requireNonNull(anchor, "anchor");
    if (isOpen()) {
      showPopup();
    } else {
      setOpen(true);
    }
  }

  public void toggle(Node anchor) {
    if (isOpen()) {
      close();
    } else {
      show(anchor);
    }
  }

  public void close() {
    restoreFocusOnHide = true;
    setOpen(false);
  }

  /** Returns the underlying JavaFX popup for window-level integration and testing. */
  public Popup getPopup() {
    return popup;
  }

  void reposition() {
    if (popup.isShowing()) {
      positionPopup();
    }
  }

  private void replaceContent(PopoverContent nextContent) {
    PopoverContent previousContent = popup.getContent().isEmpty()
      ? null
      : (PopoverContent) popup.getContent().getFirst();
    if (previousContent != null) {
      previousContent.setPopover(null);
    }
    popup.getContent().clear();
    if (nextContent != null) {
      nextContent.setPopover(this);
      nextContent.addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
      popup.getContent().add(nextContent);
    }
    if (isOpen()) {
      showPopup();
    }
  }

  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE) {
      close();
      event.consume();
    }
  }

  private void applyOpenState(boolean isOpen) {
    if (synchronizingPopup) {
      return;
    }
    if (isOpen) {
      restoreFocusOnHide = false;
      showPopup();
    } else if (popup.isShowing()) {
      popup.hide();
    }
  }

  private void showPopup() {
    PopoverContent popupContent = getContent();
    Scene anchorScene = anchor == null ? null : anchor.getScene();
    Window owner = anchorScene == null ? null : anchorScene.getWindow();
    if (popupContent == null || owner == null || !owner.isShowing()) {
      return;
    }

    configureStyles(popupContent, anchorScene);
    popupContent.setOpenState(false);
    if (!popup.isShowing()) {
      popup.show(anchor, 0.0, 0.0);
    }
    popupContent.applyCss();
    popupContent.autosize();
    positionPopup();
    Platform.runLater(() -> {
      if (!isOpen() || !popup.isShowing()) {
        return;
      }
      popupContent.setOpenState(true);
      focusFirst(popupContent);
    });
  }

  private void positionPopup() {
    if (anchor == null || getContent() == null) {
      return;
    }
    Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
    if (anchorBounds == null) {
      return;
    }

    PopoverContent popupContent = getContent();
    double width = popupContent.prefWidth(-1.0);
    double height = popupContent.prefHeight(width);
    Rectangle2D screen = screenFor(anchorBounds).getVisualBounds();
    PopoverContent.Side side = physicalSide(popupContent.getSide());
    if (!fits(side, anchorBounds, width, height, popupContent.getSideOffset(), screen)) {
      PopoverContent.Side opposite = opposite(side);
      if (fits(opposite, anchorBounds, width, height, popupContent.getSideOffset(), screen)) {
        side = opposite;
      }
    }

    double x;
    double y;
    if (side == PopoverContent.Side.TOP || side == PopoverContent.Side.BOTTOM) {
      x = alignedStart(anchorBounds.getMinX(), anchorBounds.getWidth(), width, popupContent.getAlign());
      x += popupContent.getAlignOffset();
      y =
        side == PopoverContent.Side.BOTTOM
          ? anchorBounds.getMaxY() + popupContent.getSideOffset()
          : anchorBounds.getMinY() - height - popupContent.getSideOffset();
    } else {
      x =
        side == PopoverContent.Side.RIGHT
          ? anchorBounds.getMaxX() + popupContent.getSideOffset()
          : anchorBounds.getMinX() - width - popupContent.getSideOffset();
      y = alignedStart(anchorBounds.getMinY(), anchorBounds.getHeight(), height, popupContent.getAlign());
      y += popupContent.getAlignOffset();
    }

    popupContent.setResolvedSide(side);
    popup.setX(clamp(x, screen.getMinX() + SCREEN_PADDING, screen.getMaxX() - width - SCREEN_PADDING));
    popup.setY(clamp(y, screen.getMinY() + SCREEN_PADDING, screen.getMaxY() - height - SCREEN_PADDING));
  }

  private PopoverContent.Side physicalSide(PopoverContent.Side side) {
    if (side == PopoverContent.Side.INLINE_START) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
        ? PopoverContent.Side.RIGHT
        : PopoverContent.Side.LEFT;
    }
    if (side == PopoverContent.Side.INLINE_END) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
        ? PopoverContent.Side.LEFT
        : PopoverContent.Side.RIGHT;
    }
    return side;
  }

  private static boolean fits(
    PopoverContent.Side side,
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

  private static PopoverContent.Side opposite(PopoverContent.Side side) {
    return switch (side) {
      case TOP -> PopoverContent.Side.BOTTOM;
      case BOTTOM -> PopoverContent.Side.TOP;
      case LEFT -> PopoverContent.Side.RIGHT;
      case RIGHT -> PopoverContent.Side.LEFT;
      case INLINE_START, INLINE_END -> throw new IllegalArgumentException("Expected a physical side");
    };
  }

  private static double alignedStart(
    double anchorStart,
    double anchorSize,
    double popupSize,
    PopoverContent.Align align
  ) {
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

  private static void configureStyles(PopoverContent popupContent, Scene anchorScene) {
    for (String stylesheet : anchorScene.getStylesheets()) {
      if (!popupContent.getStylesheets().contains(stylesheet)) {
        popupContent.getStylesheets().add(stylesheet);
      }
    }
    popupContent.getStyleClass().removeAll("theme-light", "theme-dark");
    if (anchorScene.getRoot().getStyleClass().contains("theme-dark")) {
      popupContent.getStyleClass().add("theme-dark");
    } else if (anchorScene.getRoot().getStyleClass().contains("theme-light")) {
      popupContent.getStyleClass().add("theme-light");
    }
  }

  private static void focusFirst(Node node) {
    if (node.isFocusTraversable() && node.isVisible() && !node.isDisabled()) {
      node.requestFocus();
      return;
    }
    if (node instanceof javafx.scene.Parent parent) {
      for (Node child : parent.getChildrenUnmodifiable()) {
        if (child.isFocusTraversable() && child.isVisible() && !child.isDisabled()) {
          child.requestFocus();
          return;
        }
      }
    }
  }

  private void popupWasHidden() {
    if (isOpen()) {
      synchronizingPopup = true;
      setOpen(false);
      synchronizingPopup = false;
    }
    if (getContent() != null) {
      getContent().setOpenState(false);
    }
    if (restoreFocusOnHide && anchor != null) {
      Platform.runLater(anchor::requestFocus);
    }
    restoreFocusOnHide = false;
  }
}
