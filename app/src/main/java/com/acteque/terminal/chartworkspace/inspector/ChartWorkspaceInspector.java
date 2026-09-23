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

  /** Creates and connects the chart workspace inspector's MVCI components. */
  public ChartWorkspaceInspector() {
    model = new ChartWorkspaceInspectorModel();
    interactor = new ChartWorkspaceInspectorInteractor(model);
    interactor.initialize(ChartType.LINE);

    viewBuilder = new ChartWorkspaceInspectorViewBuilder(model, interactor::selectChartType, interactor::setOpen);
  }

  /**
   * Returns the composed inspector view.
   *
   * @return the chart-type drawer
   */
  public Drawer getView() {
    return viewBuilder.build();
  }

  /**
   * Returns the observable drawer state.
   *
   * @return the read-only open state
   */
  public ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  /** Opens the inspector to display the available chart types. */
  public void showChartTypes() {
    interactor.showChartTypes();
  }

  /** Closes the inspector. */
  public void close() {
    interactor.close();
  }

  /**
   * Updates the chart type displayed as selected without notifying the selection callback.
   *
   * @param chartType the chart type to display as selected
   */
  public void setChartType(ChartType chartType) {
    interactor.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  /**
   * Registers the action invoked when the user selects a chart type.
   *
   * @param callback the selected-chart-type callback
   */
  public void onChartTypeSelected(Consumer<ChartType> callback) {
    interactor.onChartTypeSelected(callback);
  }
}
