package com.acteque.terminal.ui.core.tooltip;

import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/** Scene-graph wrapper that owns the node which triggers a {@link Tooltip}. */
public final class TooltipTrigger extends StackPane {

  private final ReadOnlyBooleanWrapper targetDisabled = new ReadOnlyBooleanWrapper(this, "targetDisabled");
  private final ReadOnlyBooleanWrapper targetFocusVisible = new ReadOnlyBooleanWrapper(this, "targetFocusVisible");
  private final ChangeListener<Boolean> disabledListener = (ignored, oldValue, value) -> targetDisabled.set(value);
  private final ChangeListener<Boolean> focusVisibleListener = (ignored, oldValue, value) ->
    targetFocusVisible.set(value);
  private final ObjectProperty<Node> target = new ObjectPropertyBase<>() {
    @Override
    public void set(Node value) {
      super.set(Objects.requireNonNull(value, "target"));
    }

    @Override
    public Object getBean() {
      return TooltipTrigger.this;
    }

    @Override
    public String getName() {
      return "target";
    }
  };

  public TooltipTrigger(Node target) {
    getStyleClass().add("core-tooltip-trigger");
    setPickOnBounds(false);
    this.target.addListener((ignored, oldTarget, newTarget) -> replaceTarget(oldTarget, newTarget));
    setTarget(target);
  }

  public Node getTarget() {
    return target.get();
  }

  public void setTarget(Node value) {
    target.set(Objects.requireNonNull(value, "target"));
  }

  public ObjectProperty<Node> targetProperty() {
    return target;
  }

  public boolean isTargetDisabled() {
    return targetDisabled.get();
  }

  public ReadOnlyBooleanProperty targetDisabledProperty() {
    return targetDisabled.getReadOnlyProperty();
  }

  public boolean isTargetFocusVisible() {
    return targetFocusVisible.get();
  }

  public ReadOnlyBooleanProperty targetFocusVisibleProperty() {
    return targetFocusVisible.getReadOnlyProperty();
  }

  private void replaceTarget(Node oldTarget, Node newTarget) {
    if (oldTarget != null) {
      oldTarget.disabledProperty().removeListener(disabledListener);
      oldTarget.focusVisibleProperty().removeListener(focusVisibleListener);
    }
    getChildren().setAll(newTarget);
    targetDisabled.set(newTarget.isDisabled());
    targetFocusVisible.set(newTarget.isFocusVisible());
    newTarget.disabledProperty().addListener(disabledListener);
    newTarget.focusVisibleProperty().addListener(focusVisibleListener);
  }
}
