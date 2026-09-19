package com.acteque.terminal.chartworkspace;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

final class ChartWorkspaceModel {

  private final ReadOnlyObjectWrapper<ChartWorkspaceItem> root = new ReadOnlyObjectWrapper<>(this, "root");

  ChartWorkspaceItem getRoot() {
    return root.get();
  }

  ReadOnlyObjectProperty<ChartWorkspaceItem> rootProperty() {
    return root.getReadOnlyProperty();
  }

  void setRoot(ChartWorkspaceItem value) {
    root.set(value);
  }
}
