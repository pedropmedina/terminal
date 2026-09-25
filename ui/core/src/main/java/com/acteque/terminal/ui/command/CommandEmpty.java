package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.Label;
import java.util.Objects;
import javafx.css.PseudoClass;

/** Message displayed when command filtering produces no matching items. */
public final class CommandEmpty extends Label {

  private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");

  private final Command command;

  /**
   * Creates an empty-state message.
   *
   * @param command the owning command
   * @param text the empty-state text
   */
  public CommandEmpty(Command command, String text) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    setText(text);
    getStyleClass().add("core-command-empty");
    setManaged(false);
    setVisible(false);
    this.command.registerEmptyState(this);
  }

  /** Removes this empty state from the command registry. */
  public void dispose() {
    command.unregisterEmptyState(this);
  }

  /**
   * Updates empty-state visibility.
   *
   * @param value {@code true} when the command has no matching items
   */
  void setEmpty(boolean value) {
    pseudoClassStateChanged(EMPTY, value);
    setManaged(value);
    setVisible(value);
  }
}
