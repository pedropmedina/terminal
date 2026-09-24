package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;

/** Compact representation of one selected combobox value. */
public final class ComboboxChip<T> extends HBox {

  private final Combobox<T> combobox;
  private final T item;
  private final Label label = new Label();
  private final Button remove = new Button(
    null,
    new LucideIcon(LucideIcons.X),
    Button.Variant.GHOST,
    Button.Size.ICON_XS
  );
  private final BooleanProperty showRemove = new SimpleBooleanProperty(this, "showRemove", true);

  /** Creates a chip using the converted item label. */
  public ComboboxChip(Combobox<T> combobox, T item) {
    this(combobox, item, null);
  }

  /** Creates a chip with optional custom content. */
  public ComboboxChip(Combobox<T> combobox, T item, Node customContent) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    this.item = Objects.requireNonNull(item, "item cannot be null");
    getStyleClass().add("core-combobox-chip");
    setAccessibleRole(AccessibleRole.LIST_ITEM);
    setFocusTraversable(true);
    setAlignment(Pos.CENTER);
    label.setText(combobox.labelFor(item));
    remove.getStyleClass().add("combobox-chip-remove");
    remove.setAccessibleText("Remove " + label.getText());
    remove.setOnAction(ignored -> combobox.removeSelected(item));
    getChildren().add(customContent == null ? label : customContent);
    getChildren().add(remove);
    showRemove.addListener(ignored -> refreshRemove());
    addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    refreshRemove();
  }

  /** Returns the represented value. */
  public T getItem() {
    return item;
  }

  /** Returns whether the removal affordance is shown. */
  public boolean isShowRemove() {
    return showRemove.get();
  }

  /** Sets whether the removal affordance is shown. */
  public void setShowRemove(boolean next) {
    showRemove.set(next);
  }

  /** Returns the show-remove property. */
  public BooleanProperty showRemoveProperty() {
    return showRemove;
  }

  /** Applies the show-remove property to the removal button. */
  private void refreshRemove() {
    remove.setVisible(isShowRemove());
    remove.setManaged(isShowRemove());
  }

  /** Removes this chip for Delete or Backspace. */
  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.DELETE || event.getCode() == KeyCode.BACK_SPACE) {
      combobox.removeSelected(item);
      event.consume();
    }
  }
}
