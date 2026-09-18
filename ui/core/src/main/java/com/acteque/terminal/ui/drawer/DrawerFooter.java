package com.acteque.terminal.ui.drawer;

import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Action group at the bottom of a drawer. */
public final class DrawerFooter extends VBox {

  public DrawerFooter(Node... children) {
    getStyleClass().add("core-drawer-footer");
    getChildren().addAll(children);
  }
}
