package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.Label;

/** Muted trailing keyboard-shortcut text for a command item. */
public final class CommandShortcut extends Label {

  /** Creates an empty shortcut label. */
  public CommandShortcut() {
    this("");
  }

  /**
   * Creates a shortcut label.
   *
   * @param text the displayed shortcut
   */
  public CommandShortcut(String text) {
    super(text);
    getStyleClass().add("core-command-shortcut");
  }
}
