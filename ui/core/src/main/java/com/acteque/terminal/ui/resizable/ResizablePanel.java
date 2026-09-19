package com.acteque.terminal.ui.resizable;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** Content hosted inside a {@link ResizablePanelGroup}. */
public final class ResizablePanel extends StackPane {

  private final Rectangle clip = new Rectangle();

  public ResizablePanel() {
    this(new Node[0]);
  }

  public ResizablePanel(Node... children) {
    getStyleClass().add("core-resizable-panel");
    setAccessibleRole(AccessibleRole.PARENT);
    setMinSize(0.0, 0.0);
    clip.widthProperty().bind(widthProperty());
    clip.heightProperty().bind(heightProperty());
    setClip(clip);
    getChildren().addAll(children);
  }
}
