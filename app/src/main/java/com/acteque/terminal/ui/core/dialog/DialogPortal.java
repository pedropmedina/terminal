package com.acteque.terminal.ui.core.dialog;

import javafx.scene.layout.StackPane;

/** Scene-graph layer containing a dialog backdrop and popup. */
public final class DialogPortal extends StackPane {

  public DialogPortal() {
    getStyleClass().add("core-dialog-portal");
  }
}
