package com.acteque.terminal.ui.core.inputgroup;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Non-interactive text or supporting content displayed in an input-group addon. */
public final class InputGroupText extends HBox {

  private static final String ROOT_STYLE_CLASS = "core-input-group-text";

  public InputGroupText() {
    this(new Node[0]);
  }

  public InputGroupText(String text) {
    this(new Label(text));
  }

  public InputGroupText(Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setMouseTransparent(true);
    getChildren().addAll(children);
  }
}
