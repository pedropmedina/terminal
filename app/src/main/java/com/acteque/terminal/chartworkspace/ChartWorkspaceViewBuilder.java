package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.ui.resizable.ResizableHandle;
import com.acteque.terminal.ui.resizable.ResizablePanel;
import com.acteque.terminal.ui.resizable.ResizablePanelGroup;
import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

final class ChartWorkspaceViewBuilder implements Builder<StackPane> {

  private final ChartWorkspaceModel model;
  private final StackPane root = new StackPane();

  ChartWorkspaceViewBuilder(ChartWorkspaceModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    root.getStyleClass().add("chart-workspace");
    model.rootProperty().addListener((ignored, previous, current) -> rebuild());
    rebuild();
  }

  @Override
  public StackPane build() {
    return root;
  }

  private void rebuild() {
    List<Node> previous = List.copyOf(root.getChildren());
    root.getChildren().clear();
    previous.forEach(ChartWorkspaceViewBuilder::detach);
    if (model.getRoot() != null) {
      root.getChildren().add(build(model.getRoot()));
    }
  }

  private Node build(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      StackPane chart = leaf.chart().getView();
      chart.setMinSize(0.0, 0.0);
      return chart;
    }

    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    ResizableHandle handle = new ResizableHandle(true);
    ResizablePanelGroup group = new ResizablePanelGroup(
      split.orientation(),
      new ResizablePanel(build(split.first())),
      handle,
      new ResizablePanel(build(split.second()))
    );
    group.getStyleClass().add("chart-workspace-split");
    group.setMinSize(0.0, 0.0);
    group.setDividerPositions(split.dividerPosition());
    handle
      .positionProperty()
      .addListener((ignored, previous, current) -> split.setDividerPosition(current.doubleValue()));
    return group;
  }

  private static void detach(Node node) {
    if (node instanceof ResizablePanelGroup group) {
      List<Node> children = List.copyOf(group.getChildren());
      group.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
      return;
    }
    if (node instanceof ResizablePanel panel) {
      List<Node> children = List.copyOf(panel.getChildren());
      panel.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
    }
  }
}
