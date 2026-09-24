package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenuModel.Item;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.scene.layout.Region;

/** Composes and exposes the workspace menu's MVCI feature. */
public final class ChartWorkspaceMenu implements AutoCloseable {

  private final ChartWorkspaceMenuInteractor interactor;
  private final ChartWorkspaceMenuViewBuilder viewBuilder;
  private Runnable instrumentSelectionAction = () -> {};
  private Runnable intervalSelectionAction = () -> {};
  private Runnable chartTypeSelectionAction = () -> {};
  private Consumer<ChartSplitDirection> splitAction = ignored -> {};
  private Runnable closeAction = () -> {};

  /**
   * Creates and connects the workspace-menu MVCI components.
   *
   * @param activeChart the chart initially represented by the menu
   */
  public ChartWorkspaceMenu(Chart activeChart) {
    ChartWorkspaceMenuModel model = new ChartWorkspaceMenuModel();

    interactor = new ChartWorkspaceMenuInteractor(model);
    interactor.onActionRequested(this::requestAction);
    interactor.initialize(activeChart);

    viewBuilder = new ChartWorkspaceMenuViewBuilder(model, interactor::request, this::requestSplit);
  }

  /**
   * Returns the composed workspace menu.
   *
   * @return the workspace-menu region
   */
  public Region getView() {
    return viewBuilder.build();
  }

  /**
   * Updates the chart represented by the menu after closing transient controls.
   *
   * @param chart the active workspace chart
   */
  public void setActiveChart(Chart chart) {
    viewBuilder.closeTransientUi();
    interactor.setActiveChart(chart);
  }

  /**
   * Updates whether actions that require multiple charts are available.
   *
   * @param value true when the workspace contains multiple charts
   */
  public void setMultipleCharts(boolean value) {
    interactor.setMultipleCharts(value);
  }

  /**
   * Updates the chart-type button's open-state presentation.
   *
   * @param value true when the chart-type selector is open
   */
  public void setChartTypeSelectionOpen(boolean value) {
    viewBuilder.setChartTypeSelectionOpen(value);
  }

  /**
   * Registers the action invoked when instrument selection is requested.
   *
   * @param callback the instrument-selection request callback
   */
  public void onInstrumentSelectionRequested(Runnable callback) {
    instrumentSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when interval selection is requested.
   *
   * @param callback the interval-selection request callback
   */
  public void onIntervalSelectionRequested(Runnable callback) {
    intervalSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when chart-type selection is requested.
   *
   * @param callback the chart-type selection request callback
   */
  public void onChartTypeSelectionRequested(Runnable callback) {
    chartTypeSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when a directional chart split is requested.
   *
   * @param callback the split request callback
   */
  public void onSplitRequested(Consumer<ChartSplitDirection> callback) {
    splitAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when closing the active chart is requested.
   *
   * @param callback the close request callback
   */
  public void onCloseRequested(Runnable callback) {
    closeAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** Closes transient controls and releases active-chart bindings. */
  @Override
  public void close() {
    viewBuilder.closeTransientUi();
    interactor.close();
  }

  /**
   * Routes a validated menu-item request to its external feature callback.
   *
   * @param item the requested menu item
   */
  private void requestAction(Item item) {
    switch (item) {
      case INSTRUMENT -> instrumentSelectionAction.run();
      case INTERVAL -> intervalSelectionAction.run();
      case CHART_TYPE -> chartTypeSelectionAction.run();
      case SPLIT -> throw new IllegalStateException("Split directions are requested directly");
      case CLOSE -> closeAction.run();
    }
  }

  /**
   * Routes a directional split request to the workspace callback.
   *
   * @param direction the requested split direction
   */
  private void requestSplit(ChartSplitDirection direction) {
    splitAction.accept(direction);
  }
}
