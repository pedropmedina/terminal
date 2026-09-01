package com.acteque.terminal.ui.core.popover;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Vertical title and description group for popover content. */
public final class PopoverHeader extends VBox {

  public PopoverHeader(Node... children) {
    getStyleClass().add("core-popover-header");
    getChildren().addAll(children);
  }
}
