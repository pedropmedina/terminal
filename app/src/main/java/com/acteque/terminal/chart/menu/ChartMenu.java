package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import java.util.Objects;
import javafx.scene.layout.Region;

/** Composes and exposes the chart menu's MVCI feature. */
public final class ChartMenu {

  private final ChartMenuInteractor interactor;
  private final ChartMenuViewBuilder viewBuilder;
  private Runnable instrumentSelectionAction = () -> {};
  private Runnable intervalSelectionAction = () -> {};
  private Runnable chartTypeSelectionAction = () -> {};

  public ChartMenu() {
    ChartMenuModel model = new ChartMenuModel();
    interactor = new ChartMenuInteractor(model);
    interactor.onActionRequested(this::requestAction);
    interactor.initialize();
    viewBuilder = new ChartMenuViewBuilder(model, interactor::request);
  }

  public Region getView() {
    return viewBuilder.build();
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

  private void requestAction(Item item) {
    switch (item) {
      case INSTRUMENT -> instrumentSelectionAction.run();
      case INTERVAL -> intervalSelectionAction.run();
      case CHART_TYPE -> chartTypeSelectionAction.run();
    }
  }
}
