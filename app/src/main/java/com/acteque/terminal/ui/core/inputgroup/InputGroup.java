package com.acteque.terminal.ui.core.inputgroup;

import java.util.ArrayList;
import java.util.List;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Control;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.Pane;

/** A composable control that visually joins text input with supporting content and actions. */
public final class InputGroup extends Pane {

  private static final String ROOT_STYLE_CLASS = "core-input-group";
  private static final String WITH_INPUT_STYLE_CLASS = "with-input";
  private static final String WITH_TEXTAREA_STYLE_CLASS = "with-textarea";
  private static final String HAS_INLINE_START_STYLE_CLASS = "has-inline-start";
  private static final String HAS_INLINE_END_STYLE_CLASS = "has-inline-end";
  private static final String HAS_BLOCK_START_STYLE_CLASS = "has-block-start";
  private static final String HAS_BLOCK_END_STYLE_CLASS = "has-block-end";
  private static final List<String> STRUCTURAL_STYLE_CLASSES = List.of(
    WITH_INPUT_STYLE_CLASS,
    WITH_TEXTAREA_STYLE_CLASS,
    HAS_INLINE_START_STYLE_CLASS,
    HAS_INLINE_END_STYLE_CLASS,
    HAS_BLOCK_START_STYLE_CLASS,
    HAS_BLOCK_END_STYLE_CLASS
  );
  private static final PseudoClass CONTROL_FOCUS_VISIBLE_PSEUDO_CLASS = PseudoClass.getPseudoClass(
    "control-focus-visible"
  );
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass CONTAINS_DISABLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("contains-disabled");

  private final List<Runnable> detachObservers = new ArrayList<>();
  private final InvalidationListener structureListener = ignored -> refreshStructure();
  private final InvalidationListener stateListener = ignored -> {
    refreshDerivedState();
    requestLayout();
  };
  private boolean refreshingStructure;

  public InputGroup() {
    this(new Node[0]);
  }

  public InputGroup(Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addAll(children);
    refreshStructure();
  }

  /** Returns the first managed input-group control, or {@code null} when none is present. */
  public TextInputControl getControl() {
    return getManagedChildren()
      .stream()
      .filter(InputGroupControl.class::isInstance)
      .map(TextInputControl.class::cast)
      .findFirst()
      .orElse(null);
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
  protected double computeMaxWidth(double height) {
    return Double.MAX_VALUE;
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
  protected double computeMaxHeight(double width) {
    return getManagedChildren().stream().anyMatch(InputGroupTextarea.class::isInstance)
      ? Double.MAX_VALUE
      : computePrefHeight(width);
  }

  @Override
  protected void layoutChildren() {
    LayoutParts parts = layoutParts();
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double contentWidth = Math.max(0.0, getWidth() - x - snappedRightInset());
    double contentHeight = Math.max(0.0, getHeight() - y - snappedBottomInset());

    double blockStartHeight = layoutBlockChildren(parts.blockStart(), x, y, contentWidth);
    y += blockStartHeight;

    double blockEndHeight = preferredBlockHeight(parts.blockEnd(), contentWidth, false);
    double rowHeight = Math.max(0.0, contentHeight - blockStartHeight - blockEndHeight);
    layoutInlineRow(parts, x, y, contentWidth, rowHeight);
    y += rowHeight;

    layoutBlockChildren(parts.blockEnd(), x, y, contentWidth);
  }

  void requestControlFocus() {
    Node control = getControl();
    if (control != null && !control.isDisabled()) {
      control.requestFocus();
    }
  }

  private void refreshStructure() {
    if (refreshingStructure) {
      return;
    }
    refreshingStructure = true;
    try {
      detachObservers.forEach(Runnable::run);
      detachObservers.clear();
      observeChildren(this);
      for (Node child : getChildren()) {
        observeSubtree(child);
      }
      refreshDerivedState();
      requestLayout();
    } finally {
      refreshingStructure = false;
    }
  }

  private void observeSubtree(Node node) {
    observe(node.disabledProperty(), stateListener);
    observe(node.managedProperty(), stateListener);
    if (node instanceof InputGroupControl control) {
      observe(control.invalidProperty(), stateListener);
      observe(node.focusVisibleProperty(), stateListener);
    }
    if (node instanceof InputGroupAddon addon) {
      observe(addon.alignmentPositionProperty(), stateListener);
    }
    if (node instanceof Parent parent && !(node instanceof Control)) {
      observeChildren(parent);
      for (Node child : parent.getChildrenUnmodifiable()) {
        observeSubtree(child);
      }
    }
  }

  private void observeChildren(Parent parent) {
    ObservableList<Node> children = parent.getChildrenUnmodifiable();
    children.addListener(structureListener);
    detachObservers.add(() -> children.removeListener(structureListener));
  }

  private void observe(Observable observable, InvalidationListener listener) {
    observable.addListener(listener);
    detachObservers.add(() -> observable.removeListener(listener));
  }

  private void refreshDerivedState() {
    List<Node> managedChildren = getManagedChildren();
    boolean controlFocusVisible = managedChildren
      .stream()
      .filter(InputGroupControl.class::isInstance)
      .anyMatch(Node::isFocusVisible);
    boolean invalid = managedChildren
      .stream()
      .filter(InputGroupControl.class::isInstance)
      .map(InputGroupControl.class::cast)
      .anyMatch(InputGroupControl::isInvalid);
    boolean containsDisabled = getChildren().stream().anyMatch(this::containsDisabledNode);

    pseudoClassStateChanged(CONTROL_FOCUS_VISIBLE_PSEUDO_CLASS, controlFocusVisible);
    pseudoClassStateChanged(INVALID_PSEUDO_CLASS, invalid);
    pseudoClassStateChanged(CONTAINS_DISABLED_PSEUDO_CLASS, containsDisabled);

    getStyleClass().removeAll(STRUCTURAL_STYLE_CLASSES);
    if (managedChildren.stream().anyMatch(InputGroupInput.class::isInstance)) {
      getStyleClass().add(WITH_INPUT_STYLE_CLASS);
    }
    if (managedChildren.stream().anyMatch(InputGroupTextarea.class::isInstance)) {
      getStyleClass().add(WITH_TEXTAREA_STYLE_CLASS);
    }
    addAlignmentStyleClass(managedChildren, InputGroupAlignment.INLINE_START, HAS_INLINE_START_STYLE_CLASS);
    addAlignmentStyleClass(managedChildren, InputGroupAlignment.INLINE_END, HAS_INLINE_END_STYLE_CLASS);
    addAlignmentStyleClass(managedChildren, InputGroupAlignment.BLOCK_START, HAS_BLOCK_START_STYLE_CLASS);
    addAlignmentStyleClass(managedChildren, InputGroupAlignment.BLOCK_END, HAS_BLOCK_END_STYLE_CLASS);
  }

  private boolean containsDisabledNode(Node node) {
    if (node.isDisabled()) {
      return true;
    }
    return (
      node instanceof Parent parent &&
      !(node instanceof Control) &&
      parent.getChildrenUnmodifiable().stream().anyMatch(this::containsDisabledNode)
    );
  }

  private void addAlignmentStyleClass(List<Node> children, InputGroupAlignment alignment, String styleClass) {
    boolean present = children
      .stream()
      .filter(InputGroupAddon.class::isInstance)
      .map(InputGroupAddon.class::cast)
      .anyMatch(addon -> addon.getAlignmentPosition() == alignment);
    if (present) {
      getStyleClass().add(styleClass);
    }
  }

  private double computeContentWidth(double height, boolean minimum) {
    LayoutParts parts = layoutParts();
    double inlineWidth = sumWidths(parts.inlineStart(), height, minimum);
    inlineWidth += sumWidths(parts.controls(), height, minimum);
    inlineWidth += sumWidths(parts.inlineEnd(), height, minimum);
    double blockWidth = Math.max(
      maximumWidth(parts.blockStart(), height, minimum),
      maximumWidth(parts.blockEnd(), height, minimum)
    );
    return snappedLeftInset() + Math.max(inlineWidth, blockWidth) + snappedRightInset();
  }

  private double computeContentHeight(double width, boolean minimum) {
    LayoutParts parts = layoutParts();
    double contentWidth =
      width < 0.0
        ? Math.max(0.0, computeContentWidth(-1.0, minimum) - snappedLeftInset() - snappedRightInset())
        : Math.max(0.0, width - snappedLeftInset() - snappedRightInset());
    double blockStartHeight = preferredBlockHeight(parts.blockStart(), contentWidth, minimum);
    double blockEndHeight = preferredBlockHeight(parts.blockEnd(), contentWidth, minimum);
    double inlineAddonWidth =
      sumWidths(parts.inlineStart(), -1.0, minimum) + sumWidths(parts.inlineEnd(), -1.0, minimum);
    double controlWidth = Math.max(0.0, contentWidth - inlineAddonWidth);
    double rowHeight = maximumHeight(parts.inlineStart(), -1.0, minimum);
    rowHeight = Math.max(rowHeight, maximumHeight(parts.inlineEnd(), -1.0, minimum));
    rowHeight = Math.max(rowHeight, maximumHeight(parts.controls(), controlWidth, minimum));
    return snappedTopInset() + blockStartHeight + rowHeight + blockEndHeight + snappedBottomInset();
  }

  private double layoutBlockChildren(List<Node> children, double x, double y, double width) {
    double consumedHeight = 0.0;
    for (Node child : children) {
      double childHeight = boundedSize(child.prefHeight(width), child.minHeight(width), child.maxHeight(width));
      child.resizeRelocate(x, y + consumedHeight, width, snapSizeY(childHeight));
      consumedHeight += snapSizeY(childHeight);
    }
    return consumedHeight;
  }

  private void layoutInlineRow(LayoutParts parts, double x, double y, double width, double height) {
    double leadingWidth = layoutInlineChildren(parts.inlineStart(), x, y, height, false);
    double trailingWidth = snappedInlineWidth(parts.inlineEnd(), height);
    double controlsWidth = Math.max(0.0, width - leadingWidth - trailingWidth);
    double controlX = x + leadingWidth;
    double remainingControlWidth = controlsWidth;
    for (int index = 0; index < parts.controls().size(); index++) {
      Node control = parts.controls().get(index);
      int remainingControls = parts.controls().size() - index;
      double controlWidth =
        remainingControls == 1 ? remainingControlWidth : snapSizeX(remainingControlWidth / remainingControls);
      layoutInlineChild(control, controlX, y, controlWidth, height);
      controlX += controlWidth;
      remainingControlWidth -= controlWidth;
    }
    layoutInlineChildren(parts.inlineEnd(), x + width - trailingWidth, y, height, false);
  }

  private double layoutInlineChildren(List<Node> children, double x, double y, double height, boolean minimum) {
    double consumedWidth = 0.0;
    for (Node child : children) {
      double preferred = minimum ? child.minWidth(height) : child.prefWidth(height);
      double childWidth = boundedSize(preferred, child.minWidth(height), child.maxWidth(height));
      layoutInlineChild(child, x + consumedWidth, y, childWidth, height);
      consumedWidth += snapSizeX(childWidth);
    }
    return consumedWidth;
  }

  private double snappedInlineWidth(List<Node> children, double height) {
    return children
      .stream()
      .mapToDouble(child -> boundedSize(child.prefWidth(height), child.minWidth(height), child.maxWidth(height)))
      .map(this::snapSizeX)
      .sum();
  }

  private void layoutInlineChild(Node child, double x, double y, double width, double rowHeight) {
    double height = boundedSize(rowHeight, child.minHeight(width), child.maxHeight(width));
    double childY = y + Math.max(0.0, (rowHeight - height) / 2.0);
    child.resizeRelocate(x, childY, snapSizeX(width), snapSizeY(height));
  }

  private double preferredBlockHeight(List<Node> children, double width, boolean minimum) {
    return children
      .stream()
      .mapToDouble(child -> minimum ? child.minHeight(width) : child.prefHeight(width))
      .sum();
  }

  private static double sumWidths(List<Node> children, double height, boolean minimum) {
    return children
      .stream()
      .mapToDouble(child -> minimum ? child.minWidth(height) : child.prefWidth(height))
      .sum();
  }

  private static double maximumWidth(List<Node> children, double height, boolean minimum) {
    return children
      .stream()
      .mapToDouble(child -> minimum ? child.minWidth(height) : child.prefWidth(height))
      .max()
      .orElse(0.0);
  }

  private static double maximumHeight(List<Node> children, double width, boolean minimum) {
    return children
      .stream()
      .mapToDouble(child -> minimum ? child.minHeight(width) : child.prefHeight(width))
      .max()
      .orElse(0.0);
  }

  private static double boundedSize(double preferred, double minimum, double maximum) {
    return Math.min(Math.max(preferred, minimum), Math.max(minimum, maximum));
  }

  private LayoutParts layoutParts() {
    List<Node> inlineStart = new ArrayList<>();
    List<Node> inlineEnd = new ArrayList<>();
    List<Node> blockStart = new ArrayList<>();
    List<Node> blockEnd = new ArrayList<>();
    List<Node> controls = new ArrayList<>();

    for (Node child : getManagedChildren()) {
      if (child instanceof InputGroupAddon addon) {
        switch (addon.getAlignmentPosition()) {
          case INLINE_START -> inlineStart.add(addon);
          case INLINE_END -> inlineEnd.add(addon);
          case BLOCK_START -> blockStart.add(addon);
          case BLOCK_END -> blockEnd.add(addon);
        }
      } else {
        controls.add(child);
      }
    }
    return new LayoutParts(inlineStart, inlineEnd, blockStart, blockEnd, controls);
  }

  private record LayoutParts(
    List<Node> inlineStart,
    List<Node> inlineEnd,
    List<Node> blockStart,
    List<Node> blockEnd,
    List<Node> controls
  ) {}
}
