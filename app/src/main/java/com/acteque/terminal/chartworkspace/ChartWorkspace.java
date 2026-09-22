package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.AppService;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chartworkspace.inspector.ChartWorkspaceInspector;
import com.acteque.terminal.chartworkspace.instrumentsearch.InstrumentSearch;
import com.acteque.terminal.chartworkspace.intervalselection.ChartWorkspaceIntervalSelection;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenu;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.LogoSession;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.StackPane;

/** Composes and exposes the recursively split chart workspace. */
public final class ChartWorkspace implements AutoCloseable {

  private final ChartWorkspaceInteractor interactor;
  private final ChartWorkspaceViewBuilder viewBuilder;
  private final ChartWorkspaceModel model;
  private final ChartWorkspaceMenu menu;
  private final ChartWorkspaceInspector inspector;
  private final InstrumentSearch instrumentSearch;
  private final ChartWorkspaceIntervalSelection intervalSelection;
  private final ChangeListener<Boolean> modalOpenListener;
  private final ChangeListener<Boolean> instrumentSearchOpenListener;
  private final ChangeListener<Boolean> intervalSelectionOpenListener;
  private final ChangeListener<String> symbolListener;
  private final ChangeListener<ChartInterval> intervalListener;
  private final ChangeListener<ChartType> chartTypeListener;
  private Chart observedChart;

  public ChartWorkspace(
    AppService services,
    String symbol,
    ChartInterval interval,
    ChartType chartType,
    Executor uiExecutor
  ) {
    this(
      new ChartWorkspaceSettings(symbol, interval, chartType),
      settings -> createChart(services, settings, uiExecutor),
      Objects.requireNonNull(services, "services cannot be null").catalog(),
      ForkJoinPool.commonPool(),
      uiExecutor
    );
  }

  ChartWorkspace(
    ChartWorkspaceSettings settings,
    ChartWorkspaceChartFactory chartFactory,
    InstrumentCatalog instrumentCatalog,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    model = new ChartWorkspaceModel();
    interactor = new ChartWorkspaceInteractor(model, chartFactory);
    interactor.initialize(settings);

    inspector = new ChartWorkspaceInspector();
    instrumentSearch = new InstrumentSearch(
      model.getActiveChart().getSymbol(),
      instrumentCatalog,
      backgroundExecutor,
      uiExecutor
    );
    intervalSelection = new ChartWorkspaceIntervalSelection(model.getActiveChart().getInterval());
    modalOpenListener = (ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        inspector.close();
      }
    };
    instrumentSearchOpenListener = (ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        inspector.close();
        instrumentSearch.show();
      } else {
        instrumentSearch.close();
      }
    };
    intervalSelectionOpenListener = (ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        inspector.close();
        intervalSelection.show();
      } else {
        intervalSelection.close();
      }
    };
    symbolListener = (ignored, previous, current) -> instrumentSearch.setCurrentSymbol(current);
    intervalListener = (ignored, previous, current) -> intervalSelection.setCurrentInterval(current);
    chartTypeListener = (ignored, previous, current) -> inspector.setChartType(current);
    inspector.onChartTypeSelected(interactor::setActiveChartType);
    instrumentSearch.onInstrumentSelected(interactor::setActiveChartInstrument);
    instrumentSearch.onRequestClose(interactor::closeActiveInstrumentSearch);
    intervalSelection.onIntervalSelected(interactor::setActiveChartInterval);
    intervalSelection.onRequestClose(interactor::closeActiveIntervalSelection);
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

    viewBuilder = new ChartWorkspaceViewBuilder(
      model,
      interactor::activate,
      interactor::navigateActive,
      interactor::splitActive,
      interactor::removeActive,
      menu.getView(),
      inspector.getView(),
      intervalSelection.getView(),
      instrumentSearch.getView()
    );
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
    instrumentSearch.close();
    intervalSelection.close();
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
    instrumentSearch.close();
    instrumentSearch.setCurrentSymbol(observedChart.getSymbol());
    intervalSelection.close();
    intervalSelection.setCurrentInterval(observedChart.getInterval());
    intervalSelection.setAvailability(observedChart::supportsInterval);
    observedChart.modalOpenProperty().addListener(modalOpenListener);
    observedChart.instrumentSearchOpenProperty().addListener(instrumentSearchOpenListener);
    observedChart.intervalSelectionOpenProperty().addListener(intervalSelectionOpenListener);
    observedChart.symbolProperty().addListener(symbolListener);
    observedChart.intervalProperty().addListener(intervalListener);
    observedChart.chartTypeProperty().addListener(chartTypeListener);
    if (observedChart.intervalSelectionOpenProperty().get()) {
      intervalSelection.show();
    }
    if (observedChart.instrumentSearchOpenProperty().get()) {
      instrumentSearch.show();
    }
  }

  private void stopObservingActiveChart() {
    if (observedChart == null) {
      return;
    }
    ObservableBooleanValue modalOpen = observedChart.modalOpenProperty();
    modalOpen.removeListener(modalOpenListener);
    observedChart.instrumentSearchOpenProperty().removeListener(instrumentSearchOpenListener);
    observedChart.intervalSelectionOpenProperty().removeListener(intervalSelectionOpenListener);
    observedChart.symbolProperty().removeListener(symbolListener);
    observedChart.intervalProperty().removeListener(intervalListener);
    observedChart.chartTypeProperty().removeListener(chartTypeListener);
    observedChart.closeInstrumentSearch();
    observedChart.closeIntervalSelection();
    observedChart = null;
  }

  private static Chart createChart(AppService services, ChartWorkspaceSettings settings, Executor uiExecutor) {
    Objects.requireNonNull(services, "services cannot be null");
    Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
    MarketDataSession marketData = services.createMarketDataSession(settings.symbol());
    LogoSession logos = null;
    try {
      logos = services.createLogoSession();
      Chart chart = new Chart(List.of(), settings.symbol(), settings.interval(), marketData, logos, uiExecutor);
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
