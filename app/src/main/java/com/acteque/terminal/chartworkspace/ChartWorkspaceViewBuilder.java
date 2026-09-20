package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.drawer.Drawer;
import com.acteque.terminal.ui.resizable.ResizableHandle;
import com.acteque.terminal.ui.resizable.ResizablePanel;
import com.acteque.terminal.ui.resizable.ResizablePanelGroup;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Builder;

final class ChartWorkspaceViewBuilder implements Builder<StackPane> {

  private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("workspace-active");

  private final ChartWorkspaceModel model;
  private final Consumer<Chart> chartActivatedHandler;
  private final Map<Chart, ChartContainer> chartContainers = new IdentityHashMap<>();
  private final StackPane root = new StackPane();
  private final StackPane chartLayer = new StackPane();
  private final StackPane menuOverlay;
  private final Drawer inspectorDrawer;
  private final Dialog intervalSelectionDialog;

  ChartWorkspaceViewBuilder(
    ChartWorkspaceModel model,
    Consumer<Chart> chartActivatedHandler,
    Node menu,
    Drawer inspectorDrawer,
    Dialog intervalSelectionDialog
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.chartActivatedHandler = Objects.requireNonNull(chartActivatedHandler, "chartActivatedHandler cannot be null");
    this.inspectorDrawer = Objects.requireNonNull(inspectorDrawer, "inspectorDrawer cannot be null");
    this.intervalSelectionDialog = Objects.requireNonNull(
      intervalSelectionDialog,
      "intervalSelectionDialog cannot be null"
    );
    menuOverlay = new StackPane(Objects.requireNonNull(menu, "menu cannot be null"));
    menuOverlay.getStyleClass().add("chart-workspace-menu-overlay");
    menuOverlay.setMaxHeight(StackPane.USE_PREF_SIZE);
    menuOverlay.setPickOnBounds(false);
    StackPane.setAlignment(menuOverlay, Pos.TOP_CENTER);
    menuOverlay.boundsInParentProperty().addListener((ignored, previous, current) -> positionInspectorBelowMenu());
    root.getStyleClass().add("chart-workspace");
    root.getChildren().setAll(chartLayer, menuOverlay, inspectorDrawer, intervalSelectionDialog);
    model.rootProperty().addListener((ignored, previous, current) -> rebuild());
    model.activeChartProperty().addListener((ignored, previous, current) -> refreshActiveChart());
    model.multipleChartsProperty().addListener((ignored, previous, current) -> refreshActiveChart());
    rebuild();
  }

  @Override
  public StackPane build() {
    return root;
  }

  private void rebuild() {
    List<Node> previous = List.copyOf(chartLayer.getChildren());
    chartLayer.getChildren().clear();
    previous.forEach(ChartWorkspaceViewBuilder::detach);
    chartContainers.clear();
    if (model.getRoot() != null) {
      chartLayer.getChildren().add(build(model.getRoot()));
    }
    refreshActiveChart();
  }

  private void positionInspectorBelowMenu() {
    double menuBottom = Math.max(0.0, menuOverlay.getBoundsInParent().getMaxY());
    StackPane.setMargin(inspectorDrawer, new Insets(menuBottom, 0.0, 0.0, 0.0));
  }

  private Node build(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      Chart chart = leaf.chart();
      ChartContainer container = new ChartContainer(chart.getView(), () -> chartActivatedHandler.accept(chart));
      chartContainers.put(chart, container);
      return new ChartSlot(container);
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
    boolean multipleCharts = model.hasMultipleCharts();
    chartContainers.forEach((chart, container) ->
      container.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, multipleCharts && chart == activeChart)
    );
  }

  private static void detach(Node node) {
    if (node instanceof ChartSlot slot) {
      List<Node> children = List.copyOf(slot.getChildren());
      slot.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
      return;
    }
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

  private static final class ChartSlot extends StackPane {

    private ChartSlot(ChartContainer container) {
      getStyleClass().add("chart-workspace-chart-slot");
      setMinSize(0.0, 0.0);
      getChildren().add(container);
    }
  }

  private static final class ChartContainer extends StackPane {

    private final Rectangle clip = new Rectangle();

    private ChartContainer(StackPane chart, Runnable chartActivatedHandler) {
      getStyleClass().add("chart-workspace-chart");
      setMinSize(0.0, 0.0);
      chart.setMinSize(0.0, 0.0);
      clip.widthProperty().bind(chart.widthProperty());
      clip.heightProperty().bind(chart.heightProperty());
      chart.setClip(clip);
      chart.backgroundProperty().addListener((ignored, previous, current) -> updateClipRadius(chart));
      getChildren().add(chart);
      updateClipRadius(chart);
      addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        if (event.getButton() == MouseButton.PRIMARY) {
          chartActivatedHandler.run();
        }
      });
    }

    private void updateClipRadius(StackPane chart) {
      if (chart.getBackground() == null || chart.getBackground().getFills().isEmpty()) {
        return;
      }
      double radius = chart.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius();
      clip.setArcWidth(radius * 2.0);
      clip.setArcHeight(radius * 2.0);
    }
  }
}
