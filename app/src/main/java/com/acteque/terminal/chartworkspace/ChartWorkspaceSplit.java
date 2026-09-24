package com.acteque.terminal.chartworkspace;

import java.util.Objects;
import javafx.geometry.Orientation;

/** A branch workspace-tree node containing two resizable child items. */
final class ChartWorkspaceSplit implements ChartWorkspaceItem {

  private final Orientation orientation;
  private final ChartWorkspaceItem first;
  private final ChartWorkspaceItem second;
  private double dividerPosition;

  /**
   * Creates an evenly divided workspace branch.
   *
   * @param orientation the child layout orientation
   * @param first the first child item
   * @param second the second child item
   */
  ChartWorkspaceSplit(Orientation orientation, ChartWorkspaceItem first, ChartWorkspaceItem second) {
    this(orientation, first, second, 0.5);
  }

  /**
   * Creates a workspace branch with a retained divider position.
   *
   * @param orientation the child layout orientation
   * @param first the first child item
   * @param second the second child item
   * @param dividerPosition the proportional divider position
   */
  ChartWorkspaceSplit(
    Orientation orientation,
    ChartWorkspaceItem first,
    ChartWorkspaceItem second,
    double dividerPosition
  ) {
    this.orientation = Objects.requireNonNull(orientation, "orientation cannot be null");
    this.first = Objects.requireNonNull(first, "first cannot be null");
    this.second = Objects.requireNonNull(second, "second cannot be null");
    this.dividerPosition = dividerPosition;
  }

  /**
   * Returns the child layout orientation.
   *
   * @return the split orientation
   */
  Orientation orientation() {
    return orientation;
  }

  /**
   * Returns the first child item.
   *
   * @return the first child
   */
  ChartWorkspaceItem first() {
    return first;
  }

  /**
   * Returns the second child item.
   *
   * @return the second child
   */
  ChartWorkspaceItem second() {
    return second;
  }

  /**
   * Returns the proportional divider position.
   *
   * @return the divider position
   */
  double dividerPosition() {
    return dividerPosition;
  }

  /**
   * Retains the proportional divider position after user resizing.
   *
   * @param value the divider position
   */
  void setDividerPosition(double value) {
    dividerPosition = value;
  }
}
