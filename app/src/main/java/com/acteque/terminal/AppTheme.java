package com.acteque.terminal;

/** Visual themes supported by the application design system. */
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
