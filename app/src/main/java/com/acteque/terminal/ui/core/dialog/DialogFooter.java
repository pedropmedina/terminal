package com.acteque.terminal.ui.core.dialog;

import com.acteque.terminal.ui.core.Button;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.layout.HBox;

/** Action row at the bottom of a dialog, with an optional outline close action. */
public final class DialogFooter extends HBox {

  private final Button closeButton = new DialogClose("Close", Button.Variant.OUTLINE, Button.Size.DEFAULT);
  private final BooleanProperty showCloseButton = new SimpleBooleanProperty(this, "showCloseButton", false);

  public DialogFooter(Node... children) {
    getStyleClass().add("core-dialog-footer");
    getChildren().addAll(children);
    showCloseButton.addListener((ignored, wasVisible, isVisible) -> updateCloseButton(isVisible));
  }

  public final boolean isShowCloseButton() {
    return showCloseButton.get();
  }

  public final void setShowCloseButton(boolean value) {
    showCloseButton.set(value);
  }

  public final BooleanProperty showCloseButtonProperty() {
    return showCloseButton;
  }

  public final Button getCloseButton() {
    return closeButton;
  }

  private void updateCloseButton(boolean visible) {
    if (visible && !getChildren().contains(closeButton)) {
      getChildren().add(closeButton);
    } else if (!visible) {
      getChildren().remove(closeButton);
    }
  }
}
