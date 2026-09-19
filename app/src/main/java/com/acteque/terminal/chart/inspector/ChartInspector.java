package com.acteque.terminal.chart.inspector;

import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.ui.drawer.Drawer;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the chart inspector MVCI feature. */
public final class ChartInspector {

  private final ChartInspectorModel model;
  private final ChartInspectorInteractor interactor;
  private final ChartInspectorViewBuilder viewBuilder;

  public ChartInspector() {
    model = new ChartInspectorModel();
    interactor = new ChartInspectorInteractor(model);
    interactor.initialize(ChartType.LINE);
    viewBuilder = new ChartInspectorViewBuilder(model, interactor::selectChartType, interactor::setOpen);
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
