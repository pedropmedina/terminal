package com.acteque.terminal.ui.core.dialog;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;

/** Primary accessible heading for dialog content. */
public final class DialogTitle extends javafx.scene.control.Label {

  public DialogTitle() {
    this(null, null);
  }

  public DialogTitle(String text) {
    this(text, null);
  }

  public DialogTitle(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add("core-dialog-title");
    setAccessibleRole(AccessibleRole.TEXT);
    setWrapText(true);
  }
}
