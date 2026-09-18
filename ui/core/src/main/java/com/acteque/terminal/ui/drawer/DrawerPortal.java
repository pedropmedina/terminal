package com.acteque.terminal.ui.drawer;

import javafx.scene.layout.StackPane;

/** Full-host layer for the drawer overlay, popup, and nested drawers. */
public final class DrawerPortal extends StackPane {

  public DrawerPortal() {
    getStyleClass().add("core-drawer-portal");
    setPickOnBounds(false);
  }
}
