package com.acteque.terminal.ui.core.togglegroup;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import com.acteque.terminal.ui.core.Toggle;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/** A horizontal or vertical set of related toggles styled like shadcn/ui Toggle Group. */
public final class ToggleGroup extends Pane {

  private static final StyleablePropertyFactory<ToggleGroup> STYLEABLES = new StyleablePropertyFactory<>(
    Pane.getClassCssMetaData()
  );
  private static final String ROOT_STYLE_CLASS = "core-toggle-group";
  private static final String HORIZONTAL_STYLE_CLASS = "toggle-group-horizontal";
  private static final String VERTICAL_STYLE_CLASS = "toggle-group-vertical";
  private static final String ZERO_SPACING_STYLE_CLASS = "toggle-group-spacing-zero";
  private static final List<String> ITEM_STYLE_CLASSES = List.of(
    "toggle-group-item",
    "toggle-group-first",
    "toggle-group-middle",
    "toggle-group-last",
    "toggle-group-only"
  );

  private final javafx.scene.control.ToggleGroup exclusiveSelection = new javafx.scene.control.ToggleGroup();
  private final InvalidationListener managedListener = ignored -> refreshItems();
  private final InvalidationListener focusedListener = ignored -> refreshFocusedViewOrder();
  private final InvalidationListener focusVisibleListener = ignored -> refreshFocusVisibleViewOrder();
  private final Map<Node, Double> unfocusedViewOrders = new IdentityHashMap<>();
  private boolean applyingCssSpacing;
  private boolean spacingWasSet;
  private final StyleableProperty<Number> cssSpacing = STYLEABLES.createStyleableNumberProperty(
    this,
    "cssSpacing",
    "-fx-spacing",
    group -> group.cssSpacing,
    8.0
  );
  private final DoubleProperty spacing = new DoublePropertyBase(8.0) {
    @Override
    public void set(double value) {
      validateSpacing(value);
      if (!applyingCssSpacing) {
        spacingWasSet = true;
      }
      super.set(value);
    }

    @Override
    protected void invalidated() {
      validateSpacing(get());
      refreshSpacingStyleClass();
      requestLayout();
    }

    @Override
    public Object getBean() {
      return ToggleGroup.this;
    }

    @Override
    public String getName() {
      return "spacing";
    }
  };

  private final ObjectProperty<Orientation> orientation = new ObjectPropertyBase<>(Orientation.HORIZONTAL) {
    @Override
    public void set(Orientation value) {
      super.set(Objects.requireNonNull(value, "orientation"));
    }

    @Override
    protected void invalidated() {
      applyOrientation(Objects.requireNonNull(get(), "orientation"));
      requestLayout();
    }

    @Override
    public Object getBean() {
      return ToggleGroup.this;
    }

    @Override
    public String getName() {
      return "orientation";
    }
  };

  private final ObjectProperty<Toggle.Variant> variant = new ObjectPropertyBase<>(Toggle.Variant.DEFAULT) {
    @Override
    public void set(Toggle.Variant value) {
      super.set(Objects.requireNonNull(value, "variant"));
    }

    @Override
    protected void invalidated() {
      applyVariant(Objects.requireNonNull(get(), "variant"));
      refreshItems();
    }

    @Override
    public Object getBean() {
      return ToggleGroup.this;
    }

    @Override
    public String getName() {
      return "variant";
    }
  };

  private final ObjectProperty<Toggle.Size> size = new ObjectPropertyBase<>(Toggle.Size.DEFAULT) {
    @Override
    public void set(Toggle.Size value) {
      super.set(Objects.requireNonNull(value, "size"));
    }

    @Override
    protected void invalidated() {
      applySize(Objects.requireNonNull(get(), "size"));
      refreshItems();
    }

    @Override
    public Object getBean() {
      return ToggleGroup.this;
    }

    @Override
    public String getName() {
      return "size";
    }
  };

  private final BooleanProperty multiple = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      refreshSelectionMode();
    }

    @Override
    public Object getBean() {
      return ToggleGroup.this;
    }

    @Override
    public String getName() {
      return "multiple";
    }
  };

  public ToggleGroup() {
    this(Orientation.HORIZONTAL);
  }

  public ToggleGroup(ToggleGroupItem... items) {
    this(Orientation.HORIZONTAL, items);
  }

  public ToggleGroup(Orientation orientation, ToggleGroupItem... items) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addListener(this::childrenChanged);
    ((Observable) cssSpacing).addListener(ignored -> applyCssSpacing());
    applyOrientation(Orientation.HORIZONTAL);
    applyVariant(Toggle.Variant.DEFAULT);
    applySize(Toggle.Size.DEFAULT);
    refreshSpacingStyleClass();
    setOrientation(orientation);
    getChildren().addAll(items);
  }

  public final ObjectProperty<Orientation> orientationProperty() {
    return orientation;
  }

  public final Orientation getOrientation() {
    return orientation.get();
  }

  public final void setOrientation(Orientation value) {
    orientation.set(Objects.requireNonNull(value, "orientation"));
  }

  public final ObjectProperty<Toggle.Variant> variantProperty() {
    return variant;
  }

  public final Toggle.Variant getVariant() {
    return variant.get();
  }

  public final void setVariant(Toggle.Variant value) {
    variant.set(Objects.requireNonNull(value, "variant"));
  }

  public final ObjectProperty<Toggle.Size> sizeProperty() {
    return size;
  }

  public final Toggle.Size getSize() {
    return size.get();
  }

  public final void setSize(Toggle.Size value) {
    size.set(Objects.requireNonNull(value, "size"));
  }

  /** Whether more than one item may be selected at a time. */
  public final BooleanProperty multipleProperty() {
    return multiple;
  }

  public final boolean isMultiple() {
    return multiple.get();
  }

  public final void setMultiple(boolean value) {
    multiple.set(value);
  }

  /** Gap between items, in JavaFX logical pixels. The shadcn default spacing of 2 maps to 8px. */
  public final double getSpacing() {
    return spacing.get();
  }

  public final void setSpacing(double value) {
    spacing.set(value);
  }

  public final DoubleProperty spacingProperty() {
    return spacing;
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

  @Override
  protected double computeMinWidth(double height) {
    return computeWidth(height, true);
  }

  @Override
  protected double computePrefWidth(double height) {
    return computeWidth(height, false);
  }

  @Override
  protected double computeMaxWidth(double height) {
    return computePrefWidth(height);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computeHeight(width, true);
  }

  @Override
  protected double computePrefHeight(double width) {
    return computeHeight(width, false);
  }

  @Override
  protected double computeMaxHeight(double width) {
    return computePrefHeight(width);
  }

  @Override
  protected void layoutChildren() {
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double width = Math.max(0.0, getWidth() - x - snappedRightInset());
    double height = Math.max(0.0, getHeight() - y - snappedBottomInset());
    List<Node> children = getManagedChildren();

    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      if (getOrientation() == Orientation.HORIZONTAL) {
        double childWidth = boundedSize(child.prefWidth(height), child.minWidth(height), child.maxWidth(height));
        child.resizeRelocate(x, y, snapSizeX(childWidth), height);
        x += snapSizeX(childWidth);
      } else {
        double childHeight = boundedSize(child.prefHeight(width), child.minHeight(width), child.maxHeight(width));
        child.resizeRelocate(x, y, width, snapSizeY(childHeight));
        y += snapSizeY(childHeight);
      }
      if (index < children.size() - 1) {
        if (getOrientation() == Orientation.HORIZONTAL) {
          x += snappedSpacing();
        } else {
          y += snappedSpacing();
        }
      }
    }
  }

  private double computeWidth(double height, boolean minimum) {
    double contentWidth = 0.0;
    for (Node child : getManagedChildren()) {
      double childWidth = minimum ? child.minWidth(height) : child.prefWidth(height);
      contentWidth =
        getOrientation() == Orientation.HORIZONTAL ? contentWidth + childWidth : Math.max(contentWidth, childWidth);
    }
    if (getOrientation() == Orientation.HORIZONTAL) {
      contentWidth += totalSpacing();
    }
    return snappedLeftInset() + contentWidth + snappedRightInset();
  }

  private double computeHeight(double width, boolean minimum) {
    double contentHeight = 0.0;
    for (Node child : getManagedChildren()) {
      double childHeight = minimum ? child.minHeight(width) : child.prefHeight(width);
      contentHeight =
        getOrientation() == Orientation.VERTICAL ? contentHeight + childHeight : Math.max(contentHeight, childHeight);
    }
    if (getOrientation() == Orientation.VERTICAL) {
      contentHeight += totalSpacing();
    }
    return snappedTopInset() + contentHeight + snappedBottomInset();
  }

  private double totalSpacing() {
    return Math.max(0, getManagedChildren().size() - 1) * snappedSpacing();
  }

  private double snappedSpacing() {
    return getOrientation() == Orientation.HORIZONTAL ? snapSpaceX(getSpacing()) : snapSpaceY(getSpacing());
  }

  private static double boundedSize(double preferred, double minimum, double maximum) {
    return Math.min(Math.max(preferred, minimum), Math.max(minimum, maximum));
  }

  private void childrenChanged(ListChangeListener.Change<? extends Node> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        change.getRemoved().forEach(node -> {
          node.managedProperty().removeListener(managedListener);
          node.focusedProperty().removeListener(focusedListener);
          node.focusVisibleProperty().removeListener(focusVisibleListener);
          restoreViewOrder(node);
          clearItemStyleClasses(node);
          if (node instanceof ToggleGroupItem item && item.getToggleGroup() == exclusiveSelection) {
            item.setToggleGroup(null);
          }
        });
      }
      if (change.wasAdded()) {
        change.getAddedSubList().forEach(node -> {
          node.managedProperty().addListener(managedListener);
          node.focusedProperty().addListener(focusedListener);
          node.focusVisibleProperty().addListener(focusVisibleListener);
        });
      }
    }
    refreshItems();
    refreshFocusedViewOrder();
    requestLayout();
  }

  private void refreshItems() {
    List<Node> managedChildren = getManagedChildren();
    for (Node child : getChildren()) {
      clearItemStyleClasses(child);
      if (child instanceof ToggleGroupItem item) {
        item.setVariant(getVariant());
        item.setSize(getSize());
        item.setToggleGroup(isMultiple() ? null : exclusiveSelection);
      }
    }
    for (int index = 0; index < managedChildren.size(); index++) {
      Node child = managedChildren.get(index);
      child.getStyleClass().add("toggle-group-item");
      if (managedChildren.size() == 1) {
        child.getStyleClass().add("toggle-group-only");
      } else if (index == 0) {
        child.getStyleClass().add("toggle-group-first");
      } else if (index == managedChildren.size() - 1) {
        child.getStyleClass().add("toggle-group-last");
      } else {
        child.getStyleClass().add("toggle-group-middle");
      }
    }
  }

  private void refreshSelectionMode() {
    if (isMultiple()) {
      exclusiveSelection.selectToggle(null);
    }
    refreshItems();
  }

  private void applyOrientation(Orientation selectedOrientation) {
    getStyleClass().removeAll(HORIZONTAL_STYLE_CLASS, VERTICAL_STYLE_CLASS);
    getStyleClass().add(selectedOrientation == Orientation.HORIZONTAL ? HORIZONTAL_STYLE_CLASS : VERTICAL_STYLE_CLASS);
  }

  private void applyVariant(Toggle.Variant selectedVariant) {
    getStyleClass().removeAll("toggle-group-variant-default", "toggle-group-variant-outline");
    getStyleClass().add("toggle-group-variant-" + selectedVariant.name().toLowerCase(java.util.Locale.ROOT));
  }

  private void applySize(Toggle.Size selectedSize) {
    getStyleClass().removeAll("toggle-group-size-default", "toggle-group-size-sm", "toggle-group-size-lg");
    getStyleClass().add("toggle-group-size-" + selectedSize.name().toLowerCase(java.util.Locale.ROOT));
  }

  private void refreshSpacingStyleClass() {
    if (getSpacing() == 0.0) {
      if (!getStyleClass().contains(ZERO_SPACING_STYLE_CLASS)) {
        getStyleClass().add(ZERO_SPACING_STYLE_CLASS);
      }
    } else {
      getStyleClass().remove(ZERO_SPACING_STYLE_CLASS);
    }
  }

  private void applyCssSpacing() {
    if (spacingWasSet || spacing.isBound()) {
      return;
    }
    applyingCssSpacing = true;
    try {
      spacing.set(cssSpacing.getValue().doubleValue());
    } finally {
      applyingCssSpacing = false;
    }
  }

  private static void validateSpacing(double value) {
    if (!Double.isFinite(value) || value < 0.0) {
      throw new IllegalArgumentException("spacing must be a finite, non-negative number");
    }
  }

  private void refreshFocusVisibleViewOrder() {
    refreshFocusedViewOrder();
  }

  private void refreshFocusedViewOrder() {
    for (Node child : getChildren()) {
      if (child.isFocused() || child.isFocusVisible()) {
        unfocusedViewOrders.putIfAbsent(child, child.getViewOrder());
        double frontViewOrder = getChildren()
          .stream()
          .filter(other -> other != child)
          .mapToDouble(Node::getViewOrder)
          .min()
          .orElse(0.0);
        child.setViewOrder(Math.nextDown(frontViewOrder));
      } else {
        restoreViewOrder(child);
      }
    }
  }

  private void restoreViewOrder(Node child) {
    Double viewOrder = unfocusedViewOrders.remove(child);
    if (viewOrder != null) {
      child.setViewOrder(viewOrder);
    }
  }

  private static void clearItemStyleClasses(Node node) {
    node.getStyleClass().removeAll(ITEM_STYLE_CLASSES);
  }
}
