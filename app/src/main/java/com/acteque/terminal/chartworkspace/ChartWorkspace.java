package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.AppService;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.inspector.ChartInspector;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenu;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.LogoSession;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.StackPane;

/** Composes and exposes the recursively split chart workspace. */
public final class ChartWorkspace implements AutoCloseable {

  private final ChartWorkspaceInteractor interactor;
  private final ChartWorkspaceViewBuilder viewBuilder;
  private final ChartWorkspaceModel model;
  private final ChartWorkspaceMenu menu;
  private final ChartInspector inspector;
  private final ChangeListener<Boolean> modalOpenListener;
  private final ChangeListener<ChartType> chartTypeListener;
  private Chart observedChart;

  public ChartWorkspace(
    AppService services,
    String symbol,
    ChartInterval interval,
    ChartType chartType,
    Executor uiExecutor
  ) {
    this(new ChartWorkspaceSettings(symbol, interval, chartType), settings ->
      createChart(services, settings, uiExecutor)
    );
  }

  ChartWorkspace(ChartWorkspaceSettings settings, ChartWorkspaceChartFactory chartFactory) {
    model = new ChartWorkspaceModel();
    interactor = new ChartWorkspaceInteractor(model, chartFactory);
    interactor.initialize(settings);

    inspector = new ChartInspector();
    modalOpenListener = (ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        inspector.close();
      }
    };
    chartTypeListener = (ignored, previous, current) -> inspector.setChartType(current);
    inspector.onChartTypeSelected(interactor::setActiveChartType);
    menu = new ChartWorkspaceMenu(model.getActiveChart());
    menu.onInstrumentSelectionRequested(() -> {
      inspector.close();
      interactor.showActiveInstrumentSearch();
    });
    menu.onIntervalSelectionRequested(() -> {
      inspector.close();
      interactor.showActiveIntervalSelection();
    });
    menu.onChartTypeSelectionRequested(inspector::showChartTypes);
    menu.onSplitRequested(interactor::splitActive);
    menu.onCloseRequested(interactor::removeActive);
    menu.setMultipleCharts(model.hasMultipleCharts());
    inspector.openProperty().addListener((ignored, wasOpen, isOpen) -> menu.setChartTypeSelectionOpen(isOpen));
    model.activeChartProperty().addListener((ignored, previous, current) -> observeActiveChart(current));
    model.multipleChartsProperty().addListener((ignored, previous, current) -> menu.setMultipleCharts(current));
    observeActiveChart(model.getActiveChart());

    viewBuilder = new ChartWorkspaceViewBuilder(model, interactor::activate, menu.getView(), inspector.getView());
  }

  public StackPane getView() {
    return viewBuilder.build();
  }

  public void start() {
    interactor.start();
  }

  @Override
  public void close() {
    stopObservingActiveChart();
    inspector.close();
    menu.close();
    interactor.close();
  }

  private void observeActiveChart(Chart chart) {
    stopObservingActiveChart();
    observedChart = Objects.requireNonNull(chart, "active chart cannot be null");
    menu.setActiveChart(observedChart);
    inspector.close();
    inspector.setChartType(observedChart.getChartType());
    observedChart.modalOpenProperty().addListener(modalOpenListener);
    observedChart.chartTypeProperty().addListener(chartTypeListener);
  }

  private void stopObservingActiveChart() {
    if (observedChart == null) {
      return;
    }
    ObservableBooleanValue modalOpen = observedChart.modalOpenProperty();
    modalOpen.removeListener(modalOpenListener);
    observedChart.chartTypeProperty().removeListener(chartTypeListener);
    observedChart = null;
  }

  private static Chart createChart(AppService services, ChartWorkspaceSettings settings, Executor uiExecutor) {
    Objects.requireNonNull(services, "services cannot be null");
    Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
    MarketDataSession marketData = services.createMarketDataSession(settings.symbol());
    LogoSession logos = null;
    try {
      logos = services.createLogoSession();
      Chart chart = new Chart(
        List.of(),
        settings.symbol(),
        settings.interval(),
        services.catalog(),
        marketData,
        logos,
        uiExecutor
      );
      chart.setChartType(settings.chartType());
      return chart;
    } catch (RuntimeException | Error failure) {
      if (logos != null) {
        try {
          logos.close();
        } catch (RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      try {
        marketData.close();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }
}
