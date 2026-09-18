package com.acteque.terminal.ui.drawer;

import com.acteque.terminal.ui.Button;
import javafx.scene.Node;
import javafx.scene.Parent;

/** A button that closes its nearest enclosing drawer. */
public final class DrawerClose extends Button {

  public DrawerClose(String text) {
    this(text, Variant.OUTLINE, Size.DEFAULT);
  }

  public DrawerClose(String text, Variant variant, Size size) {
    this(text, null, variant, size);
  }

  public DrawerClose(String text, Node graphic, Variant variant, Size size) {
    super(text, graphic, variant, size);
    getStyleClass().add("core-drawer-close");
    setOnAction(event -> findDrawer().close());
  }

  private Drawer findDrawer() {
    Parent ancestor = getParent();
    while (ancestor != null && !(ancestor instanceof Drawer)) {
      ancestor = ancestor.getParent();
    }
    if (ancestor instanceof Drawer drawer) {
      return drawer;
    }
    throw new IllegalStateException("DrawerClose must be attached beneath a Drawer");
  }
}
