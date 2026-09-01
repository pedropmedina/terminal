package com.acteque.terminal.ui.core.dialog;

import javafx.scene.layout.Region;

/** Full-size modal backdrop used by {@link Dialog}. */
public final class DialogOverlay extends Region {

  public DialogOverlay() {
    getStyleClass().add("core-dialog-overlay");
  }
}
