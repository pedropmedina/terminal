package com.acteque.terminal.chartworkspace;

import java.util.List;
import java.util.Objects;
import javafx.scene.Node;
import javafx.scene.control.SplitPane;
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
    SplitPane pane = new SplitPane(build(split.first()), build(split.second()));
    pane.getStyleClass().add("chart-workspace-split");
    pane.setOrientation(split.orientation());
    pane.setMinSize(0.0, 0.0);
    pane.setDividerPositions(split.dividerPosition());
    pane
      .getDividers()
      .getFirst()
      .positionProperty()
      .addListener((ignored, previous, current) -> split.setDividerPosition(current.doubleValue()));
    return pane;
  }

  private static void detach(Node node) {
    if (!(node instanceof SplitPane splitPane)) {
      return;
    }
    List<Node> children = List.copyOf(splitPane.getItems());
    splitPane.getItems().clear();
    children.forEach(ChartWorkspaceViewBuilder::detach);
  }
}
