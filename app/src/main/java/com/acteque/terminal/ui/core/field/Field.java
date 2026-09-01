package com.acteque.terminal.ui.core.field;

import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/** A composable form-field primitive modeled after shadcn/ui's Field family. */
public final class Field extends Pane {

  private static final StyleablePropertyFactory<Field> STYLEABLES = new StyleablePropertyFactory<>(
    Pane.getClassCssMetaData()
  );
  private static final String ROOT_STYLE_CLASS = "core-field";
  private static final String VERTICAL_STYLE_CLASS = "field-orientation-vertical";
  private static final String HORIZONTAL_STYLE_CLASS = "field-orientation-horizontal";
  private static final String RESPONSIVE_STYLE_CLASS = "field-orientation-responsive";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final double RESPONSIVE_BREAKPOINT = 448.0;

  private final StyleableProperty<Number> spacing = STYLEABLES.createStyleableNumberProperty(
    this,
    "spacing",
    "-fx-spacing",
    field -> field.spacing,
    8.0
  );

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Field.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  private final ObjectProperty<FieldOrientation> orientation = new ObjectPropertyBase<>(FieldOrientation.VERTICAL) {
    @Override
    public void set(FieldOrientation value) {
      super.set(Objects.requireNonNull(value, "orientation"));
    }

    @Override
    protected void invalidated() {
      applyOrientation(Objects.requireNonNull(get(), "orientation"));
      requestLayout();
    }

    @Override
    public Object getBean() {
      return Field.this;
    }

    @Override
    public String getName() {
      return "orientation";
    }
  };

  public Field() {
    this(FieldOrientation.VERTICAL);
  }

  public Field(Node... children) {
    this(FieldOrientation.VERTICAL, children);
  }

  public Field(FieldOrientation orientation, Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setAccessibleRole(AccessibleRole.PARENT);
    ((Observable) spacing).addListener(ignored -> requestLayout());
    applyOrientation(FieldOrientation.VERTICAL);
    setOrientation(orientation);
    getChildren().addAll(children);
  }

  public final ObjectProperty<FieldOrientation> orientationProperty() {
    return orientation;
  }

  public final FieldOrientation getOrientation() {
    return orientation.get();
  }

  public final void setOrientation(FieldOrientation value) {
    orientation.set(Objects.requireNonNull(value, "orientation"));
  }

  public final boolean isInvalid() {
    return invalid.get();
  }

  public final void setInvalid(boolean value) {
    invalid.set(value);
  }

  public final BooleanProperty invalidProperty() {
    return invalid;
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

  @Override
  protected double computeMinWidth(double height) {
    return computeContentWidth(height, true);
  }

  @Override
  protected double computePrefWidth(double height) {
    return computeContentWidth(height, false);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computeContentHeight(width, true);
  }

  @Override
  protected double computePrefHeight(double width) {
    return computeContentHeight(width, false);
  }

  @Override
  protected void layoutChildren() {
    if (usesHorizontalLayout(getWidth())) {
      layoutHorizontally();
    } else {
      layoutVertically();
    }
  }

  private void layoutVertically() {
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double width = Math.max(0.0, getWidth() - x - snappedRightInset());
    List<Node> children = getManagedChildren();
    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      double height = boundedSize(child.prefHeight(width), child.minHeight(width), child.maxHeight(width));
      child.resizeRelocate(x, y, width, snapSizeY(height));
      y += snapSizeY(height);
      if (index < children.size() - 1) {
        y += snappedSpacingY();
      }
    }
  }

  private void layoutHorizontally() {
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double width = Math.max(0.0, getWidth() - x - snappedRightInset());
    double height = Math.max(0.0, getHeight() - y - snappedBottomInset());
    List<Node> children = getManagedChildren();
    double available = Math.max(0.0, width - Math.max(0, children.size() - 1) * snappedSpacingX());
    double fixedWidth = children
      .stream()
      .filter(node -> !(node instanceof FieldContent))
      .mapToDouble(node -> node.prefWidth(height))
      .sum();
    long flexibleChildren = children.stream().filter(FieldContent.class::isInstance).count();
    double flexibleWidth = flexibleChildren == 0 ? 0.0 : Math.max(0.0, available - fixedWidth) / flexibleChildren;
    boolean alignAtTop = flexibleChildren > 0;

    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      double childWidth = child instanceof FieldContent ? flexibleWidth : child.prefWidth(height);
      childWidth = boundedSize(childWidth, child.minWidth(height), child.maxWidth(height));
      double childHeight = boundedSize(
        child.prefHeight(childWidth),
        child.minHeight(childWidth),
        child.maxHeight(childWidth)
      );
      double childY = alignAtTop ? y : y + Math.max(0.0, (height - childHeight) / 2.0);
      child.resizeRelocate(x, childY, snapSizeX(childWidth), snapSizeY(childHeight));
      x += snapSizeX(childWidth);
      if (index < children.size() - 1) {
        x += snappedSpacingX();
      }
    }
  }

  private double computeContentWidth(double height, boolean minimum) {
    boolean horizontal = getOrientation() == FieldOrientation.HORIZONTAL;
    double content = 0.0;
    for (Node child : getManagedChildren()) {
      double childWidth = minimum ? child.minWidth(height) : child.prefWidth(height);
      content = horizontal ? content + childWidth : Math.max(content, childWidth);
    }
    if (horizontal) {
      content += Math.max(0, getManagedChildren().size() - 1) * snappedSpacingX();
    }
    return snappedLeftInset() + content + snappedRightInset();
  }

  private double computeContentHeight(double width, boolean minimum) {
    boolean horizontal = usesHorizontalLayout(width);
    double content = 0.0;
    for (Node child : getManagedChildren()) {
      double childHeight = minimum ? child.minHeight(width) : child.prefHeight(width);
      content = horizontal ? Math.max(content, childHeight) : content + childHeight;
    }
    if (!horizontal) {
      content += Math.max(0, getManagedChildren().size() - 1) * snappedSpacingY();
    }
    return snappedTopInset() + content + snappedBottomInset();
  }

  private boolean usesHorizontalLayout(double width) {
    return (
      getOrientation() == FieldOrientation.HORIZONTAL ||
      (getOrientation() == FieldOrientation.RESPONSIVE && width >= RESPONSIVE_BREAKPOINT)
    );
  }

  private double snappedSpacingX() {
    return snapSpaceX(spacing.getValue().doubleValue());
  }

  private double snappedSpacingY() {
    return snapSpaceY(spacing.getValue().doubleValue());
  }

  private static double boundedSize(double preferred, double minimum, double maximum) {
    return Math.min(Math.max(preferred, minimum), Math.max(minimum, maximum));
  }

  private void applyOrientation(FieldOrientation selectedOrientation) {
    getStyleClass().removeAll(VERTICAL_STYLE_CLASS, HORIZONTAL_STYLE_CLASS, RESPONSIVE_STYLE_CLASS);
    getStyleClass().add(selectedOrientation.styleClass());
  }
}
