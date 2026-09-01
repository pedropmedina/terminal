package com.acteque.terminal.ui.core.field;

import javafx.scene.Node;

/** Label associated with the field's input control. */
public final class FieldLabel extends com.acteque.terminal.ui.core.Label {

  public FieldLabel() {
    this(null, null);
  }

  public FieldLabel(String text) {
    this(text, null);
  }

  public FieldLabel(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add("core-field-label");
    setWrapText(true);
  }
}
