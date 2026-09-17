package com.acteque.terminal.ui;

/** Theme choices used by the component stylesheet tests. */
public enum AppTheme {
  LIGHT("theme-light"),
  DARK("theme-dark");

  private final String styleClass;

  AppTheme(String styleClass) {
    this.styleClass = styleClass;
  }

  String styleClass() {
    return styleClass;
  }
}
