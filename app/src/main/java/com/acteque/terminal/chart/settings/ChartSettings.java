package com.acteque.terminal.chart.settings;

import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.ui.drawer.Drawer;
import java.util.Objects;
import java.util.function.Consumer;

/** Owns the chart's settings drawer and routes changes back to the chart. */
public final class ChartSettings {

  private final ChartSettingsViewBuilder viewBuilder = new ChartSettingsViewBuilder(this::selectChartType);
  private Consumer<ChartType> chartTypeSelectedHandler = ignored -> {};

  public Drawer getView() {
    return viewBuilder.build();
  }

  public void showChartTypes() {
    viewBuilder.showChartTypes();
  }

  public void close() {
    viewBuilder.close();
  }

  public void setChartType(ChartType chartType) {
    viewBuilder.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
  }

  public void onChartTypeSelected(Consumer<ChartType> callback) {
    chartTypeSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  private void selectChartType(ChartType chartType) {
    setChartType(chartType);
    close();
    chartTypeSelectedHandler.accept(chartType);
  }
}
