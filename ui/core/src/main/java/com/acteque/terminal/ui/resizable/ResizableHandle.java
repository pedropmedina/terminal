package com.acteque.terminal.ui.resizable;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleAction;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** A focusable divider that resizes the panels on either side of it. */
public final class ResizableHandle extends StackPane {

  private static final PseudoClass HORIZONTAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("horizontal");
  private static final PseudoClass VERTICAL_PSEUDO_CLASS = PseudoClass.getPseudoClass("vertical");

  private final Region line = new Region();
  private final Region grip = new Region();
  private final BooleanProperty withHandle = new SimpleBooleanProperty(this, "withHandle", false) {
    @Override
    protected void invalidated() {
      grip.setManaged(get());
      grip.setVisible(get());
    }
  };
  private final ReadOnlyDoubleWrapper position = new ReadOnlyDoubleWrapper(this, "position", Double.NaN);
  private Orientation resizeOrientation = Orientation.HORIZONTAL;
  private ResizablePanelGroup group;

  public ResizableHandle() {
    this(false);
  }

  public ResizableHandle(boolean withHandle) {
    getStyleClass().add("core-resizable-handle");
    line.getStyleClass().add("resizable-handle-line");
    grip.getStyleClass().add("resizable-handle-grip");
    line.setMouseTransparent(true);
    grip.setMouseTransparent(true);
    grip.setManaged(false);
    grip.setVisible(false);
    getChildren().addAll(line, grip);

    setAccessibleRole(AccessibleRole.SLIDER);
    setAccessibleText("Resize panels");
    setFocusTraversable(true);
    setPickOnBounds(true);
    setViewOrder(-1.0);
    setWithHandle(withHandle);
    applyOrientation(Orientation.HORIZONTAL);

    addEventHandler(MouseEvent.MOUSE_PRESSED, this::handleMousePressed);
    addEventHandler(MouseEvent.MOUSE_DRAGGED, this::handleMouseDragged);
    addEventHandler(MouseEvent.MOUSE_RELEASED, this::handleMouseReleased);
    addEventHandler(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
  }

  public final BooleanProperty withHandleProperty() {
    return withHandle;
  }

  public final boolean isWithHandle() {
    return withHandle.get();
  }

  public final void setWithHandle(boolean value) {
    withHandle.set(value);
  }

  /** Effective divider position in the containing group, normalized to {@code 0..1}. */
  public final ReadOnlyDoubleProperty positionProperty() {
    return position.getReadOnlyProperty();
  }

  public final double getPosition() {
    return position.get();
  }

  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (group != null) {
      return switch (attribute) {
        case VALUE -> getPosition() * 100.0;
        case MIN_VALUE -> group.minimumPosition(this) * 100.0;
        case MAX_VALUE -> group.maximumPosition(this) * 100.0;
        case ORIENTATION -> resizeOrientation;
        default -> super.queryAccessibleAttribute(attribute, parameters);
      };
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  @Override
  public void executeAccessibleAction(AccessibleAction action, Object... parameters) {
    if (group == null || isDisabled()) {
      super.executeAccessibleAction(action, parameters);
      return;
    }
    switch (action) {
      case INCREMENT -> group.resizeByKeyboard(this, ResizablePanelGroup.KEYBOARD_INCREMENT);
      case DECREMENT -> group.resizeByKeyboard(this, -ResizablePanelGroup.KEYBOARD_INCREMENT);
      case SET_VALUE -> {
        if (parameters.length > 0 && parameters[0] instanceof Number value) {
          group.setHandlePosition(this, value.doubleValue() / 100.0);
        }
      }
      default -> super.executeAccessibleAction(action, parameters);
    }
  }

  void attach(ResizablePanelGroup value) {
    group = value;
  }

  void applyOrientation(Orientation value) {
    resizeOrientation = value;
    boolean horizontal = value == Orientation.HORIZONTAL;
    pseudoClassStateChanged(HORIZONTAL_PSEUDO_CLASS, horizontal);
    pseudoClassStateChanged(VERTICAL_PSEUDO_CLASS, !horizontal);
    notifyAccessibleAttributeChanged(AccessibleAttribute.ORIENTATION);
  }

  void updatePosition(double value) {
    if (Double.compare(position.get(), value) == 0) {
      return;
    }
    position.set(value);
    notifyAccessibleAttributeChanged(AccessibleAttribute.VALUE);
  }

  private void handleMousePressed(MouseEvent event) {
    if (group != null && group.beginDrag(this, event)) {
      requestFocus();
      event.consume();
    }
  }

  private void handleMouseDragged(MouseEvent event) {
    if (group != null && group.drag(this, event)) {
      event.consume();
    }
  }

  private void handleMouseReleased(MouseEvent event) {
    if (group != null && group.endDrag(this)) {
      event.consume();
    }
  }

  private void handleKeyPressed(KeyEvent event) {
    if (group != null && group.handleKey(this, event.getCode())) {
      event.consume();
    }
  }
}
