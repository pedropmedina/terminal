package com.acteque.terminal.ui.drawer;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Title and description group for a drawer. */
public final class DrawerHeader extends VBox {

  public DrawerHeader(Node... children) {
    getStyleClass().add("core-drawer-header");
    getChildren().addAll(children);
  }
}
