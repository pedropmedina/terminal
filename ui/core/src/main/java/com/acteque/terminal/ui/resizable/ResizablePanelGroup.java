package com.acteque.terminal.ui.resizable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;

/** A horizontal or vertical group of panels separated by draggable handles. */
public final class ResizablePanelGroup extends Pane {

  static final double KEYBOARD_INCREMENT = 10.0;
  private static final double DIVIDER_SIZE = 1.0;
  private static final double HANDLE_TARGET_SIZE = 10.0;
  private static final String HORIZONTAL_STYLE_CLASS = "resizable-panel-group-horizontal";
  private static final String VERTICAL_STYLE_CLASS = "resizable-panel-group-vertical";

  private final ObjectProperty<Orientation> orientation = new ObjectPropertyBase<>(Orientation.HORIZONTAL) {
    @Override
    public void set(Orientation value) {
      super.set(Objects.requireNonNull(value, "orientation cannot be null"));
    }

    @Override
    protected void invalidated() {
      applyOrientation(Objects.requireNonNull(get(), "orientation cannot be null"));
      requestLayout();
    }

    @Override
    public Object getBean() {
      return ResizablePanelGroup.this;
    }

    @Override
    public String getName() {
      return "orientation";
    }
  };

  private double[] panelFractions = new double[0];
  private double[] pendingDividerPositions;
  private boolean initialized;
  private ResizableHandle draggingHandle;
  private double dragStartCoordinate;
  private double dragStartLeadingSize;
  private double dragStartTrailingSize;

  public ResizablePanelGroup() {
    this(Orientation.HORIZONTAL);
  }

  public ResizablePanelGroup(Node... children) {
    this(Orientation.HORIZONTAL, children);
  }

  public ResizablePanelGroup(Orientation orientation, Node... children) {
    getStyleClass().add("core-resizable-panel-group");
    setAccessibleRole(AccessibleRole.PARENT);
    getChildren().addListener(this::childrenChanged);
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
    orientation.set(Objects.requireNonNull(value, "orientation cannot be null"));
  }

  /** Returns effective divider positions, normalized to {@code 0..1}. */
  public double[] getDividerPositions() {
    Composition composition = composition();
    if (pendingDividerPositions != null) {
      return pendingDividerPositions.clone();
    }
    double[] positions = new double[composition.handles().size()];
    for (int index = 0; index < positions.length; index++) {
      double position = composition.handles().get(index).getPosition();
      positions[index] = Double.isFinite(position) ? position : (index + 1.0) / composition.panels().size();
    }
    return positions;
  }

  /** Requests normalized divider positions in direct-child handle order. */
  public void setDividerPositions(double... positions) {
    Objects.requireNonNull(positions, "positions cannot be null");
    Composition composition = composition();
    if (positions.length != composition.handles().size()) {
      throw new IllegalArgumentException("positions must contain one value for each resizable handle");
    }
    double previous = Double.NEGATIVE_INFINITY;
    for (double position : positions) {
      if (!Double.isFinite(position) || position < 0.0 || position > 1.0) {
        throw new IllegalArgumentException("divider positions must be finite values between 0 and 1");
      }
      if (position <= previous) {
        throw new IllegalArgumentException("divider positions must be strictly increasing");
      }
      previous = position;
    }
    pendingDividerPositions = positions.clone();
    initialized = false;
    requestLayout();
  }

  @Override
  protected double computeMinWidth(double height) {
    Composition composition = composition();
    if (getOrientation() == Orientation.HORIZONTAL) {
      return (
        composition
          .panels()
          .stream()
          .mapToDouble(panel -> panel.minWidth(height))
          .sum() + dividerExtent(composition)
      );
    }
    return composition
      .panels()
      .stream()
      .mapToDouble(panel -> panel.minWidth(height))
      .max()
      .orElse(0.0);
  }

  @Override
  protected double computeMinHeight(double width) {
    Composition composition = composition();
    if (getOrientation() == Orientation.VERTICAL) {
      return (
        composition
          .panels()
          .stream()
          .mapToDouble(panel -> panel.minHeight(width))
          .sum() + dividerExtent(composition)
      );
    }
    return composition
      .panels()
      .stream()
      .mapToDouble(panel -> panel.minHeight(width))
      .max()
      .orElse(0.0);
  }

  @Override
  protected double computePrefWidth(double height) {
    Composition composition = composition();
    if (getOrientation() == Orientation.HORIZONTAL) {
      return (
        composition
          .panels()
          .stream()
          .mapToDouble(panel -> panel.prefWidth(height))
          .sum() + dividerExtent(composition)
      );
    }
    return composition
      .panels()
      .stream()
      .mapToDouble(panel -> panel.prefWidth(height))
      .max()
      .orElse(0.0);
  }

  @Override
  protected double computePrefHeight(double width) {
    Composition composition = composition();
    if (getOrientation() == Orientation.VERTICAL) {
      return (
        composition
          .panels()
          .stream()
          .mapToDouble(panel -> panel.prefHeight(width))
          .sum() + dividerExtent(composition)
      );
    }
    return composition
      .panels()
      .stream()
      .mapToDouble(panel -> panel.prefHeight(width))
      .max()
      .orElse(0.0);
  }

  @Override
  protected double computeMaxWidth(double height) {
    return Double.MAX_VALUE;
  }

  @Override
  protected double computeMaxHeight(double width) {
    return Double.MAX_VALUE;
  }

  @Override
  protected void layoutChildren() {
    Composition composition = composition();
    if (composition.panels().isEmpty()) {
      return;
    }

    double mainExtent = getOrientation() == Orientation.HORIZONTAL ? getWidth() : getHeight();
    double crossExtent = getOrientation() == Orientation.HORIZONTAL ? getHeight() : getWidth();
    double contentExtent = Math.max(0.0, mainExtent - dividerExtent(composition));
    if (!initialized || panelFractions.length != composition.panels().size()) {
      initializeFractions(composition, mainExtent, contentExtent);
    }

    double[] sizes = allocatePanelSizes(composition.panels(), contentExtent, crossExtent);
    updateFractions(sizes, contentExtent);
    layoutComposition(composition, sizes, mainExtent, crossExtent);
  }

  boolean beginDrag(ResizableHandle handle, MouseEvent event) {
    if (!isInteractive(handle)) {
      return false;
    }
    Composition composition = composition();
    int index = composition.handles().indexOf(handle);
    if (index < 0) {
      return false;
    }
    draggingHandle = handle;
    dragStartCoordinate = coordinate(event);
    dragStartLeadingSize = panelSize(composition.panels().get(index));
    dragStartTrailingSize = panelSize(composition.panels().get(index + 1));
    return true;
  }

  boolean drag(ResizableHandle handle, MouseEvent event) {
    if (draggingHandle != handle || !isInteractive(handle)) {
      return false;
    }
    resizeAdjacent(handle, dragStartLeadingSize, dragStartTrailingSize, coordinate(event) - dragStartCoordinate);
    return true;
  }

  boolean endDrag(ResizableHandle handle) {
    if (draggingHandle != handle) {
      return false;
    }
    draggingHandle = null;
    return true;
  }

  boolean handleKey(ResizableHandle handle, KeyCode code) {
    if (!isInteractive(handle)) {
      return false;
    }
    double delta;
    if (getOrientation() == Orientation.HORIZONTAL) {
      if (code == KeyCode.LEFT) {
        delta = -KEYBOARD_INCREMENT;
      } else if (code == KeyCode.RIGHT) {
        delta = KEYBOARD_INCREMENT;
      } else if (code == KeyCode.HOME) {
        delta = Double.NEGATIVE_INFINITY;
      } else if (code == KeyCode.END) {
        delta = Double.POSITIVE_INFINITY;
      } else {
        return false;
      }
    } else if (code == KeyCode.UP) {
      delta = -KEYBOARD_INCREMENT;
    } else if (code == KeyCode.DOWN) {
      delta = KEYBOARD_INCREMENT;
    } else if (code == KeyCode.HOME) {
      delta = Double.NEGATIVE_INFINITY;
    } else if (code == KeyCode.END) {
      delta = Double.POSITIVE_INFINITY;
    } else {
      return false;
    }
    resizeByKeyboard(handle, delta);
    return true;
  }

  void resizeByKeyboard(ResizableHandle handle, double delta) {
    if (!isInteractive(handle)) {
      return;
    }
    Composition composition = composition();
    int index = composition.handles().indexOf(handle);
    if (index < 0) {
      return;
    }
    ResizablePanel leading = composition.panels().get(index);
    ResizablePanel trailing = composition.panels().get(index + 1);
    resizeAdjacent(handle, panelSize(leading), panelSize(trailing), delta);
  }

  void setHandlePosition(ResizableHandle handle, double requestedPosition) {
    if (!isInteractive(handle) || !Double.isFinite(requestedPosition)) {
      return;
    }
    double mainExtent = getOrientation() == Orientation.HORIZONTAL ? getWidth() : getHeight();
    if (mainExtent <= 0.0) {
      return;
    }
    resizeByKeyboard(handle, requestedPosition * mainExtent - handle.getPosition() * mainExtent);
  }

  double minimumPosition(ResizableHandle handle) {
    return positionBound(handle, false);
  }

  double maximumPosition(ResizableHandle handle) {
    return positionBound(handle, true);
  }

  private void resizeAdjacent(ResizableHandle handle, double leadingSize, double trailingSize, double requestedDelta) {
    Composition composition = composition();
    int index = composition.handles().indexOf(handle);
    if (index < 0) {
      return;
    }
    ResizablePanel leading = composition.panels().get(index);
    ResizablePanel trailing = composition.panels().get(index + 1);
    double crossExtent = getOrientation() == Orientation.HORIZONTAL ? getHeight() : getWidth();
    double minimumDelta = Math.max(
      minimum(leading, crossExtent) - leadingSize,
      trailingSize - maximum(trailing, crossExtent)
    );
    double maximumDelta = Math.min(
      maximum(leading, crossExtent) - leadingSize,
      trailingSize - minimum(trailing, crossExtent)
    );
    double delta = Math.max(minimumDelta, Math.min(maximumDelta, requestedDelta));

    double contentExtent = contentExtent(composition);
    if (contentExtent <= 0.0) {
      return;
    }
    double[] sizes = currentPanelSizes(composition);
    sizes[index] = leadingSize + delta;
    sizes[index + 1] = trailingSize - delta;
    updateFractions(sizes, contentExtent);
    requestLayout();
  }

  private double positionBound(ResizableHandle handle, boolean maximum) {
    Composition composition = composition();
    int index = composition.handles().indexOf(handle);
    double mainExtent = getOrientation() == Orientation.HORIZONTAL ? getWidth() : getHeight();
    if (index < 0 || mainExtent <= 0.0) {
      return maximum ? 1.0 : 0.0;
    }
    ResizablePanel leading = composition.panels().get(index);
    ResizablePanel trailing = composition.panels().get(index + 1);
    double crossExtent = getOrientation() == Orientation.HORIZONTAL ? getHeight() : getWidth();
    double leadingSize = panelSize(leading);
    double trailingSize = panelSize(trailing);
    double delta = maximum
      ? Math.min(maximum(leading, crossExtent) - leadingSize, trailingSize - minimum(trailing, crossExtent))
      : Math.max(minimum(leading, crossExtent) - leadingSize, trailingSize - maximum(trailing, crossExtent));
    return Math.max(0.0, Math.min(1.0, (handle.getPosition() * mainExtent + delta) / mainExtent));
  }

  private void initializeFractions(Composition composition, double mainExtent, double contentExtent) {
    int panelCount = composition.panels().size();
    panelFractions = new double[panelCount];
    if (pendingDividerPositions == null || mainExtent <= 0.0 || contentExtent <= 0.0) {
      Arrays.fill(panelFractions, 1.0 / panelCount);
    } else {
      double cursor = 0.0;
      for (int index = 0; index < pendingDividerPositions.length; index++) {
        double center = pendingDividerPositions[index] * mainExtent;
        panelFractions[index] = Math.max(0.0, center - DIVIDER_SIZE / 2.0 - cursor) / contentExtent;
        cursor = center + DIVIDER_SIZE / 2.0;
      }
      panelFractions[panelCount - 1] = Math.max(0.0, mainExtent - cursor) / contentExtent;
      normalizeFractions();
    }
    pendingDividerPositions = null;
    initialized = true;
  }

  private double[] allocatePanelSizes(List<ResizablePanel> panels, double total, double crossExtent) {
    int count = panels.size();
    double[] minimums = new double[count];
    double[] maximums = new double[count];
    double minimumTotal = 0.0;
    for (int index = 0; index < count; index++) {
      minimums[index] = minimum(panels.get(index), crossExtent);
      maximums[index] = Math.max(minimums[index], maximum(panels.get(index), crossExtent));
      minimumTotal += minimums[index];
    }
    if (minimumTotal > total && minimumTotal > 0.0) {
      double[] sizes = new double[count];
      for (int index = 0; index < count; index++) {
        sizes[index] = (total * minimums[index]) / minimumTotal;
      }
      return sizes;
    }

    double[] sizes = new double[count];
    for (int index = 0; index < count; index++) {
      sizes[index] = Math.max(minimums[index], Math.min(maximums[index], panelFractions[index] * total));
    }
    distributeDifference(sizes, minimums, maximums, panelFractions, total - Arrays.stream(sizes).sum());
    return sizes;
  }

  private static void distributeDifference(
    double[] sizes,
    double[] minimums,
    double[] maximums,
    double[] weights,
    double difference
  ) {
    for (int attempt = 0; attempt < sizes.length && Math.abs(difference) > 0.000_001; attempt++) {
      double totalWeight = 0.0;
      for (int index = 0; index < sizes.length; index++) {
        boolean canChange = difference > 0.0 ? sizes[index] < maximums[index] : sizes[index] > minimums[index];
        if (canChange) {
          totalWeight += weights[index] > 0.0 ? weights[index] : 1.0;
        }
      }
      if (totalWeight == 0.0) {
        return;
      }
      double distributed = 0.0;
      for (int index = 0; index < sizes.length; index++) {
        boolean canChange = difference > 0.0 ? sizes[index] < maximums[index] : sizes[index] > minimums[index];
        if (!canChange) {
          continue;
        }
        double weight = weights[index] > 0.0 ? weights[index] : 1.0;
        double share = (difference * weight) / totalWeight;
        double adjusted = Math.max(minimums[index], Math.min(maximums[index], sizes[index] + share));
        distributed += adjusted - sizes[index];
        sizes[index] = adjusted;
      }
      difference -= distributed;
    }
  }

  private void layoutComposition(Composition composition, double[] sizes, double mainExtent, double crossExtent) {
    double cursor = 0.0;
    for (int index = 0; index < sizes.length; index++) {
      ResizablePanel panel = composition.panels().get(index);
      if (getOrientation() == Orientation.HORIZONTAL) {
        panel.resizeRelocate(cursor, 0.0, sizes[index], crossExtent);
      } else {
        panel.resizeRelocate(0.0, cursor, crossExtent, sizes[index]);
      }
      cursor += sizes[index];

      if (index < composition.handles().size()) {
        ResizableHandle handle = composition.handles().get(index);
        double center = cursor + DIVIDER_SIZE / 2.0;
        if (getOrientation() == Orientation.HORIZONTAL) {
          handle.resizeRelocate(center - HANDLE_TARGET_SIZE / 2.0, 0.0, HANDLE_TARGET_SIZE, crossExtent);
        } else {
          handle.resizeRelocate(0.0, center - HANDLE_TARGET_SIZE / 2.0, crossExtent, HANDLE_TARGET_SIZE);
        }
        handle.updatePosition(mainExtent <= 0.0 ? 0.0 : center / mainExtent);
        cursor += DIVIDER_SIZE;
      }
    }
  }

  private void updateFractions(double[] sizes, double total) {
    if (total <= 0.0) {
      return;
    }
    panelFractions = new double[sizes.length];
    for (int index = 0; index < sizes.length; index++) {
      panelFractions[index] = sizes[index] / total;
    }
    normalizeFractions();
  }

  private void normalizeFractions() {
    double total = Arrays.stream(panelFractions).sum();
    if (total <= 0.0) {
      Arrays.fill(panelFractions, 1.0 / panelFractions.length);
      return;
    }
    for (int index = 0; index < panelFractions.length; index++) {
      panelFractions[index] /= total;
    }
  }

  private double[] currentPanelSizes(Composition composition) {
    return composition.panels().stream().mapToDouble(this::panelSize).toArray();
  }

  private double panelSize(ResizablePanel panel) {
    return getOrientation() == Orientation.HORIZONTAL ? panel.getWidth() : panel.getHeight();
  }

  private double minimum(ResizablePanel panel, double crossExtent) {
    return Math.max(
      0.0,
      getOrientation() == Orientation.HORIZONTAL ? panel.minWidth(crossExtent) : panel.minHeight(crossExtent)
    );
  }

  private double maximum(ResizablePanel panel, double crossExtent) {
    double value =
      getOrientation() == Orientation.HORIZONTAL ? panel.maxWidth(crossExtent) : panel.maxHeight(crossExtent);
    return Double.isFinite(value) ? Math.max(0.0, value) : Double.MAX_VALUE;
  }

  private double contentExtent(Composition composition) {
    double mainExtent = getOrientation() == Orientation.HORIZONTAL ? getWidth() : getHeight();
    return Math.max(0.0, mainExtent - dividerExtent(composition));
  }

  private static double dividerExtent(Composition composition) {
    return composition.handles().size() * DIVIDER_SIZE;
  }

  private double coordinate(MouseEvent event) {
    return getOrientation() == Orientation.HORIZONTAL ? event.getSceneX() : event.getSceneY();
  }

  private boolean isInteractive(ResizableHandle handle) {
    return !isDisabled() && !handle.isDisabled() && handle.getParent() == this;
  }

  private void childrenChanged(ListChangeListener.Change<? extends Node> change) {
    while (change.next()) {
      change
        .getRemoved()
        .stream()
        .filter(ResizableHandle.class::isInstance)
        .map(ResizableHandle.class::cast)
        .forEach(handle -> handle.attach(null));
      change
        .getAddedSubList()
        .stream()
        .filter(ResizableHandle.class::isInstance)
        .map(ResizableHandle.class::cast)
        .forEach(handle -> {
          handle.attach(this);
          handle.applyOrientation(getOrientation());
        });
    }
    initialized = false;
    pendingDividerPositions = null;
    draggingHandle = null;
    requestLayout();
  }

  private void applyOrientation(Orientation value) {
    getStyleClass().removeAll(HORIZONTAL_STYLE_CLASS, VERTICAL_STYLE_CLASS);
    getStyleClass().add(value == Orientation.HORIZONTAL ? HORIZONTAL_STYLE_CLASS : VERTICAL_STYLE_CLASS);
    getChildren()
      .stream()
      .filter(ResizableHandle.class::isInstance)
      .map(ResizableHandle.class::cast)
      .forEach(handle -> handle.applyOrientation(value));
  }

  private Composition composition() {
    List<Node> children = getChildren();
    if (children.isEmpty()) {
      return new Composition(List.of(), List.of());
    }
    if (children.size() % 2 == 0) {
      throw new IllegalStateException("resizable children must alternate panel, handle, panel");
    }
    List<ResizablePanel> panels = new ArrayList<>((children.size() + 1) / 2);
    List<ResizableHandle> handles = new ArrayList<>(children.size() / 2);
    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      if (index % 2 == 0 && child instanceof ResizablePanel panel) {
        panels.add(panel);
      } else if (index % 2 == 1 && child instanceof ResizableHandle handle) {
        handles.add(handle);
      } else {
        throw new IllegalStateException("resizable children must alternate panel, handle, panel");
      }
    }
    return new Composition(List.copyOf(panels), List.copyOf(handles));
  }

  private record Composition(List<ResizablePanel> panels, List<ResizableHandle> handles) {}
}
