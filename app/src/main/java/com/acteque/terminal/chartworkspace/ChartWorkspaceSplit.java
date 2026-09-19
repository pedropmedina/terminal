package com.acteque.terminal.chartworkspace;

import java.util.Objects;
import javafx.geometry.Orientation;

final class ChartWorkspaceSplit implements ChartWorkspaceItem {

  private final Orientation orientation;
  private final ChartWorkspaceItem first;
  private final ChartWorkspaceItem second;
  private double dividerPosition;

  ChartWorkspaceSplit(Orientation orientation, ChartWorkspaceItem first, ChartWorkspaceItem second) {
    this(orientation, first, second, 0.5);
  }

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

  Orientation orientation() {
    return orientation;
  }

  ChartWorkspaceItem first() {
    return first;
  }

  ChartWorkspaceItem second() {
    return second;
  }

  double dividerPosition() {
    return dividerPosition;
  }

  void setDividerPosition(double value) {
    dividerPosition = value;
  }
}
