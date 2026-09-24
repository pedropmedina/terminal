package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.Label;

/** Muted label displayed above a combobox group. */
public final class ComboboxLabel extends Label {

  /** Creates an empty group label. */
  public ComboboxLabel() {
    this("");
  }

  /** Creates a group label with the supplied text. */
  public ComboboxLabel(String text) {
    super(text);
    getStyleClass().add("core-combobox-label");
  }
}
