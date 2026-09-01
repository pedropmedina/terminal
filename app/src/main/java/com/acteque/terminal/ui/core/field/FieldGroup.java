package com.acteque.terminal.ui.core.field;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Vertical group of fields and optional separators. */
public final class FieldGroup extends VBox {

  public FieldGroup(Node... children) {
    getStyleClass().add("core-field-group");
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addAll(children);
  }
}
