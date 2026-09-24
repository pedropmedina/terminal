package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chart.ChartType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.random.RandomGenerator;
import javafx.geometry.Orientation;
import javafx.scene.paint.Color;

/** Applies chart-workspace state transitions and owns chart lifecycles independently of layout. */
final class ChartWorkspaceInteractor implements AutoCloseable {

  private static final System.Logger LOGGER = System.getLogger(ChartWorkspaceInteractor.class.getName());

  private final ChartWorkspaceModel model;
  private final ChartWorkspaceChartFactory chartFactory;
  private final ChartIdentifierColorGenerator identifierColorGenerator;
  private final Map<Chart, Color> identifierColors = new IdentityHashMap<>();
  private boolean started;
  private boolean closed;

  /**
   * Creates an interactor using the default random source for identifier colors.
   *
   * @param model the observable workspace state
   * @param chartFactory the factory for workspace-owned charts
   */
  ChartWorkspaceInteractor(ChartWorkspaceModel model, ChartWorkspaceChartFactory chartFactory) {
    this(model, chartFactory, RandomGenerator.getDefault());
  }

  /**
   * Creates an interactor with an injectable identifier-color random source.
   *
   * @param model the observable workspace state
   * @param chartFactory the factory for workspace-owned charts
   * @param randomGenerator the random source for identifier colors
   */
  ChartWorkspaceInteractor(
    ChartWorkspaceModel model,
    ChartWorkspaceChartFactory chartFactory,
    RandomGenerator randomGenerator
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.chartFactory = Objects.requireNonNull(chartFactory, "chartFactory cannot be null");
    identifierColorGenerator = new ChartIdentifierColorGenerator(randomGenerator);
  }

  /**
   * Creates the initial chart and establishes the root workspace leaf.
   *
   * @param settings the initial chart settings
   */
  void initialize(ChartWorkspaceSettings settings) {
    if (model.getRoot() != null) {
      throw new IllegalStateException("Workspace is already initialized");
    }
    Chart chart = chartFactory.create(Objects.requireNonNull(settings, "settings cannot be null"));
    configure(chart);
    model.setRoot(new ChartWorkspaceLeaf(chart));
    model.setActiveChart(chart);
    updateMultipleChartState();
  }

  /** Starts all current charts once and causes subsequently split charts to start immediately. */
  void start() {
    requireOpen();
    if (started) {
      return;
    }
    started = true;
    charts(model.getRoot()).forEach(this::startChart);
  }

  /**
   * Splits a workspace chart in the requested direction and activates the new chart.
   *
   * @param source the chart pane to split
   * @param direction the placement of the newly created chart
   */
  void split(Chart source, ChartSplitDirection direction) {
    requireOpen();
    Objects.requireNonNull(source, "source cannot be null");
    Objects.requireNonNull(direction, "direction cannot be null");
    if (!contains(model.getRoot(), source)) {
      return;
    }

    ChartWorkspaceSettings settings = new ChartWorkspaceSettings(
      source.getSymbol(),
      source.getInterval(),
      source.getChartType()
    );
    Chart created;
    try {
      created = chartFactory.create(settings);
    } catch (RuntimeException failure) {
      LOGGER.log(System.Logger.Level.ERROR, "Could not create a split chart", failure);
      return;
    }
    configure(created);

    ChartWorkspaceLeaf sourceLeaf = new ChartWorkspaceLeaf(source);
    ChartWorkspaceLeaf createdLeaf = new ChartWorkspaceLeaf(created);
    Orientation orientation = switch (direction) {
      case LEFT, RIGHT -> Orientation.HORIZONTAL;
      case TOP, BOTTOM -> Orientation.VERTICAL;
    };
    ChartWorkspaceItem first =
      direction == ChartSplitDirection.LEFT || direction == ChartSplitDirection.TOP ? createdLeaf : sourceLeaf;
    ChartWorkspaceItem second = first == createdLeaf ? sourceLeaf : createdLeaf;
    ChartWorkspaceSplit replacement = new ChartWorkspaceSplit(orientation, first, second);

    model.setRoot(replace(model.getRoot(), source, replacement));
    model.setActiveChart(created);
    updateMultipleChartState();
    if (started) {
      startChart(created);
    }
  }

  /**
   * Activates a chart when it belongs to the current workspace tree.
   *
   * @param chart the chart to activate
   */
  void activate(Chart chart) {
    requireOpen();
    Chart requested = Objects.requireNonNull(chart, "chart cannot be null");
    if (contains(model.getRoot(), requested)) {
      model.setActiveChart(requested);
    }
  }

  /**
   * Activates the nearest chart pane in a cardinal direction without wrapping.
   *
   * @param direction the requested navigation direction
   */
  void navigateActive(ChartWorkspaceNavigationDirection direction) {
    requireOpen();
    Objects.requireNonNull(direction, "direction cannot be null");

    Chart activeChart = requireActiveChart();
    List<ChartBounds> chartBounds = new ArrayList<>();
    collectChartBounds(model.getRoot(), 0.0, 0.0, 1.0, 1.0, chartBounds);
    ChartBounds activeBounds = chartBounds
      .stream()
      .filter(bounds -> bounds.chart() == activeChart)
      .findFirst()
      .orElseThrow(() -> new IllegalStateException("active chart must belong to the workspace"));

    chartBounds
      .stream()
      .filter(candidate -> candidate.chart() != activeChart)
      .filter(candidate -> isInDirection(activeBounds, candidate, direction))
      .min(
        Comparator.comparingDouble((ChartBounds candidate) -> primaryDistance(activeBounds, candidate, direction))
          .thenComparingDouble(candidate -> perpendicularDistance(activeBounds, candidate, direction))
          .thenComparingDouble(ChartBounds::minY)
          .thenComparingDouble(ChartBounds::minX)
      )
      .ifPresent(candidate -> model.setActiveChart(candidate.chart()));
  }

  /** Requests instrument search from the active chart. */
  void showActiveInstrumentSearch() {
    requireActiveChart().showInstrumentSearch();
  }

  /** Closes instrument search on the active chart. */
  void closeActiveInstrumentSearch() {
    requireActiveChart().closeInstrumentSearch();
  }

  /** Requests interval selection from the active chart. */
  void showActiveIntervalSelection() {
    requireActiveChart().showIntervalSelection();
  }

  /** Closes interval selection on the active chart. */
  void closeActiveIntervalSelection() {
    requireActiveChart().closeIntervalSelection();
  }

  /**
   * Updates the active chart's interval.
   *
   * @param interval the selected interval
   */
  void setActiveChartInterval(ChartInterval interval) {
    requireActiveChart().setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  /**
   * Selects an instrument on the active chart.
   *
   * @param symbol the selected instrument symbol
   */
  void setActiveChartInstrument(String symbol) {
    requireActiveChart().selectInstrument(Objects.requireNonNull(symbol, "symbol cannot be null"));
  }

  /**
   * Updates the active chart's rendering type.
   *
   * @param chartType the selected chart type
   */
  void setActiveChartType(ChartType chartType) {
    requireActiveChart().setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  /**
   * Splits the active chart in the requested direction.
   *
   * @param direction the placement of the newly created chart
   */
  void splitActive(ChartSplitDirection direction) {
    split(requireActiveChart(), direction);
  }

  /** Removes the active chart when the workspace contains another chart. */
  void removeActive() {
    remove(requireActiveChart());
  }

  /**
   * Removes and closes a chart, collapsing its parent split and selecting a sibling fallback.
   *
   * @param chart the chart to remove
   */
  void remove(Chart chart) {
    requireOpen();
    Objects.requireNonNull(chart, "chart cannot be null");
    if (count(model.getRoot()) <= 1) {
      return;
    }

    Removal removal = remove(model.getRoot(), chart);
    if (!removal.removed()) {
      return;
    }
    model.setRoot(removal.item());
    if (model.getActiveChart() == chart) {
      model.setActiveChart(Objects.requireNonNull(removal.fallback(), "removed chart must have a fallback"));
    }
    identifierColors.remove(chart);
    updateMultipleChartState();
    try {
      chart.close();
    } catch (RuntimeException failure) {
      LOGGER.log(System.Logger.Level.WARNING, "Could not completely close a removed chart", failure);
    }
  }

  /** Closes every workspace-owned chart once and aggregates close failures. */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    RuntimeException failure = null;
    for (Chart chart : charts(model.getRoot())) {
      try {
        chart.close();
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure = exception;
        } else {
          failure.addSuppressed(exception);
        }
      }
    }
    identifierColors.clear();
    if (failure != null) {
      throw failure;
    }
  }

  /**
   * Assigns stable workspace identifier presentation to a newly created chart.
   *
   * @param chart the chart to configure
   */
  private void configure(Chart chart) {
    Color identifierColor = identifierColorGenerator.next(identifierColors.values());
    identifierColors.put(chart, identifierColor);
    chart.setIdentifierColor(identifierColor);
    chart.setIdentifierVisible(true);
  }

  /** Updates the derived multiple-chart model state from the workspace tree. */
  private void updateMultipleChartState() {
    model.setMultipleCharts(count(model.getRoot()) > 1);
  }

  /**
   * Starts rendering and initial instrument loading for a chart.
   *
   * @param chart the chart to start
   */
  private void startChart(Chart chart) {
    chart.drawChart();
    chart.loadInitialInstrument();
  }

  /** Ensures workspace commands are not applied after closure. */
  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Workspace is closed");
    }
  }

  /**
   * Returns the active chart after validating workspace lifecycle and initialization.
   *
   * @return the active chart
   */
  private Chart requireActiveChart() {
    requireOpen();
    return Objects.requireNonNull(model.getActiveChart(), "workspace must have an active chart");
  }

  /**
   * Recursively replaces the leaf containing a chart while preserving untouched branches.
   *
   * @param item the current tree item
   * @param source the chart whose leaf should be replaced
   * @param replacement the replacement tree item
   * @return the original or rebuilt tree item
   */
  private static ChartWorkspaceItem replace(ChartWorkspaceItem item, Chart source, ChartWorkspaceItem replacement) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart() == source ? replacement : leaf;
    }
    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    ChartWorkspaceItem first = replace(split.first(), source, replacement);
    ChartWorkspaceItem second = replace(split.second(), source, replacement);
    return first == split.first() && second == split.second()
      ? split
      : new ChartWorkspaceSplit(split.orientation(), first, second, split.dividerPosition());
  }

  /**
   * Recursively removes a chart leaf and determines the chart promoted as fallback.
   *
   * @param item the current tree item
   * @param target the chart to remove
   * @return the removal result for the current subtree
   */
  private static Removal remove(ChartWorkspaceItem item, Chart target) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart() == target ? new Removal(null, true, null) : new Removal(leaf, false, null);
    }

    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    Removal first = remove(split.first(), target);
    if (first.removed()) {
      return new Removal(
        first.item() == null
          ? split.second()
          : new ChartWorkspaceSplit(split.orientation(), first.item(), split.second(), split.dividerPosition()),
        true,
        first.fallback() != null ? first.fallback() : firstChart(split.second())
      );
    }
    Removal second = remove(split.second(), target);
    if (second.removed()) {
      return new Removal(
        second.item() == null
          ? split.first()
          : new ChartWorkspaceSplit(split.orientation(), split.first(), second.item(), split.dividerPosition()),
        true,
        second.fallback() != null ? second.fallback() : firstChart(split.first())
      );
    }
    return new Removal(split, false, null);
  }

  /**
   * Returns the first chart contained in a subtree.
   *
   * @param item the subtree root
   * @return the first chart in tree order
   */
  private static Chart firstChart(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart();
    }
    return firstChart(((ChartWorkspaceSplit) item).first());
  }

  /**
   * Reports whether a subtree contains a chart by identity.
   *
   * @param item the subtree root
   * @param target the chart to locate
   * @return true when the chart belongs to the subtree
   */
  private static boolean contains(ChartWorkspaceItem item, Chart target) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart() == target;
    }
    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    return contains(split.first(), target) || contains(split.second(), target);
  }

  /**
   * Counts chart leaves in a subtree.
   *
   * @param item the subtree root, or null
   * @return the number of chart leaves
   */
  private static int count(ChartWorkspaceItem item) {
    if (item == null) {
      return 0;
    }
    if (item instanceof ChartWorkspaceLeaf) {
      return 1;
    }
    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    return count(split.first()) + count(split.second());
  }

  /**
   * Flattens a workspace tree into chart order.
   *
   * @param item the tree root
   * @return the charts in tree order
   */
  private static List<Chart> charts(ChartWorkspaceItem item) {
    List<Chart> charts = new ArrayList<>();
    collectCharts(item, charts);
    return charts;
  }

  /**
   * Recursively projects workspace items into normalized pane bounds.
   *
   * @param item the current tree item
   * @param minX the subtree's minimum normalized x-coordinate
   * @param minY the subtree's minimum normalized y-coordinate
   * @param width the subtree's normalized width
   * @param height the subtree's normalized height
   * @param bounds the destination pane bounds
   */
  private static void collectChartBounds(
    ChartWorkspaceItem item,
    double minX,
    double minY,
    double width,
    double height,
    List<ChartBounds> bounds
  ) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      bounds.add(new ChartBounds(leaf.chart(), minX, minY, minX + width, minY + height));
      return;
    }

    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    double dividerPosition = split.dividerPosition();
    if (split.orientation() == Orientation.HORIZONTAL) {
      double firstWidth = width * dividerPosition;
      collectChartBounds(split.first(), minX, minY, firstWidth, height, bounds);
      collectChartBounds(split.second(), minX + firstWidth, minY, width - firstWidth, height, bounds);
    } else {
      double firstHeight = height * dividerPosition;
      collectChartBounds(split.first(), minX, minY, width, firstHeight, bounds);
      collectChartBounds(split.second(), minX, minY + firstHeight, width, height - firstHeight, bounds);
    }
  }

  /**
   * Reports whether candidate bounds lie wholly in a direction from active bounds.
   *
   * @param active the active chart bounds
   * @param candidate the candidate chart bounds
   * @param direction the requested navigation direction
   * @return true when the candidate lies in the requested direction
   */
  private static boolean isInDirection(
    ChartBounds active,
    ChartBounds candidate,
    ChartWorkspaceNavigationDirection direction
  ) {
    return switch (direction) {
      case LEFT -> candidate.maxX() <= active.minX();
      case RIGHT -> candidate.minX() >= active.maxX();
      case ABOVE -> candidate.maxY() <= active.minY();
      case BELOW -> candidate.minY() >= active.maxY();
    };
  }

  /**
   * Computes separation along the requested navigation axis.
   *
   * @param active the active chart bounds
   * @param candidate the candidate chart bounds
   * @param direction the requested navigation direction
   * @return the primary-axis distance
   */
  private static double primaryDistance(
    ChartBounds active,
    ChartBounds candidate,
    ChartWorkspaceNavigationDirection direction
  ) {
    return switch (direction) {
      case LEFT -> active.minX() - candidate.maxX();
      case RIGHT -> candidate.minX() - active.maxX();
      case ABOVE -> active.minY() - candidate.maxY();
      case BELOW -> candidate.minY() - active.maxY();
    };
  }

  /**
   * Computes center separation perpendicular to the requested navigation axis.
   *
   * @param active the active chart bounds
   * @param candidate the candidate chart bounds
   * @param direction the requested navigation direction
   * @return the perpendicular center distance
   */
  private static double perpendicularDistance(
    ChartBounds active,
    ChartBounds candidate,
    ChartWorkspaceNavigationDirection direction
  ) {
    return switch (direction) {
      case LEFT, RIGHT -> Math.abs(active.centerY() - candidate.centerY());
      case ABOVE, BELOW -> Math.abs(active.centerX() - candidate.centerX());
    };
  }

  /**
   * Recursively collects charts from a workspace tree.
   *
   * @param item the current tree item, or null
   * @param charts the destination chart list
   */
  private static void collectCharts(ChartWorkspaceItem item, List<Chart> charts) {
    if (item == null) {
      return;
    }
    if (item instanceof ChartWorkspaceLeaf leaf) {
      charts.add(leaf.chart());
      return;
    }
    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    collectCharts(split.first(), charts);
    collectCharts(split.second(), charts);
  }

  /**
   * Describes a recursive chart-removal result.
   *
   * @param item the remaining subtree, or null when its leaf was removed
   * @param removed true when the target was found
   * @param fallback the chart promoted after removal, or null
   */
  private record Removal(ChartWorkspaceItem item, boolean removed, Chart fallback) {}

  /**
   * Normalized pane bounds used for directional navigation.
   *
   * @param chart the represented chart
   * @param minX the minimum x-coordinate
   * @param minY the minimum y-coordinate
   * @param maxX the maximum x-coordinate
   * @param maxY the maximum y-coordinate
   */
  private record ChartBounds(Chart chart, double minX, double minY, double maxX, double maxY) {
    /**
     * Returns the horizontal center coordinate.
     *
     * @return the horizontal center
     */
    private double centerX() {
      return (minX + maxX) / 2.0;
    }

    /**
     * Returns the vertical center coordinate.
     *
     * @return the vertical center
     */
    private double centerY() {
      return (minY + maxY) / 2.0;
    }
  }
}
