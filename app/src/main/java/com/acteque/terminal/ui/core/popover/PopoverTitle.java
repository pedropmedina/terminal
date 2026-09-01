package com.acteque.terminal.ui.core.popover;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;

/** Primary accessible heading for popover content. */
public final class PopoverTitle extends javafx.scene.control.Label {

  public PopoverTitle() {
    this(null, null);
  }

  public PopoverTitle(String text) {
    this(text, null);
  }

  public PopoverTitle(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add("core-popover-title");
    setAccessibleRole(AccessibleRole.TEXT);
    setWrapText(true);
  }
}
