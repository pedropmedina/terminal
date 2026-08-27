package com.acteque.terminal.ui;

/** A view whose visual state can be rebuilt without replacing its scene-graph node. */
public interface RefreshableView {
  void refreshView();

  default void refreshStylesheets() {}
}
