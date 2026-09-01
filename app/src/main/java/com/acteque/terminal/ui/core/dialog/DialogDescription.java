package com.acteque.terminal.ui.core.dialog;

/** Supporting text for a dialog. */
public final class DialogDescription extends javafx.scene.control.Label {

  public DialogDescription() {
    this(null);
  }

  public DialogDescription(String text) {
    super(text);
    getStyleClass().add("core-dialog-description");
    setWrapText(true);
  }
}
