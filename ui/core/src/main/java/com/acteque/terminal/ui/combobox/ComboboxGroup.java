package com.acteque.terminal.ui.combobox;

import java.util.Objects;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

/** Group of related combobox options. */
public final class ComboboxGroup<T> extends VBox {

  /**
   * Creates an option group.
   *
   * @param combobox the owning combobox
   * @param children the label, collection, or explicit items
   */
  public ComboboxGroup(Combobox<T> combobox, Node... children) {
    Objects.requireNonNull(combobox, "combobox cannot be null").registerGroup(this);
    getStyleClass().add("core-combobox-group");
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addAll(children);
    refreshVisibility();
  }

  /** Shows the group only when it contains a filtered-visible item. */
  void refreshVisibility() {
    boolean hasVisibleItem = containsVisibleItem(this);
    setVisible(hasVisibleItem);
    setManaged(hasVisibleItem);
  }

  /** Returns whether a subtree contains a filtered-visible item. */
  private static boolean containsVisibleItem(Parent parent) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof ComboboxItem<?> item && item.isFilteredVisible()) {
        return true;
      }
      if (child instanceof Parent nested && containsVisibleItem(nested)) {
        return true;
      }
    }
    return false;
  }
}
