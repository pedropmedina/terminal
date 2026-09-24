package com.acteque.terminal.ui.combobox;

import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.util.Callback;

/** Reactive rendering of the combobox selection. */
public final class ComboboxValue<T> extends FlowPane {

  private final Combobox<T> combobox;
  private final ObjectProperty<Callback<T, Node>> nodeFactory = new SimpleObjectProperty<>(this, "nodeFactory");

  /** Creates a value renderer with default label and chip nodes. */
  public ComboboxValue(Combobox<T> combobox) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    getStyleClass().add("core-combobox-value");
    setAccessibleRole(AccessibleRole.PARENT);
    setHgap(4.0);
    setVgap(4.0);
    nodeFactory.addListener(ignored -> refresh());
    combobox.valueProperty().addListener(ignored -> refresh());
    combobox.multipleProperty().addListener(ignored -> refresh());
    combobox.converterProperty().addListener(ignored -> refresh());
    combobox.getSelectedItems().addListener((ListChangeListener<T>) ignored -> refresh());
    refresh();
  }

  /** Returns the optional custom node factory. */
  public Callback<T, Node> getNodeFactory() {
    return nodeFactory.get();
  }

  /** Sets a custom selected-value node factory, or {@code null} for defaults. */
  public void setNodeFactory(Callback<T, Node> next) {
    nodeFactory.set(next);
  }

  /** Returns the selected-value node-factory property. */
  public ObjectProperty<Callback<T, Node>> nodeFactoryProperty() {
    return nodeFactory;
  }

  /** Rebuilds selected-value nodes from current root state. */
  private void refresh() {
    getChildren().clear();
    List<T> values = combobox.isMultiple()
      ? List.copyOf(combobox.getSelectedItems())
      : combobox.getValue() == null
        ? List.of()
        : List.of(combobox.getValue());
    for (T value : values) {
      Node node = getNodeFactory() == null ? defaultNode(value) : getNodeFactory().call(value);
      getChildren().add(Objects.requireNonNull(node, "nodeFactory returned null"));
    }
  }

  /** Creates the default label or removable chip for a selected value. */
  private Node defaultNode(T value) {
    return combobox.isMultiple() ? new ComboboxChip<>(combobox, value) : new Label(combobox.labelFor(value));
  }
}
