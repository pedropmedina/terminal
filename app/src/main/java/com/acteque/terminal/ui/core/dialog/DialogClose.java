package com.acteque.terminal.ui.core.dialog;

import com.acteque.terminal.ui.core.Button;
import javafx.scene.Node;
import javafx.scene.Parent;

/** A button that closes the nearest ancestor dialog. */
public final class DialogClose extends Button {

  public DialogClose(String text) {
    this(text, Variant.OUTLINE, Size.DEFAULT);
  }

  public DialogClose(String text, Variant variant, Size size) {
    this(text, null, variant, size);
  }

  public DialogClose(String text, Node graphic, Variant variant, Size size) {
    super(text, graphic, variant, size);
    getStyleClass().add("core-dialog-close");
    setOnAction(event -> findDialog().close());
  }

  private Dialog findDialog() {
    Parent ancestor = getParent();
    while (ancestor != null && !(ancestor instanceof Dialog)) {
      ancestor = ancestor.getParent();
    }
    if (ancestor instanceof Dialog dialog) {
      return dialog;
    }
    throw new IllegalStateException("DialogClose must be attached beneath a Dialog");
  }
}
