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

  public ChartWorkspaceMenu(Chart activeChart) {
    ChartWorkspaceMenuModel model = new ChartWorkspaceMenuModel();
    interactor = new ChartWorkspaceMenuInteractor(model);
    interactor.onActionRequested(this::requestAction);
    interactor.initialize(activeChart);
    viewBuilder = new ChartWorkspaceMenuViewBuilder(model, interactor::request, this::requestSplit);
  }

  public Region getView() {
    return viewBuilder.build();
  }

  public void setActiveChart(Chart chart) {
    viewBuilder.closeTransientUi();
    interactor.setActiveChart(chart);
  }

  public void setMultipleCharts(boolean value) {
    interactor.setMultipleCharts(value);
  }

  public void setChartTypeSelectionOpen(boolean value) {
    viewBuilder.setChartTypeSelectionOpen(value);
  }

  public void onInstrumentSelectionRequested(Runnable callback) {
    instrumentSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onIntervalSelectionRequested(Runnable callback) {
    intervalSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onChartTypeSelectionRequested(Runnable callback) {
    chartTypeSelectionAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onSplitRequested(Consumer<ChartSplitDirection> callback) {
    splitAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onCloseRequested(Runnable callback) {
    closeAction = Objects.requireNonNull(callback, "callback cannot be null");
  }

  @Override
  public void close() {
    viewBuilder.closeTransientUi();
    interactor.close();
  }

  private void requestAction(Item item) {
    switch (item) {
      case INSTRUMENT -> instrumentSelectionAction.run();
      case INTERVAL -> intervalSelectionAction.run();
      case CHART_TYPE -> chartTypeSelectionAction.run();
      case SPLIT -> throw new IllegalStateException("Split directions are requested directly");
      case CLOSE -> closeAction.run();
    }
  }

  private void requestSplit(ChartSplitDirection direction) {
    splitAction.accept(direction);
  }
}
