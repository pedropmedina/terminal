package com.acteque.terminal.ui.combobox;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.layout.VBox;
import javafx.util.Callback;

/** Data-driven collection that materializes combobox items from an observable list. */
public final class ComboboxCollection<T> extends VBox {

  private final Combobox<T> combobox;
  private final ObservableList<T> source;
  private final List<ComboboxItem<T>> generatedItems = new ArrayList<>();
  private final ObjectProperty<Callback<T, ComboboxItem<T>>> itemFactory;
  private final InvalidationListener rebuildListener = ignored -> rebuild();

  /** Creates a collection backed by the root items. */
  public ComboboxCollection(Combobox<T> combobox) {
    this(combobox, Objects.requireNonNull(combobox, "combobox cannot be null").itemsProperty());
  }

  /**
   * Creates a collection backed by the supplied items.
   *
   * @param combobox the owning combobox
   * @param source the observable items rendered by this collection
   */
  public ComboboxCollection(Combobox<T> combobox, ObservableList<T> source) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    this.source = Objects.requireNonNull(source, "source cannot be null");
    itemFactory = new SimpleObjectProperty<>(this, "itemFactory", value -> new ComboboxItem<>(this.combobox, value));
    getStyleClass().add("core-combobox-collection");
    setAccessibleRole(AccessibleRole.PARENT);
    this.source.addListener(rebuildListener);
    itemFactory.addListener(rebuildListener);
    rebuild();
  }

  /** Returns the item factory. */
  public Callback<T, ComboboxItem<T>> getItemFactory() {
    return itemFactory.get();
  }

  /** Sets the factory used to render each source item. */
  public void setItemFactory(Callback<T, ComboboxItem<T>> next) {
    itemFactory.set(Objects.requireNonNull(next, "itemFactory cannot be null"));
  }

  /** Returns the item-factory property. */
  public ObjectProperty<Callback<T, ComboboxItem<T>>> itemFactoryProperty() {
    return itemFactory;
  }

  /** Stops observing the source and unregisters generated items. */
  public void dispose() {
    source.removeListener(rebuildListener);
    itemFactory.removeListener(rebuildListener);
    clearGeneratedItems();
  }

  /** Recreates generated item nodes after source or factory changes. */
  private void rebuild() {
    clearGeneratedItems();
    for (T value : source) {
      ComboboxItem<T> item = Objects.requireNonNull(getItemFactory().call(value), "itemFactory returned null");
      generatedItems.add(item);
      getChildren().add(item);
    }
  }

  /** Disposes and removes all currently generated item nodes. */
  private void clearGeneratedItems() {
    generatedItems.forEach(ComboboxItem::dispose);
    generatedItems.clear();
    getChildren().clear();
  }
}
