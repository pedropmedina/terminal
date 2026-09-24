package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Objects;
import javafx.css.PseudoClass;
import javafx.event.ActionEvent;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Selectable combobox option with highlighted and selected presentation states. */
public final class ComboboxItem<T> extends Button {

  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

  private final Combobox<T> combobox;
  private final T item;
  private final Label defaultLabel = new Label();
  private final HBox content = new HBox();
  private final StackPane indicator = new StackPane(new LucideIcon(LucideIcons.CHECK));
  private Node customContent;
  private boolean filteredVisible = true;

  /**
   * Creates an item using the root converter for its label.
   *
   * @param combobox the owning combobox
   * @param item the represented value
   */
  public ComboboxItem(Combobox<T> combobox, T item) {
    this(combobox, item, null);
  }

  /**
   * Creates an item with custom content.
   *
   * @param combobox the owning combobox
   * @param item the represented value
   * @param customContent the custom item node, or {@code null} for the converted label
   */
  public ComboboxItem(Combobox<T> combobox, T item, Node customContent) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    this.item = Objects.requireNonNull(item, "item cannot be null");
    this.customContent = customContent;
    getStyleClass().setAll("core-combobox-item");
    setAccessibleRole(AccessibleRole.LIST_ITEM);
    setFocusTraversable(false);
    setMaxWidth(Double.MAX_VALUE);
    setAlignment(Pos.CENTER_LEFT);
    content.getStyleClass().add("combobox-item-content");
    content.setAlignment(Pos.CENTER_LEFT);
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    indicator.getStyleClass().add("combobox-item-indicator");
    content.getChildren().addAll(displayNode(), spacer, indicator);
    setGraphic(content);
    setOnMouseEntered(ignored -> this.combobox.highlight(this));
    addEventHandler(ActionEvent.ACTION, ignored -> this.combobox.select(this.item));
    this.combobox.registerItem(this);
    refreshText();
    refreshSelection();
  }

  /** Returns the represented value. */
  public T getItem() {
    return item;
  }

  /** Returns the custom content node, or {@code null} when the default label is used. */
  public Node getItemContent() {
    return customContent;
  }

  /** Replaces the content displayed before the selection indicator. */
  public void setItemContent(Node next) {
    customContent = next;
    content.getChildren().set(0, displayNode());
  }

  /** Removes this item from its root's interaction registry. */
  public void dispose() {
    combobox.unregisterItem(this);
  }

  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (attribute == AccessibleAttribute.SELECTED) {
      return combobox.isSelected(item);
    }
    if (attribute == AccessibleAttribute.TEXT) {
      return combobox.labelFor(item);
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  /** Updates selected state and indicator visibility from root selection. */
  void refreshSelection() {
    boolean selected = combobox.isSelected(item);
    pseudoClassStateChanged(SELECTED, selected);
    indicator.setVisible(selected);
    indicator.setManaged(true);
    notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
  }

  /** Refreshes the default label from the root converter. */
  void refreshText() {
    defaultLabel.setText(combobox.labelFor(item));
  }

  /** Applies keyboard or pointer highlight state. */
  void setHighlighted(boolean value) {
    pseudoClassStateChanged(HIGHLIGHTED, value);
  }

  /** Applies filter/limit visibility without losing the item registration. */
  void setFilteredVisible(boolean value) {
    filteredVisible = value;
    setVisible(value);
    setManaged(value);
  }

  /** Applies root disabled-item predicate state. */
  void setItemDisabled(boolean value) {
    setDisable(value);
  }

  /** Returns whether this item is included by current filtering and limits. */
  boolean isFilteredVisible() {
    return filteredVisible;
  }

  /** Returns custom content or the default converted label. */
  private Node displayNode() {
    return customContent == null ? defaultLabel : customContent;
  }
}
