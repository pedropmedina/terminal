package com.acteque.terminal.ui.core.field;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Flexing label, description, and error content used in horizontal fields. */
public final class FieldContent extends VBox {

  public FieldContent(Node... children) {
    getStyleClass().add("core-field-content");
    getChildren().addAll(children);
  }
}
