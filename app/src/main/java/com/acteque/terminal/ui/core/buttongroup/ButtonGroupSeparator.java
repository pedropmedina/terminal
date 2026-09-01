package com.acteque.terminal.ui.core.buttongroup;

import java.util.Objects;
import javafx.geometry.Orientation;

/** A separator sized and colored for use inside a button group. */
public final class ButtonGroupSeparator extends javafx.scene.control.Separator {

  private static final String ROOT_STYLE_CLASS = "core-button-group-separator";

  public ButtonGroupSeparator() {
    this(Orientation.VERTICAL);
  }

  public ButtonGroupSeparator(Orientation orientation) {
    super(Objects.requireNonNull(orientation, "orientation"));
    getStyleClass().add(ROOT_STYLE_CLASS);
  }
}
