package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.inputgroup.InputGroupButton;
import java.util.Objects;

/** Compact button that toggles a combobox popup. */
public final class ComboboxTrigger extends InputGroupButton {

  /** Creates a trigger for the supplied root. */
  public ComboboxTrigger(Combobox<?> combobox) {
    super(new LucideIcon(LucideIcons.CHEVRON_DOWN));
    Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-trigger");
    setAccessibleText("Open options");
    setOnAction(ignored -> combobox.toggle());
  }
}
