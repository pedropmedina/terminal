package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.Separator;
import java.util.Objects;

/** Horizontal divider between command sections. */
public final class CommandSeparator extends Separator {

  private final Command command;

  /**
   * Creates a command separator that hides while a search query is active.
   *
   * @param command the owning command
   */
  public CommandSeparator(Command command) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    getStyleClass().add("core-command-separator");
    this.command.registerSeparator(this);
  }

  /** Removes this separator from search-state updates. */
  public void dispose() {
    command.unregisterSeparator(this);
  }

  /**
   * Hides the separator while filtering avoids orphaned visual divisions.
   *
   * @param value {@code true} while a non-empty query is active
   */
  void setFiltering(boolean value) {
    setVisible(!value);
    setManaged(!value);
  }
}
