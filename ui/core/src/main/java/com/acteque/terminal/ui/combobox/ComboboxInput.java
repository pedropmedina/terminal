package com.acteque.terminal.ui.combobox;

import com.acteque.terminal.ui.inputgroup.InputGroup;
import com.acteque.terminal.ui.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.inputgroup.InputGroupInput;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;

/** Shadcn-style input group containing a combobox editor and optional actions. */
public final class ComboboxInput<T> extends InputGroup {

  private final Combobox<T> combobox;
  private final InputGroupInput editor = new InputGroupInput();
  private final ComboboxTrigger trigger;
  private final ComboboxClear clear;
  private final InputGroupAddon addon;
  private final BooleanProperty showTrigger = new SimpleBooleanProperty(this, "showTrigger", true);
  private final BooleanProperty showClear = new SimpleBooleanProperty(this, "showClear", false);

  /** Creates an input group for the supplied root. */
  public ComboboxInput(Combobox<T> combobox) {
    this.combobox = Objects.requireNonNull(combobox, "combobox cannot be null");
    trigger = new ComboboxTrigger(combobox);
    clear = new ComboboxClear(combobox);
    addon = new InputGroupAddon(InputGroupAlignment.INLINE_END, trigger, clear);
    getStyleClass().add("core-combobox-input");
    setAccessibleRole(AccessibleRole.COMBO_BOX);
    editor.getStyleClass().add("combobox-input-editor");
    getChildren().addAll(editor, addon);
    editor.disableProperty().bind(disabledProperty());
    trigger.disableProperty().bind(disabledProperty());
    clear.disableProperty().bind(disabledProperty());
    showTrigger.addListener(ignored -> refreshActions());
    showClear.addListener(ignored -> refreshActions());
    combobox.valueProperty().addListener(ignored -> refreshActions());
    combobox.getSelectedItems().addListener((javafx.collections.ListChangeListener<T>) ignored -> refreshActions());
    combobox.inputValueProperty().addListener(ignored -> refreshActions());
    combobox.registerEditor(editor, this);
    combobox.openProperty().addListener(ignored -> notifyAccessibleAttributeChanged(AccessibleAttribute.EXPANDED));
    refreshActions();
  }

  /** Returns combobox-specific accessible state. */
  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (attribute == AccessibleAttribute.EXPANDED) {
      return combobox.isOpen();
    }
    if (attribute == AccessibleAttribute.TEXT) {
      return combobox.getInputValue();
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  /** Returns the editable text control. */
  public InputGroupInput getEditor() {
    return editor;
  }

  /** Returns whether the popup trigger is available when the clear action is hidden. */
  public boolean isShowTrigger() {
    return showTrigger.get();
  }

  /** Sets whether the popup trigger is available. */
  public void setShowTrigger(boolean next) {
    showTrigger.set(next);
  }

  /** Returns the show-trigger property. */
  public BooleanProperty showTriggerProperty() {
    return showTrigger;
  }

  /** Returns whether a contextual clear action is available. */
  public boolean isShowClear() {
    return showClear.get();
  }

  /** Sets whether a contextual clear action is available. */
  public void setShowClear(boolean next) {
    showClear.set(next);
  }

  /** Returns the show-clear property. */
  public BooleanProperty showClearProperty() {
    return showClear;
  }

  /** Returns the popup trigger button. */
  public ComboboxTrigger getTrigger() {
    return trigger;
  }

  /** Returns the clear button. */
  public ComboboxClear getClear() {
    return clear;
  }

  /** Shows the clear action contextually and otherwise shows the trigger. */
  private void refreshActions() {
    boolean hasValue = combobox.getValue() != null || !combobox.getSelectedItems().isEmpty();
    boolean clearVisible = isShowClear() && (hasValue || !combobox.getInputValue().isEmpty());
    clear.setVisible(clearVisible);
    clear.setManaged(clearVisible);
    boolean triggerVisible = isShowTrigger() && !clearVisible;
    trigger.setVisible(triggerVisible);
    trigger.setManaged(triggerVisible);
    addon.setVisible(clearVisible || triggerVisible);
    addon.setManaged(clearVisible || triggerVisible);
  }
}
