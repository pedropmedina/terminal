package com.acteque.terminal.chartworkspace.inspector;

import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.ui.drawer.Drawer;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the chart inspector MVCI feature. */
public final class ChartWorkspaceInspector {

  private final ChartWorkspaceInspectorModel model;
  private final ChartWorkspaceInspectorInteractor interactor;
  private final ChartWorkspaceInspectorViewBuilder viewBuilder;

  public ChartWorkspaceInspector() {
    model = new ChartWorkspaceInspectorModel();
    interactor = new ChartWorkspaceInspectorInteractor(model);
    interactor.initialize(ChartType.LINE);
    viewBuilder = new ChartWorkspaceInspectorViewBuilder(model, interactor::selectChartType, interactor::setOpen);
  }

  public Drawer getView() {
    return viewBuilder.build();
  }

  public ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  public void showChartTypes() {
    interactor.showChartTypes();
  }

  public void close() {
    interactor.close();
  }

  public void setChartType(ChartType chartType) {
    interactor.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  public void onChartTypeSelected(Consumer<ChartType> callback) {
    interactor.onChartTypeSelected(callback);
  }
}
