package com.acteque.terminal.ui.drawer;

import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;

/** Primary accessible heading for drawer content. */
public final class DrawerTitle extends Label {

  public DrawerTitle(String text) {
    super(text);
    getStyleClass().add("core-drawer-title");
    setAccessibleRole(AccessibleRole.TEXT);
    setWrapText(true);
  }
}
