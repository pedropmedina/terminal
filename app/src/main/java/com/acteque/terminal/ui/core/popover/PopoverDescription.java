package com.acteque.terminal.ui.core.popover;

/** Supporting text for a popover. */
public final class PopoverDescription extends javafx.scene.control.Label {

  public PopoverDescription() {
    this(null);
  }

  public PopoverDescription(String text) {
    super(text);
    getStyleClass().add("core-popover-description");
    setWrapText(true);
  }
}
