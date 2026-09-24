package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.inputgroup.InputGroupButton;
import java.util.Objects;

/** Compact button that clears combobox selection and input state. */
public final class ComboboxClear extends InputGroupButton {

  /** Creates a clear button for the supplied root. */
  public ComboboxClear(Combobox<?> combobox) {
    super(new LucideIcon(LucideIcons.X));
    Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-clear");
    setAccessibleText("Clear selection");
    setOnAction(ignored -> combobox.clear());
  }
}
