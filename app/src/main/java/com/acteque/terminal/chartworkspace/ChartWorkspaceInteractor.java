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

final class ChartWorkspaceInteractor implements AutoCloseable {

  private static final System.Logger LOGGER = System.getLogger(ChartWorkspaceInteractor.class.getName());

  private final ChartWorkspaceModel model;
  private final ChartWorkspaceChartFactory chartFactory;
  private final ChartIdentifierColorGenerator identifierColorGenerator;
  private final Map<Chart, Color> identifierColors = new IdentityHashMap<>();
  private boolean started;
  private boolean closed;

  ChartWorkspaceInteractor(ChartWorkspaceModel model, ChartWorkspaceChartFactory chartFactory) {
    this(model, chartFactory, RandomGenerator.getDefault());
  }

  ChartWorkspaceInteractor(
    ChartWorkspaceModel model,
    ChartWorkspaceChartFactory chartFactory,
    RandomGenerator randomGenerator
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.chartFactory = Objects.requireNonNull(chartFactory, "chartFactory cannot be null");
    identifierColorGenerator = new ChartIdentifierColorGenerator(randomGenerator);
  }

  void initialize(ChartWorkspaceSettings settings) {
    if (model.getRoot() != null) {
      throw new IllegalStateException("Workspace is already initialized");
    }
    Chart chart = chartFactory.create(Objects.requireNonNull(settings, "settings cannot be null"));
    configure(chart);
    model.setRoot(new ChartWorkspaceLeaf(chart));
    model.setActiveChart(chart);
    updateMultiChartAvailability();
  }

  void start() {
    requireOpen();
    if (started) {
      return;
    }
    started = true;
    charts(model.getRoot()).forEach(this::startChart);
  }

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
    updateMultiChartAvailability();
    if (started) {
      startChart(created);
    }
  }

  void activate(Chart chart) {
    requireOpen();
    Chart requested = Objects.requireNonNull(chart, "chart cannot be null");
    if (contains(model.getRoot(), requested)) {
      model.setActiveChart(requested);
    }
  }

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

  void showActiveInstrumentSearch() {
    requireActiveChart().showInstrumentSearch();
  }

  void closeActiveInstrumentSearch() {
    requireActiveChart().closeInstrumentSearch();
  }

  void showActiveIntervalSelection() {
    requireActiveChart().showIntervalSelection();
  }

  void closeActiveIntervalSelection() {
    requireActiveChart().closeIntervalSelection();
  }

  void setActiveChartInterval(ChartInterval interval) {
    requireActiveChart().setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  void setActiveChartInstrument(String symbol) {
    requireActiveChart().selectInstrument(Objects.requireNonNull(symbol, "symbol cannot be null"));
  }

  void setActiveChartType(ChartType chartType) {
    requireActiveChart().setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  void splitActive(ChartSplitDirection direction) {
    split(requireActiveChart(), direction);
  }

  void removeActive() {
    remove(requireActiveChart());
  }

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
    updateMultiChartAvailability();
    try {
      chart.close();
    } catch (RuntimeException failure) {
      LOGGER.log(System.Logger.Level.WARNING, "Could not completely close a removed chart", failure);
    }
  }

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

  private void configure(Chart chart) {
    Color identifierColor = identifierColorGenerator.next(identifierColors.values());
    identifierColors.put(chart, identifierColor);
    chart.setIdentifierColor(identifierColor);
  }

  private void updateMultiChartAvailability() {
    boolean available = count(model.getRoot()) > 1;
    model.setMultipleCharts(available);
    charts(model.getRoot()).forEach(chart -> chart.setIdentifierVisible(available));
  }

  private void startChart(Chart chart) {
    chart.drawChart();
    chart.loadInitialInstrument();
  }

  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Workspace is closed");
    }
  }

  private Chart requireActiveChart() {
    requireOpen();
    return Objects.requireNonNull(model.getActiveChart(), "workspace must have an active chart");
  }

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

  private static Chart firstChart(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart();
    }
    return firstChart(((ChartWorkspaceSplit) item).first());
  }

  private static boolean contains(ChartWorkspaceItem item, Chart target) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      return leaf.chart() == target;
    }
    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    return contains(split.first(), target) || contains(split.second(), target);
  }

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

  private static List<Chart> charts(ChartWorkspaceItem item) {
    List<Chart> charts = new ArrayList<>();
    collectCharts(item, charts);
    return charts;
  }

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

  private record Removal(ChartWorkspaceItem item, boolean removed, Chart fallback) {}

  private record ChartBounds(Chart chart, double minX, double minY, double maxX, double maxY) {
    private double centerX() {
      return (minX + maxX) / 2.0;
    }

    private double centerY() {
      return (minY + maxY) / 2.0;
    }
  }
}
