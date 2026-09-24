package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.Label;
import java.util.Objects;
import javafx.css.PseudoClass;

/** Message displayed when filtering produces no visible options. */
public final class ComboboxEmpty extends Label {

  private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");

  /** Creates an empty-state message. */
  public ComboboxEmpty(Combobox<?> combobox, String text) {
    Objects.requireNonNull(combobox, "combobox cannot be null");
    setText(text);
    getStyleClass().add("core-combobox-empty");
    setManaged(false);
    setVisible(false);
  }

  /** Updates visibility and empty presentation state. */
  void setEmpty(boolean value) {
    pseudoClassStateChanged(EMPTY, value);
    setManaged(value);
    setVisible(value);
  }
}
