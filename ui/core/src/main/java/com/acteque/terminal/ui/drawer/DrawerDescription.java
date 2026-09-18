package com.acteque.terminal.ui.drawer;

import javafx.scene.control.Label;

/** Supporting text for a drawer. */
public final class DrawerDescription extends Label {

  public DrawerDescription(String text) {
    super(text);
    getStyleClass().add("core-drawer-description");
    setWrapText(true);
  }
}
