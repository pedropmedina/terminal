package com.acteque.terminal.ui.drawer;

import javafx.scene.shape.Rectangle;

/** Backdrop used by modal drawers. */
public final class DrawerOverlay extends Rectangle {

  public DrawerOverlay() {
    getStyleClass().add("core-drawer-overlay");
    setManaged(false);
  }
}
