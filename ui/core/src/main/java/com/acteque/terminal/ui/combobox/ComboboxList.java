package com.acteque.terminal.ui.combobox;

import java.util.Objects;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Scrollable list container for combobox groups, collections, and explicit items. */
public final class ComboboxList<T> extends ScrollPane {

  private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");
  private final VBox entries = new VBox();

  /** Creates a combobox list containing the supplied nodes. */
  public ComboboxList(Combobox<T> combobox, Node... children) {
    Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-list");
    entries.getStyleClass().add("combobox-list-entries");
    entries.getChildren().addAll(children);
    setContent(entries);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    setVbarPolicy(ScrollBarPolicy.AS_NEEDED);
    setAccessibleRole(AccessibleRole.LIST_VIEW);
    setFocusTraversable(false);
  }

  /** Returns the mutable list of rendered entry nodes. */
  public javafx.collections.ObservableList<Node> getEntries() {
    return entries.getChildren();
  }

  /** Applies the shadcn list maximum-height formula. */
  void setAvailableHeight(double value) {
    double maximum = Math.max(0.0, Math.min(252.0, value - 36.0));
    setMaxHeight(maximum);
    setPrefViewportHeight(Math.min(entries.prefHeight(-1.0), maximum));
  }

  /** Applies the empty pseudo-class used to remove list padding. */
  void setEmpty(boolean value) {
    pseudoClassStateChanged(EMPTY, value);
  }
}
