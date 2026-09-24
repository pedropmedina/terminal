package com.acteque.terminal.ui.combobox;

import java.util.Objects;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.FlowPane;

/** Wrapping selection-chip surface used as a multiple combobox anchor. */
public final class ComboboxChips<T> extends FlowPane {

  private static final PseudoClass CONTROL_FOCUS_VISIBLE = PseudoClass.getPseudoClass("control-focus-visible");
  private final Combobox<T> combobox;

  /** Creates a chips surface. */
  public ComboboxChips(Combobox<T> combobox, Node... children) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-chips");
    setAccessibleRole(AccessibleRole.PARENT);
    setHgap(4.0);
    setVgap(4.0);
    getChildren().addAll(children);
    focusedProperty().addListener(ignored -> refreshFocusState());
    getChildren().addListener((javafx.collections.ListChangeListener<Node>) ignored -> refreshFocusState());
    addEventFilter(KeyEvent.KEY_PRESSED, this::moveFocus);
  }

  /** Returns the owning combobox. */
  public Combobox<T> getCombobox() {
    return combobox;
  }

  /** Returns the mutable chip and editor children. */
  public javafx.collections.ObservableList<Node> getItems() {
    return getChildren();
  }

  /** Mirrors descendant focus-visible state to the chips surface. */
  private void refreshFocusState() {
    boolean focused = isFocusVisible() || getChildren().stream().anyMatch(Node::isFocusVisible);
    pseudoClassStateChanged(CONTROL_FOCUS_VISIBLE, focused);
  }

  /** Moves focus between visible chip controls and the input with arrow keys. */
  private void moveFocus(KeyEvent event) {
    if (event.getCode() != KeyCode.LEFT && event.getCode() != KeyCode.RIGHT) {
      return;
    }
    Scene scene = getScene();
    Node focused = scene == null ? null : scene.getFocusOwner();
    java.util.List<Node> focusable = getChildren()
      .stream()
      .filter(Node::isVisible)
      .filter(node -> !node.isDisabled())
      .filter(Node::isFocusTraversable)
      .toList();
    if (focusable.isEmpty()) {
      return;
    }
    int current = focusable.indexOf(focused);
    int direction = event.getCode() == KeyCode.RIGHT ? 1 : -1;
    int next = current < 0 ? (direction > 0 ? 0 : focusable.size() - 1) : current + direction;
    if (next >= 0 && next < focusable.size()) {
      focusable.get(next).requestFocus();
      event.consume();
    }
  }
}
