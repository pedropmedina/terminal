package com.acteque.terminal.ui.drawer;

/** Edge toward which a drawer is swiped to dismiss it. */
public enum DrawerDirection {
  DOWN,
  UP,
  LEFT,
  RIGHT;

  boolean isVertical() {
    return this == DOWN || this == UP;
  }

  int dismissSign() {
    return this == DOWN || this == RIGHT ? 1 : -1;
  }
}
