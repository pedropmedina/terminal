package com.acteque.terminal.ui.kbd;

import javafx.scene.Node;
import javafx.scene.layout.Region;

/** A compact, non-interactive representation of keyboard input. */
public final class Kbd extends javafx.scene.control.Label {

  private static final String ROOT_STYLE_CLASS = "core-kbd";

  public Kbd() {
    this(null, null);
  }

  public Kbd(String text) {
    this(text, null);
  }

  public Kbd(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add(ROOT_STYLE_CLASS);
    setMaxWidth(Region.USE_PREF_SIZE);
    setMouseTransparent(true);
  }
}
