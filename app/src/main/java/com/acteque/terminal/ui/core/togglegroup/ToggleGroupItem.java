package com.acteque.terminal.ui.core.togglegroup;

import com.acteque.terminal.ui.core.Toggle;
import javafx.scene.Node;

/** A toggle item whose appearance and selection behavior can be provided by a {@link ToggleGroup}. */
public final class ToggleGroupItem extends Toggle {

  public ToggleGroupItem() {
    super();
  }

  public ToggleGroupItem(String text) {
    super(text);
  }

  public ToggleGroupItem(String text, Node graphic) {
    super(text, graphic);
  }

  public ToggleGroupItem(String text, Variant variant, Size size) {
    super(text, variant, size);
  }

  public ToggleGroupItem(String text, Node graphic, Variant variant, Size size) {
    super(text, graphic, variant, size);
  }
}
