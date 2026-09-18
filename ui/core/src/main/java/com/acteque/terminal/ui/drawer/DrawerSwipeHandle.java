package com.acteque.terminal.ui.drawer;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Optional visible drag affordance on the dismissing edge of a drawer. */
public final class DrawerSwipeHandle extends StackPane {

  public DrawerSwipeHandle() {
    getStyleClass().add("core-drawer-swipe-handle");
    Region bar = new Region();
    bar.getStyleClass().add("core-drawer-swipe-handle-bar");
    getChildren().add(bar);
  }
}
