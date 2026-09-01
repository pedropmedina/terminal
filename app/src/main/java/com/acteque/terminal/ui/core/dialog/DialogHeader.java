package com.acteque.terminal.ui.core.dialog;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Vertical title and description group for a dialog. */
public final class DialogHeader extends VBox {

  public DialogHeader(Node... children) {
    getStyleClass().add("core-dialog-header");
    getChildren().addAll(children);
  }
}
