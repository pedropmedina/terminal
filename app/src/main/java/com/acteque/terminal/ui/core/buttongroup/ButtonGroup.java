package com.acteque.terminal.ui.core.buttongroup;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/** A compact container that visually joins related controls. */
public final class ButtonGroup extends Pane {

  private static final StyleablePropertyFactory<ButtonGroup> STYLEABLES = new StyleablePropertyFactory<>(
    Pane.getClassCssMetaData()
  );
  private static final String ROOT_STYLE_CLASS = "core-button-group";
  private static final String HORIZONTAL_STYLE_CLASS = "button-group-horizontal";
  private static final String VERTICAL_STYLE_CLASS = "button-group-vertical";
  private static final String NESTED_STYLE_CLASS = "button-group-nested";
  private static final List<String> ITEM_STYLE_CLASSES = List.of(
    "button-group-item",
    "button-group-first",
    "button-group-middle",
    "button-group-last",
    "button-group-only"
  );
  private final InvalidationListener managedListener = ignored -> refreshItemStyleClasses();
  private final InvalidationListener focusVisibleListener = ignored -> refreshFocusVisibleViewOrder();
  private final Map<Node, Double> unfocusedViewOrders = new IdentityHashMap<>();
  private final StyleableProperty<Number> spacing = STYLEABLES.createStyleableNumberProperty(
    this,
    "spacing",
    "-fx-spacing",
    group -> group.spacing,
    0.0
  );

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
      return ButtonGroup.this;
    }

    @Override
    public String getName() {
      return "orientation";
    }
  };

  public ButtonGroup() {
    this(Orientation.HORIZONTAL);
  }

  public ButtonGroup(Node... children) {
    this(Orientation.HORIZONTAL, children);
  }

  public ButtonGroup(Orientation orientation, Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    getChildren().addListener(this::childrenChanged);
    ((Observable) spacing).addListener(ignored -> requestLayout());
    applyOrientation(Orientation.HORIZONTAL);
    setOrientation(orientation);
    getChildren().addAll(children);
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
    return getOrientation() == Orientation.HORIZONTAL
      ? snapSpaceX(spacing.getValue().doubleValue())
      : snapSpaceY(spacing.getValue().doubleValue());
  }

  private static double boundedSize(double preferred, double minimum, double maximum) {
    return Math.min(Math.max(preferred, minimum), Math.max(minimum, maximum));
  }

  private void childrenChanged(ListChangeListener.Change<? extends Node> change) {
    while (change.next()) {
      if (change.wasRemoved()) {
        change.getRemoved().forEach(node -> {
          node.managedProperty().removeListener(managedListener);
          node.focusVisibleProperty().removeListener(focusVisibleListener);
          restoreViewOrder(node);
          clearItemStyleClasses(node);
        });
      }
      if (change.wasAdded()) {
        change.getAddedSubList().forEach(node -> {
          node.managedProperty().addListener(managedListener);
          node.focusVisibleProperty().addListener(focusVisibleListener);
        });
      }
    }
    refreshNestedStyleClass();
    refreshItemStyleClasses();
    refreshFocusVisibleViewOrder();
  }

  private void refreshNestedStyleClass() {
    boolean nested = getChildren().stream().anyMatch(ButtonGroup.class::isInstance);
    if (nested && !getStyleClass().contains(NESTED_STYLE_CLASS)) {
      getStyleClass().add(NESTED_STYLE_CLASS);
    } else if (!nested) {
      getStyleClass().remove(NESTED_STYLE_CLASS);
    }
  }

  private void refreshFocusVisibleViewOrder() {
    for (Node child : getChildren()) {
      if (child.isFocusVisible()) {
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

  private void applyOrientation(Orientation selectedOrientation) {
    getStyleClass().removeAll(HORIZONTAL_STYLE_CLASS, VERTICAL_STYLE_CLASS);
    getStyleClass().add(selectedOrientation == Orientation.HORIZONTAL ? HORIZONTAL_STYLE_CLASS : VERTICAL_STYLE_CLASS);
    refreshItemStyleClasses();
  }

  private void refreshItemStyleClasses() {
    List<Node> managedChildren = getManagedChildren();
    for (Node child : getChildren()) {
      clearItemStyleClasses(child);
    }
    for (int index = 0; index < managedChildren.size(); index++) {
      Node child = managedChildren.get(index);
      child.getStyleClass().add("button-group-item");
      if (managedChildren.size() == 1) {
        child.getStyleClass().add("button-group-only");
      } else if (index == 0) {
        child.getStyleClass().add("button-group-first");
      } else if (index == managedChildren.size() - 1) {
        child.getStyleClass().add("button-group-last");
      } else {
        child.getStyleClass().add("button-group-middle");
      }
    }
  }

  private static void clearItemStyleClasses(Node node) {
    node.getStyleClass().removeAll(ITEM_STYLE_CLASSES);
  }
}
