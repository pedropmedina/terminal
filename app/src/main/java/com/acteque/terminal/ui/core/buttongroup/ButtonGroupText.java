package com.acteque.terminal.ui.core.buttongroup;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Text or supporting content displayed as an item in a button group. */
public final class ButtonGroupText extends HBox {

  private static final String ROOT_STYLE_CLASS = "core-button-group-text";

  public ButtonGroupText() {
    this(new Node[0]);
  }

  public ButtonGroupText(String text) {
    this(new Label(text));
  }

  public ButtonGroupText(Node... children) {
    setAlignment(Pos.CENTER);
    getStyleClass().add(ROOT_STYLE_CLASS);
    getChildren().addAll(children);
  }
}
