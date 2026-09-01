package com.acteque.terminal.ui.core.field;

/** Supporting text for a field or field set. */
public final class FieldDescription extends javafx.scene.control.Label {

  public FieldDescription() {
    this(null);
  }

  public FieldDescription(String text) {
    super(text);
    getStyleClass().add("core-field-description");
    setWrapText(true);
  }
}
