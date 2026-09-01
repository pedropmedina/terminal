package com.acteque.terminal.ui.core;

import java.util.Objects;
import javafx.geometry.Orientation;

/** A theme-aware divider that fills the available space along its orientation. */
public final class Separator extends javafx.scene.control.Separator {

  private static final String ROOT_STYLE_CLASS = "core-separator";

  public Separator() {
    this(Orientation.HORIZONTAL);
  }

  public Separator(Orientation orientation) {
    super(Objects.requireNonNull(orientation, "orientation"));
    getStyleClass().add(ROOT_STYLE_CLASS);
  }
}
