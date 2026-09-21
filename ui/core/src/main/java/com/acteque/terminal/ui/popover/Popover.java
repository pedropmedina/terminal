package com.acteque.terminal.ui.popover;

import java.util.Objects;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.css.TransitionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;

/** A non-modal popup root with anchored positioning, dismissal, and focus restoration. */
public final class Popover {

  private static final double SCREEN_PADDING = 8.0;
  private static final String OPACITY_PROPERTY = "-fx-opacity";

  private final Popup popup = new Popup();
  private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
  private final ObjectProperty<PopoverContent> content = new SimpleObjectProperty<>(this, "content") {
    @Override
    protected void invalidated() {
      replaceContent(get());
    }
  };
  private final ChangeListener<Boolean> focusListener = this::popupFocusChanged;
  private final EventHandler<KeyEvent> keyPressedHandler = this::handleKeyPressed;
  private final EventHandler<MouseEvent> ownerMousePressedHandler = this::handleOwnerMousePressed;
  private final EventHandler<ScrollEvent> ownerScrollHandler = this::handleOwnerScroll;
  private final EventHandler<TransitionEvent> transitionFinishedHandler = this::transitionFinished;
  private final EventHandler<TransitionEvent> transitionStartedHandler = this::transitionStarted;
  private Node anchor;
  private Window owner;
  private AnimationTimer openingTimer;
  private PopoverContent.Phase phase = PopoverContent.Phase.CLOSED;
  private long lifecycleRevision;
  private boolean closingOpacityTransition;
  private boolean dismissalFiltersAttached;
  private boolean restoreFocusOnHide;
  private boolean synchronizingPopup;

  public Popover() {
    this(null);
  }

  public Popover(PopoverContent content) {
    popup.setAutoFix(false);
    popup.setAutoHide(false);
    popup.setHideOnEscape(false);
    popup.setConsumeAutoHidingEvents(false);
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
    this.anchor = Objects.requireNonNull(anchor, "anchor cannot be null");
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
      detachContentFilters(previousContent);
      previousContent.setPopover(null);
      previousContent.setPhase(PopoverContent.Phase.CLOSED);
    }

    popup.getContent().clear();

    if (nextContent != null) {
      nextContent.setPopover(this);
      nextContent.setPhase(PopoverContent.Phase.CLOSED);
      popup.getContent().add(nextContent);
      if (dismissalFiltersAttached) {
        attachContentFilters(nextContent);
      }
    }

    if (isOpen()) {
      showPopup();
    } else if (popup.isShowing()) {
      beginClosing();
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
      beginClosing();
    } else {
      cancelOpeningTimer();
      lifecycleRevision++;
      setPhase(PopoverContent.Phase.CLOSED);
      restoreFocusOnHide = false;
    }
  }

  private void showPopup() {
    PopoverContent popupContent = getContent();
    Scene anchorScene = anchor == null ? null : anchor.getScene();
    Window nextOwner = anchorScene == null ? null : anchorScene.getWindow();
    if (popupContent == null || nextOwner == null || !nextOwner.isShowing()) {
      return;
    }

    configureStyles(popupContent, anchorScene);
    if (popup.isShowing()) {
      lifecycleRevision++;
      cancelOpeningTimer();
      setPhase(PopoverContent.Phase.OPEN);
      popupContent.applyCss();
      popupContent.autosize();
      positionPopup();
      focusFirst(popupContent);
      return;
    }

    owner = nextOwner;
    setPhase(PopoverContent.Phase.OPENING);
    popupContent.applyCss();
    popupContent.autosize();
    PopupPosition position = popupPosition();
    if (position == null) {
      return;
    }
    popupContent.applyCss();

    long revision = ++lifecycleRevision;
    popup.show(owner, position.x(), position.y());
    attachDismissalFilters();
    startOpeningTimer(popupContent, revision);
  }

  private void startOpeningTimer(PopoverContent popupContent, long revision) {
    cancelOpeningTimer();
    openingTimer = new AnimationTimer() {
      private boolean renderedOpeningFrame;

      @Override
      public void handle(long now) {
        if (revision != lifecycleRevision || !isOpen() || !popup.isShowing() || getContent() != popupContent) {
          stop();
          if (openingTimer == this) {
            openingTimer = null;
          }
          return;
        }
        if (!renderedOpeningFrame) {
          renderedOpeningFrame = true;
          return;
        }

        stop();
        if (openingTimer == this) {
          openingTimer = null;
        }
        setPhase(PopoverContent.Phase.OPEN);
        popupContent.applyCss();
        focusFirst(popupContent);
      }
    };
    openingTimer.start();
  }

  private void cancelOpeningTimer() {
    if (openingTimer != null) {
      openingTimer.stop();
      openingTimer = null;
    }
  }

  private void beginClosing() {
    PopoverContent popupContent = getContent();
    long revision = ++lifecycleRevision;
    cancelOpeningTimer();
    setPhase(PopoverContent.Phase.CLOSING);
    if (popupContent == null || !popup.isShowing()) {
      finishClosing(revision);
      return;
    }

    closingOpacityTransition = false;
    popupContent.applyCss();
    if (!closingOpacityTransition) {
      finishClosing(revision);
    }
  }

  private void transitionStarted(TransitionEvent event) {
    if (phase == PopoverContent.Phase.CLOSING && OPACITY_PROPERTY.equals(event.getPropertyName())) {
      closingOpacityTransition = true;
    }
  }

  private void transitionFinished(TransitionEvent event) {
    if (phase == PopoverContent.Phase.CLOSING && OPACITY_PROPERTY.equals(event.getPropertyName())) {
      finishClosing(lifecycleRevision);
    }
  }

  private void finishClosing(long revision) {
    if (revision != lifecycleRevision || phase != PopoverContent.Phase.CLOSING) {
      return;
    }
    detachDismissalFilters();
    setPhase(PopoverContent.Phase.CLOSED);
    if (popup.isShowing()) {
      popup.hide();
    } else {
      popupWasHidden();
    }
  }

  private void attachDismissalFilters() {
    if (dismissalFiltersAttached || owner == null) {
      return;
    }
    dismissalFiltersAttached = true;
    owner.addEventFilter(MouseEvent.MOUSE_PRESSED, ownerMousePressedHandler);
    owner.addEventFilter(ScrollEvent.SCROLL, ownerScrollHandler);
    owner.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
    popup.focusedProperty().addListener(focusListener);
    if (getContent() != null) {
      attachContentFilters(getContent());
    }
  }

  private void detachDismissalFilters() {
    if (!dismissalFiltersAttached) {
      return;
    }
    dismissalFiltersAttached = false;
    if (owner != null) {
      owner.removeEventFilter(MouseEvent.MOUSE_PRESSED, ownerMousePressedHandler);
      owner.removeEventFilter(ScrollEvent.SCROLL, ownerScrollHandler);
      owner.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
    }
    popup.focusedProperty().removeListener(focusListener);
    if (getContent() != null) {
      detachContentFilters(getContent());
    }
  }

  private void attachContentFilters(PopoverContent popupContent) {
    popupContent.addEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
    popupContent.addEventHandler(TransitionEvent.RUN, transitionStartedHandler);
    popupContent.addEventHandler(TransitionEvent.END, transitionFinishedHandler);
  }

  private void detachContentFilters(PopoverContent popupContent) {
    popupContent.removeEventFilter(KeyEvent.KEY_PRESSED, keyPressedHandler);
    popupContent.removeEventHandler(TransitionEvent.RUN, transitionStartedHandler);
    popupContent.removeEventHandler(TransitionEvent.END, transitionFinishedHandler);
  }

  private void handleOwnerMousePressed(MouseEvent event) {
    if (!isInAnchor(event.getTarget())) {
      requestDismissal(false);
    }
  }

  private void handleOwnerScroll(ScrollEvent event) {
    requestDismissal(false);
  }

  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE && isOpen()) {
      requestDismissal(true);
      event.consume();
    }
  }

  private void popupFocusChanged(ObservableValue<? extends Boolean> ignored, Boolean wasFocused, Boolean isFocused) {
    if (wasFocused && !isFocused && isOpen()) {
      requestDismissal(false);
    }
  }

  private void requestDismissal(boolean restoreFocus) {
    if (!isOpen()) {
      return;
    }
    restoreFocusOnHide = restoreFocus;
    setOpen(false);
  }

  private boolean isInAnchor(Object eventTarget) {
    if (!(eventTarget instanceof Node node) || anchor == null) {
      return false;
    }
    for (Node current = node; current != null; current = current.getParent()) {
      if (current == anchor) {
        return true;
      }
    }
    return false;
  }

  private void setPhase(PopoverContent.Phase nextPhase) {
    phase = nextPhase;
    if (getContent() != null) {
      getContent().setPhase(nextPhase);
    }
  }

  private void positionPopup() {
    PopupPosition position = popupPosition();
    if (position == null) {
      return;
    }
    popup.setX(position.x());
    popup.setY(position.y());
  }

  private PopupPosition popupPosition() {
    if (anchor == null || getContent() == null) {
      return null;
    }
    Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
    if (anchorBounds == null) {
      return null;
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
    return new PopupPosition(
      clamp(x, screen.getMinX() + SCREEN_PADDING, screen.getMaxX() - width - SCREEN_PADDING),
      clamp(y, screen.getMinY() + SCREEN_PADDING, screen.getMaxY() - height - SCREEN_PADDING)
    );
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
    cancelOpeningTimer();
    detachDismissalFilters();
    long revision = ++lifecycleRevision;
    if (isOpen()) {
      synchronizingPopup = true;
      setOpen(false);
      synchronizingPopup = false;
    }
    setPhase(PopoverContent.Phase.CLOSED);
    if (restoreFocusOnHide && anchor != null) {
      Node focusTarget = anchor;
      Platform.runLater(() -> {
        if (revision == lifecycleRevision && !isOpen()) {
          focusTarget.requestFocus();
        }
      });
    }
    restoreFocusOnHide = false;
    owner = null;
  }

  private record PopupPosition(double x, double y) {}
}
