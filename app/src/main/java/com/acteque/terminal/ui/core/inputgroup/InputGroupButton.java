package com.acteque.terminal.ui.core.inputgroup;

import com.acteque.terminal.ui.core.Button;
import javafx.scene.Node;

/** A compact button displayed in an input-group addon. */
public final class InputGroupButton extends Button {

  private static final String ROOT_STYLE_CLASS = "core-input-group-button";

  public InputGroupButton() {
    this(null, null);
  }

  public InputGroupButton(String text) {
    this(text, null);
  }

  public InputGroupButton(Node graphic) {
    this(null, graphic, Variant.GHOST, Size.ICON_XS);
  }

  public InputGroupButton(String text, Node graphic) {
    this(text, graphic, Variant.GHOST, Size.XS);
  }

  public InputGroupButton(String text, Node graphic, Variant variant, Size size) {
    super(text, graphic, variant, size);
    getStyleClass().add(ROOT_STYLE_CLASS);
  }
}
