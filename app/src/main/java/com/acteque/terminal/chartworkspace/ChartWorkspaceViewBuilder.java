package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.ui.resizable.ResizableHandle;
import com.acteque.terminal.ui.resizable.ResizablePanel;
import com.acteque.terminal.ui.resizable.ResizablePanelGroup;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

final class ChartWorkspaceViewBuilder implements Builder<StackPane> {

  private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("workspace-active");

  private final ChartWorkspaceModel model;
  private final Consumer<Chart> chartActivatedHandler;
  private final Map<Chart, ChartContainer> chartContainers = new IdentityHashMap<>();
  private final StackPane root = new StackPane();

  ChartWorkspaceViewBuilder(ChartWorkspaceModel model, Consumer<Chart> chartActivatedHandler) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.chartActivatedHandler = Objects.requireNonNull(chartActivatedHandler, "chartActivatedHandler cannot be null");
    root.getStyleClass().add("chart-workspace");
    model.rootProperty().addListener((ignored, previous, current) -> rebuild());
    model.activeChartProperty().addListener((ignored, previous, current) -> refreshActiveChart());
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
    chartContainers.clear();
    if (model.getRoot() != null) {
      root.getChildren().add(build(model.getRoot()));
    }
    refreshActiveChart();
  }

  private Node build(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      Chart chart = leaf.chart();
      ChartContainer container = new ChartContainer(chart.getView(), () -> chartActivatedHandler.accept(chart));
      chartContainers.put(chart, container);
      return container;
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

  private void refreshActiveChart() {
    Chart activeChart = model.getActiveChart();
    chartContainers.forEach((chart, container) ->
      container.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, chart == activeChart)
    );
  }

  private static void detach(Node node) {
    if (node instanceof ChartContainer container) {
      container.getChildren().clear();
      return;
    }
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

  private static final class ChartContainer extends StackPane {

    private ChartContainer(StackPane chart, Runnable chartActivatedHandler) {
      getStyleClass().add("chart-workspace-chart");
      setMinSize(0.0, 0.0);
      chart.setMinSize(0.0, 0.0);
      getChildren().add(chart);
      addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        if (event.getButton() == MouseButton.PRIMARY) {
          chartActivatedHandler.run();
        }
      });
    }
  }
}
