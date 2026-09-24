package com.acteque.terminal;

/** Visual themes supported by the application design system. */
public enum AppTheme {
  /** The light application color scheme. */
  LIGHT("theme-light"),

  /** The dark application color scheme. */
  DARK("theme-dark");

  private final String styleClass;

  /**
   * Creates a theme associated with its root CSS class.
   *
   * @param styleClass the CSS class that activates the theme
   */
  AppTheme(String styleClass) {
    this.styleClass = styleClass;
  }

  /**
   * Returns the CSS class that activates this theme.
   *
   * @return the theme's root CSS class
   */
  String styleClass() {
    return styleClass;
  }
}
