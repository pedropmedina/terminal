package com.acteque.terminal.ui.core.field;

import javafx.scene.Node;

/** Non-interactive field heading used when a label association is not appropriate. */
public final class FieldTitle extends javafx.scene.control.Label {

  public FieldTitle() {
    this(null, null);
  }

  public FieldTitle(String text) {
    this(text, null);
  }

  public FieldTitle(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add("core-field-title");
    setWrapText(true);
  }
}
