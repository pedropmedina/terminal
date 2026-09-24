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
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;

/** Composes and exposes the recursively split chart workspace. */
public final class ChartWorkspace implements AutoCloseable {

  private final ChartWorkspaceModel model;
  private final ChartWorkspaceInteractor interactor;
  private final ChartWorkspaceViewBuilder viewBuilder;
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

  /**
   * Creates a workspace backed by application-owned provider registries.
   *
   * @param services the application services used to create chart resources
   * @param symbol the initial chart symbol
   * @param interval the initial chart interval
   * @param chartType the initial chart type
   * @param uiExecutor the executor used to publish chart results to JavaFX state
   */
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

  /**
   * Creates a workspace from injectable chart and catalog services.
   *
   * @param settings the initial chart settings
   * @param chartFactory the factory for independently owned charts
   * @param instrumentCatalog the source used by instrument search
   * @param backgroundExecutor the executor used to load the instrument catalog
   * @param uiExecutor the executor used to publish catalog results to JavaFX state
   */
  ChartWorkspace(
    ChartWorkspaceSettings settings,
    ChartWorkspaceChartFactory chartFactory,
    InstrumentCatalog instrumentCatalog,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    ChartWorkspaceSettings initialSettings = Objects.requireNonNull(settings, "settings cannot be null");
    ChartWorkspaceChartFactory validatedChartFactory = Objects.requireNonNull(
      chartFactory,
      "chartFactory cannot be null"
    );
    InstrumentCatalog validatedInstrumentCatalog = Objects.requireNonNull(
      instrumentCatalog,
      "instrumentCatalog cannot be null"
    );
    Executor validatedBackgroundExecutor = Objects.requireNonNull(
      backgroundExecutor,
      "backgroundExecutor cannot be null"
    );
    Executor validatedUiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");

    model = new ChartWorkspaceModel();
    interactor = new ChartWorkspaceInteractor(model, validatedChartFactory);
    interactor.initialize(initialSettings);

    inspector = new ChartWorkspaceInspector();
    instrumentSearch = new InstrumentSearch(
      model.getActiveChart().getSymbol(),
      validatedInstrumentCatalog,
      validatedBackgroundExecutor,
      validatedUiExecutor
    );
    intervalSelection = new ChartWorkspaceIntervalSelection(model.getActiveChart().getInterval());
    menu = new ChartWorkspaceMenu(model.getActiveChart());

    modalOpenListener = this::handleModalOpenChanged;
    instrumentSearchOpenListener = this::handleInstrumentSearchOpenChanged;
    intervalSelectionOpenListener = this::handleIntervalSelectionOpenChanged;
    symbolListener = this::handleSymbolChanged;
    intervalListener = this::handleIntervalChanged;
    chartTypeListener = this::handleChartTypeChanged;

    viewBuilder = new ChartWorkspaceViewBuilder(
      model,
      menu.getView(),
      inspector.getView(),
      intervalSelection.getView(),
      instrumentSearch.getView(),
      interactor::activate,
      interactor::navigateActive,
      interactor::splitActive,
      interactor::removeActive
    );

    connectComponents();
    observeActiveChart(model.getActiveChart());
  }

  /**
   * Returns the composed chart workspace.
   *
   * @return the workspace root
   */
  public StackPane getView() {
    return viewBuilder.build();
  }

  /** Starts every chart currently owned by the workspace. */
  public void start() {
    interactor.start();
  }

  /** Stops observation and closes every workspace-owned feature and chart. */
  @Override
  public void close() {
    stopObservingActiveChart();
    instrumentSearch.close();
    intervalSelection.close();
    inspector.close();
    menu.close();
    interactor.close();
  }

  /** Connects child-feature requests and observable workspace state. */
  private void connectComponents() {
    inspector.onChartTypeSelected(interactor::setActiveChartType);
    instrumentSearch.onInstrumentSelected(interactor::setActiveChartInstrument);
    instrumentSearch.onRequestClose(interactor::closeActiveInstrumentSearch);
    intervalSelection.onIntervalSelected(interactor::setActiveChartInterval);
    intervalSelection.onRequestClose(interactor::closeActiveIntervalSelection);

    menu.onInstrumentSelectionRequested(this::requestInstrumentSelection);
    menu.onIntervalSelectionRequested(this::requestIntervalSelection);
    menu.onChartTypeSelectionRequested(inspector::showChartTypes);
    menu.onSplitRequested(interactor::splitActive);
    menu.onCloseRequested(interactor::removeActive);
    menu.setMultipleCharts(model.hasMultipleCharts());

    inspector.openProperty().addListener((ignored, wasOpen, isOpen) -> menu.setChartTypeSelectionOpen(isOpen));
    model.activeChartProperty().addListener((ignored, previous, current) -> observeActiveChart(current));
    model.multipleChartsProperty().addListener((ignored, previous, current) -> menu.setMultipleCharts(current));
  }

  /** Closes the inspector before requesting instrument selection for the active chart. */
  private void requestInstrumentSelection() {
    inspector.close();
    interactor.showActiveInstrumentSearch();
  }

  /** Closes the inspector before requesting interval selection for the active chart. */
  private void requestIntervalSelection() {
    inspector.close();
    interactor.showActiveIntervalSelection();
  }

  /**
   * Closes the non-modal inspector when any chart modal opens.
   *
   * @param observable the observed modal state
   * @param wasOpen the previous modal state
   * @param isOpen the current modal state
   */
  private void handleModalOpenChanged(ObservableValue<? extends Boolean> observable, Boolean wasOpen, Boolean isOpen) {
    if (isOpen) {
      inspector.close();
    }
  }

  /**
   * Mirrors the active chart's instrument-search state into the shared dialog.
   *
   * @param observable the observed chart state
   * @param wasOpen the previous search state
   * @param isOpen the current search state
   */
  private void handleInstrumentSearchOpenChanged(
    ObservableValue<? extends Boolean> observable,
    Boolean wasOpen,
    Boolean isOpen
  ) {
    if (isOpen) {
      inspector.close();
      instrumentSearch.show();
    } else {
      instrumentSearch.close();
    }
  }

  /**
   * Mirrors the active chart's interval-selection state into the shared dialog.
   *
   * @param observable the observed chart state
   * @param wasOpen the previous selection state
   * @param isOpen the current selection state
   */
  private void handleIntervalSelectionOpenChanged(
    ObservableValue<? extends Boolean> observable,
    Boolean wasOpen,
    Boolean isOpen
  ) {
    if (isOpen) {
      inspector.close();
      intervalSelection.show();
    } else {
      intervalSelection.close();
    }
  }

  /**
   * Mirrors the active chart symbol into instrument search.
   *
   * @param observable the observed symbol
   * @param previous the previous symbol
   * @param current the current symbol
   */
  private void handleSymbolChanged(ObservableValue<? extends String> observable, String previous, String current) {
    instrumentSearch.setCurrentSymbol(current);
  }

  /**
   * Mirrors the active chart interval into interval selection.
   *
   * @param observable the observed interval
   * @param previous the previous interval
   * @param current the current interval
   */
  private void handleIntervalChanged(
    ObservableValue<? extends ChartInterval> observable,
    ChartInterval previous,
    ChartInterval current
  ) {
    intervalSelection.setCurrentInterval(current);
  }

  /**
   * Mirrors the active chart type into the inspector.
   *
   * @param observable the observed chart type
   * @param previous the previous chart type
   * @param current the current chart type
   */
  private void handleChartTypeChanged(
    ObservableValue<? extends ChartType> observable,
    ChartType previous,
    ChartType current
  ) {
    inspector.setChartType(current);
  }

  /**
   * Moves shared feature observation and presentation to a newly active chart.
   *
   * @param chart the newly active chart
   */
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

  /** Stops observing the previous active chart and closes its modal requests. */
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

  /**
   * Creates a chart and releases partially acquired resources if composition fails.
   *
   * @param services the application services that own provider registries
   * @param settings the chart settings
   * @param uiExecutor the executor used to publish chart results to JavaFX state
   * @return the composed chart
   */
  private static Chart createChart(AppService services, ChartWorkspaceSettings settings, Executor uiExecutor) {
    Objects.requireNonNull(services, "services cannot be null");
    Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
    MarketDataSession marketData = services.createMarketDataSession(settings.symbol());
    LogoSession logos = null;
    try {
      logos = services.createLogoSession();
      Chart chart = new Chart(settings.symbol(), settings.interval(), List.of(), logos, marketData, uiExecutor);
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
