package com.acteque.terminal.ui.core.popover;

import com.acteque.terminal.ui.core.Button;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;

/** A core button that toggles its associated popover. */
public final class PopoverTrigger extends Button {

  private final ObjectProperty<Popover> popover = new SimpleObjectProperty<>(this, "popover");

  public PopoverTrigger(String text, Popover popover) {
    this(text, null, Variant.DEFAULT, Size.DEFAULT, popover);
  }

  public PopoverTrigger(String text, Node graphic, Variant variant, Size size, Popover popover) {
    super(text, graphic, variant, size);
    getStyleClass().add("core-popover-trigger");
    setPopover(popover);
    setOnAction(event -> getPopover().toggle(this));
  }

  public Popover getPopover() {
    return popover.get();
  }

  public void setPopover(Popover value) {
    popover.set(Objects.requireNonNull(value, "popover"));
  }

  public ObjectProperty<Popover> popoverProperty() {
    return popover;
  }
}
