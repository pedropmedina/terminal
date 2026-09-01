package com.acteque.terminal.ui.core.dialog;

import javafx.scene.shape.Rectangle;

/** Full-size modal backdrop used by {@link Dialog}. */
public final class DialogOverlay extends Rectangle {

  public DialogOverlay() {
    getStyleClass().add("core-dialog-overlay");
    setManaged(false);
  }
}
