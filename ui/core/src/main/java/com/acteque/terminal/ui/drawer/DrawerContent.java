package com.acteque.terminal.ui.drawer;

import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Drawer popup and its scrollable-content-ready body. */
public final class DrawerContent extends BorderPane {

  private final VBox body = new VBox();
  private final DrawerSwipeHandle swipeHandle = new DrawerSwipeHandle();
  private Node autoGrowNode;

  public DrawerContent(Node... children) {
    getStyleClass().add("core-drawer-content");
    setAccessibleRole(AccessibleRole.PARENT);
    setFocusTraversable(true);
    body.getStyleClass().add("core-drawer-body");
    body.getChildren().addAll(children);
    body.getChildren().addListener((javafx.collections.ListChangeListener<Node>) change -> updateFooterGrowth());
    updateFooterGrowth();
    setCenter(body);
  }

  /** The content column, whose children can be changed after construction. */
  public VBox getBody() {
    return body;
  }

  public DrawerSwipeHandle getSwipeHandle() {
    return swipeHandle;
  }

  void configureHandle(DrawerDirection direction, boolean visible) {
    setTop(null);
    setBottom(null);
    setLeft(null);
    setRight(null);
    if (!visible) {
      return;
    }
    switch (direction) {
      case DOWN -> setTop(swipeHandle);
      case UP -> setBottom(swipeHandle);
      case LEFT -> setRight(swipeHandle);
      case RIGHT -> setLeft(swipeHandle);
    }
    swipeHandle.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("vertical"), direction.isVertical());
    swipeHandle.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("horizontal"), !direction.isVertical());
  }

  private void updateFooterGrowth() {
    if (autoGrowNode != null && VBox.getVgrow(autoGrowNode) == Priority.ALWAYS) {
      VBox.setVgrow(autoGrowNode, null);
    }
    autoGrowNode = null;
    for (int index = 1; index < body.getChildren().size(); index++) {
      if (body.getChildren().get(index) instanceof DrawerFooter) {
        Node previous = body.getChildren().get(index - 1);
        if (VBox.getVgrow(previous) == null) {
          VBox.setVgrow(previous, Priority.ALWAYS);
          autoGrowNode = previous;
        }
        break;
      }
    }
  }
}
