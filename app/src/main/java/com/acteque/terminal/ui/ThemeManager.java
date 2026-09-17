package com.acteque.terminal.ui;

import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Parent;
import javafx.scene.Scene;

/** Owns the scene-level stylesheet and applies one semantic theme to the root node. */
public final class ThemeManager implements RefreshableView {

  private static final String[] STYLESHEETS = {
    stylesheet("/com/acteque/terminal/theme.css"),
    stylesheet("/com/acteque/terminal/ui/core.css"),
    stylesheet("/com/acteque/terminal/ui/icons/icon.css"),
    stylesheet("/com/acteque/terminal/app.css"),
  };

  private final Scene scene;
  private final ObjectProperty<AppTheme> theme = new SimpleObjectProperty<>(this, "theme");

  public ThemeManager(Scene scene, AppTheme initialTheme) {
    this.scene = Objects.requireNonNull(scene, "scene cannot be null");
    for (String stylesheet : STYLESHEETS) {
      if (!scene.getStylesheets().contains(stylesheet)) {
        scene.getStylesheets().add(stylesheet);
      }
    }

    theme.addListener((ignored, previous, current) -> applyTheme(current));
    scene.rootProperty().addListener((ignored, previous, current) -> configureRoot(current, getTheme()));
    configureRoot(scene.getRoot(), null);
    setTheme(initialTheme);
    ChartReloadHooks.register(this);
  }

  public ReadOnlyObjectProperty<AppTheme> themeProperty() {
    return theme;
  }

  public AppTheme getTheme() {
    return theme.get();
  }

  public void setTheme(AppTheme selectedTheme) {
    AppTheme requiredTheme = Objects.requireNonNull(selectedTheme, "selectedTheme cannot be null");
    if (Platform.isFxApplicationThread()) {
      theme.set(requiredTheme);
    } else {
      Platform.runLater(() -> theme.set(requiredTheme));
    }
  }

  @Override
  public void refreshView() {
    applyTheme(getTheme());
  }

  @Override
  public void refreshStylesheets() {
    scene.getStylesheets().removeAll(STYLESHEETS);
    scene.getStylesheets().addAll(STYLESHEETS);
  }

  private static String stylesheet(String path) {
    return Objects.requireNonNull(
      ThemeManager.class.getResource(path),
      path + " stylesheet resource was not found"
    ).toExternalForm();
  }

  private void applyTheme(AppTheme selectedTheme) {
    configureRoot(scene.getRoot(), selectedTheme);
  }

  private static void configureRoot(Parent root, AppTheme selectedTheme) {
    for (AppTheme availableTheme : AppTheme.values()) {
      root.getStyleClass().remove(availableTheme.styleClass());
    }
    if (selectedTheme != null) {
      root.getStyleClass().add(selectedTheme.styleClass());
    }
  }
}
