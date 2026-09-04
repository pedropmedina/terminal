package com.acteque.terminal.ui.core.inputgroup;

import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ButtonBase;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;

/** Supporting content positioned around an input-group control. */
public final class InputGroupAddon extends HBox {

  private static final String ROOT_STYLE_CLASS = "core-input-group-addon";

  private String appliedAlignmentStyleClass;

  private final ObjectProperty<InputGroupAlignment> alignment = new ObjectPropertyBase<>(
    InputGroupAlignment.INLINE_START
  ) {
    @Override
    public void set(InputGroupAlignment value) {
      super.set(Objects.requireNonNull(value, "alignment"));
    }

    @Override
    protected void invalidated() {
      applyAlignment(Objects.requireNonNull(get(), "alignment"));
    }

    @Override
    public Object getBean() {
      return InputGroupAddon.this;
    }

    @Override
    public String getName() {
      return "alignment";
    }
  };

  public InputGroupAddon() {
    this(InputGroupAlignment.INLINE_START);
  }

  public InputGroupAddon(Node... children) {
    this(InputGroupAlignment.INLINE_START, children);
  }

  public InputGroupAddon(InputGroupAlignment alignment, Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setAccessibleRole(AccessibleRole.PARENT);
    applyAlignment(InputGroupAlignment.INLINE_START);
    setAlignmentPosition(alignment);
    getChildren().addAll(children);
    addEventHandler(MouseEvent.MOUSE_CLICKED, this::focusControlUnlessButtonWasClicked);
  }

  public final ObjectProperty<InputGroupAlignment> alignmentPositionProperty() {
    return alignment;
  }

  public final InputGroupAlignment getAlignmentPosition() {
    return alignment.get();
  }

  public final void setAlignmentPosition(InputGroupAlignment value) {
    alignment.set(Objects.requireNonNull(value, "alignment"));
  }

  private void applyAlignment(InputGroupAlignment selectedAlignment) {
    if (appliedAlignmentStyleClass != null) {
      getStyleClass().remove(appliedAlignmentStyleClass);
    }
    appliedAlignmentStyleClass = selectedAlignment.styleClass;
    if (!getStyleClass().contains(appliedAlignmentStyleClass)) {
      getStyleClass().add(appliedAlignmentStyleClass);
    }
  }

  private void focusControlUnlessButtonWasClicked(MouseEvent event) {
    Node target = event.getPickResult().getIntersectedNode();
    for (Node current = target; current != null && current != this; current = current.getParent()) {
      if (current instanceof ButtonBase) {
        return;
      }
    }

    Parent current = getParent();
    while (current != null && !(current instanceof InputGroup)) {
      current = current.getParent();
    }
    if (current instanceof InputGroup group) {
      group.requestControlFocus();
    }
  }
}
