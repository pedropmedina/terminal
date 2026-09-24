package com.acteque.terminal;

import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.List;
import java.util.Objects;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Parent;
import javafx.scene.Scene;

/** Owns the scene-level stylesheet and applies one semantic theme to the root node. */
public final class AppThemeManager implements ReloadTarget {

  private static final List<String> STYLESHEETS = List.of(
    stylesheet("/com/acteque/terminal/theme.css"),
    stylesheet("/com/acteque/terminal/ui/core.css"),
    stylesheet("/com/acteque/terminal/ui/icons/icon.css"),
    stylesheet("/com/acteque/terminal/app.css")
  );

  private final Scene scene;
  private final ObjectProperty<AppTheme> theme = new SimpleObjectProperty<>(this, "theme");

  /**
   * Installs application stylesheets and applies the initial theme to the scene root.
   *
   * @param scene the scene whose root and stylesheets are managed
   * @param initialTheme the theme applied during initialization
   */
  public AppThemeManager(Scene scene, AppTheme initialTheme) {
    this.scene = Objects.requireNonNull(scene, "scene cannot be null");

    installStylesheets();
    connectScene();
    configureRoot(scene.getRoot(), null);
    setTheme(initialTheme);
    ReloadHooks.register(this);
  }

  /**
   * Returns the read-only observable theme state.
   *
   * @return the current theme property
   */
  public ReadOnlyObjectProperty<AppTheme> themeProperty() {
    return theme;
  }

  /**
   * Returns the current semantic theme.
   *
   * @return the selected theme
   */
  public AppTheme getTheme() {
    return theme.get();
  }

  /**
   * Selects a theme, dispatching the state change to the JavaFX application thread when necessary.
   *
   * @param selectedTheme the non-null theme to apply
   */
  public void setTheme(AppTheme selectedTheme) {
    AppTheme requiredTheme = Objects.requireNonNull(selectedTheme, "selectedTheme cannot be null");
    if (Platform.isFxApplicationThread()) {
      theme.set(requiredTheme);
    } else {
      Platform.runLater(() -> theme.set(requiredTheme));
    }
  }

  /** Reapplies semantic theme classes after a development-time view reload. */
  @Override
  public void refreshView() {
    applyTheme(getTheme());
  }

  /** Removes and reinstalls managed stylesheets after a development-time resource reload. */
  @Override
  public void refreshStylesheets() {
    scene.getStylesheets().removeAll(STYLESHEETS);
    scene.getStylesheets().addAll(STYLESHEETS);
  }

  /** Adds each managed stylesheet without duplicating an existing scene entry. */
  private void installStylesheets() {
    for (String stylesheet : STYLESHEETS) {
      if (!scene.getStylesheets().contains(stylesheet)) {
        scene.getStylesheets().add(stylesheet);
      }
    }
  }

  /** Connects theme and root changes to semantic root-class updates. */
  private void connectScene() {
    theme.addListener(this::handleThemeChanged);
    scene.rootProperty().addListener(this::handleRootChanged);
  }

  /**
   * Applies the newly selected theme.
   *
   * @param observable the observed theme property
   * @param previousTheme the previously selected theme
   * @param selectedTheme the newly selected theme
   */
  private void handleThemeChanged(
    ObservableValue<? extends AppTheme> observable,
    AppTheme previousTheme,
    AppTheme selectedTheme
  ) {
    applyTheme(selectedTheme);
  }

  /**
   * Applies the current theme when the scene root is replaced.
   *
   * @param observable the observed root property
   * @param previousRoot the previous scene root
   * @param currentRoot the new scene root
   */
  private void handleRootChanged(
    ObservableValue<? extends Parent> observable,
    Parent previousRoot,
    Parent currentRoot
  ) {
    configureRoot(currentRoot, getTheme());
  }

  /**
   * Resolves a required stylesheet resource.
   *
   * @param path the absolute classpath resource path
   * @return the resource's external URL form
   */
  private static String stylesheet(String path) {
    return Objects.requireNonNull(
      AppThemeManager.class.getResource(path),
      path + " stylesheet resource was not found"
    ).toExternalForm();
  }

  /**
   * Applies a semantic theme to the current scene root.
   *
   * @param selectedTheme the theme to apply, or {@code null} to remove all theme classes
   */
  private void applyTheme(AppTheme selectedTheme) {
    configureRoot(scene.getRoot(), selectedTheme);
  }

  /**
   * Replaces known theme classes on a root node with the selected theme class.
   *
   * @param root the root node to configure
   * @param selectedTheme the theme to apply, or {@code null} to remove all theme classes
   */
  private static void configureRoot(Parent root, AppTheme selectedTheme) {
    for (AppTheme availableTheme : AppTheme.values()) {
      root.getStyleClass().remove(availableTheme.styleClass());
    }
    if (selectedTheme != null) {
      root.getStyleClass().add(selectedTheme.styleClass());
    }
  }
}
