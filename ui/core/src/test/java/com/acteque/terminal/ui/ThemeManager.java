package com.acteque.terminal.ui;

import java.util.Objects;
import javafx.scene.Scene;

/** Loads the component styles without the application's chart reload wiring. */
public final class ThemeManager {

  public ThemeManager(Scene scene, AppTheme theme) {
    scene
      .getStylesheets()
      .add(Objects.requireNonNull(getClass().getResource("/com/acteque/terminal/ui/test-theme.css")).toExternalForm());
    scene
      .getStylesheets()
      .add(Objects.requireNonNull(getClass().getResource("/com/acteque/terminal/ui/core.css")).toExternalForm());
    scene
      .getStylesheets()
      .add(Objects.requireNonNull(getClass().getResource("/com/acteque/terminal/ui/icons/icon.css")).toExternalForm());
    scene.getRoot().getStyleClass().add(theme.styleClass());
  }
}
