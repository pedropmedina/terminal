package com.acteque.terminal.ui;

import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;

/** Shared visual constants used by reusable and feature-level UI components. */
public final class UiStyles {

  public static final Background SURFACE_BACKGROUND = new Background(
    new BackgroundFill(Color.rgb(242, 243, 245), new CornerRadii(8.0), Insets.EMPTY)
  );

  private UiStyles() {}
}
