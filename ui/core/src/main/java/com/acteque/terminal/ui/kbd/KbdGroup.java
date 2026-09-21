package com.acteque.terminal.ui.kbd;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

/** A content-sized row of related keyboard-input representations. */
public final class KbdGroup extends HBox {

  private static final String ROOT_STYLE_CLASS = "core-kbd-group";

  public KbdGroup() {
    this(new Node[0]);
  }

  public KbdGroup(Node... children) {
    setAlignment(Pos.CENTER);
    setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    getStyleClass().add(ROOT_STYLE_CLASS);
    getChildren().addAll(children);
  }
}
