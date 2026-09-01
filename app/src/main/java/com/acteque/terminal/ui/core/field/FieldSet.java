package com.acteque.terminal.ui.core.field;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Semantic container for a related set of fields. */
public final class FieldSet extends VBox {

  public FieldSet(Node... children) {
    getStyleClass().add("core-field-set");
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addAll(children);
  }
}
