package com.acteque.terminal.ui.core.dialog;

import com.acteque.terminal.ui.core.Button;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/** A core button that opens its associated dialog. */
public final class DialogTrigger extends Button {

  private final ObjectProperty<Dialog> dialog = new SimpleObjectProperty<>(this, "dialog");

  public DialogTrigger(String text, Dialog dialog) {
    this(text, null, Variant.DEFAULT, Size.DEFAULT, dialog);
  }

  public DialogTrigger(String text, Node graphic, Variant variant, Size size, Dialog dialog) {
    super(text, graphic, variant, size);
    getStyleClass().add("core-dialog-trigger");
    setDialog(dialog);
    setOnAction(event -> getDialog().show());
  }

  public Dialog getDialog() {
    return dialog.get();
  }

  public void setDialog(Dialog value) {
    dialog.set(Objects.requireNonNull(value, "dialog"));
  }

  public ObjectProperty<Dialog> dialogProperty() {
    return dialog;
  }
}
