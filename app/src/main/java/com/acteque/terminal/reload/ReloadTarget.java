package com.acteque.terminal.reload;

/** A target that refreshes visual state after development-time code or resource reloads. */
public interface ReloadTarget {
  void refreshView();

  default void refreshStylesheets() {}
}
