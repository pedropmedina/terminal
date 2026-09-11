package com.acteque.terminal.ui.core.tooltip;

import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

/** Content displayed inside a {@link Tooltip} popup. */
public final class TooltipContent extends HBox {

  public TooltipContent(String text) {
    this(new Label(Objects.requireNonNull(text, "text")));
  }

  public TooltipContent(Node... children) {
    getStyleClass().add("core-tooltip-popup");
    getChildren().addAll(Objects.requireNonNull(children, "children"));
  }
}
