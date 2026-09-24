package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.Input;
import java.util.Objects;
import javafx.scene.Node;

/** Flexible text editor displayed within a {@link ComboboxChips} container. */
public final class ComboboxChipsInput<T> extends Input {

  /** Creates a chips editor and registers the supplied anchor. */
  public ComboboxChipsInput(Combobox<T> combobox, Node anchor) {
    Objects.requireNonNull(combobox, "combobox cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");
    getStyleClass().add("core-combobox-chips-input");
    combobox.registerEditor(this, anchor);
  }
}
