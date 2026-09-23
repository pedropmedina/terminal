package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvas;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.HistoricalInterval;
import com.acteque.terminal.marketdata.HistoricalPage;
import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentHistoryLoadResult;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoRequest;
import com.acteque.terminal.marketlogos.LogoSession;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/** Composes and exposes the chart's MVCI feature. */
public final class Chart implements AutoCloseable {

  private static final System.Logger LOGGER = System.getLogger(Chart.class.getName());

  private final LogoSession logoSession;
  private final String initialSymbol;
  private final boolean hasMarketDataSession;
  private final ChartModel model;
  private final ChartInteractor interactor;
  private final ChartCanvas canvas;
  private final ChartViewBuilder viewBuilder;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  /**
   * Creates a chart whose data is supplied directly by its caller.
   *
   * @param symbol the initial instrument symbol
   * @param interval the initial chart interval
   * @param pricePoints the initial ordered price points
   */
  public Chart(String symbol, ChartInterval interval, List<PricePoint> pricePoints) {
    this(symbol, interval, pricePoints, LogoSession.NONE, Optional.empty(), Runnable::run);
  }

  /**
   * Creates a caller-fed chart with an owned logo session.
   *
   * @param symbol the initial instrument symbol
   * @param interval the initial chart interval
   * @param pricePoints the initial ordered price points
   * @param logoSession the logo session owned by this chart
   * @param uiExecutor the executor used to publish asynchronous UI state
   */
  public Chart(
    String symbol,
    ChartInterval interval,
    List<PricePoint> pricePoints,
    LogoSession logoSession,
    Executor uiExecutor
  ) {
    this(symbol, interval, pricePoints, logoSession, Optional.empty(), uiExecutor);
  }

  /**
   * Creates a chart with an owned market-data session and no external logo provider.
   *
   * @param symbol the initial instrument symbol
   * @param interval the initial chart interval
   * @param pricePoints the initial ordered price points
   * @param marketDataSession the market-data session owned by this chart
   * @param uiExecutor the executor used to publish asynchronous UI state
   */
  public Chart(
    String symbol,
    ChartInterval interval,
    List<PricePoint> pricePoints,
    MarketDataSession marketDataSession,
    Executor uiExecutor
  ) {
    this(
      symbol,
      interval,
      pricePoints,
      LogoSession.NONE,
      Optional.of(Objects.requireNonNull(marketDataSession, "marketDataSession cannot be null")),
      uiExecutor
    );
  }

  /**
   * Creates a chart that owns both supplied sessions; provider instances may be shared through separate sessions.
   *
   * @param symbol the initial instrument symbol
   * @param interval the initial chart interval
   * @param pricePoints the initial ordered price points
   * @param logoSession the logo session owned by this chart
   * @param marketDataSession the market-data session owned by this chart
   * @param uiExecutor the executor used to publish asynchronous UI state
   */
  public Chart(
    String symbol,
    ChartInterval interval,
    List<PricePoint> pricePoints,
    LogoSession logoSession,
    MarketDataSession marketDataSession,
    Executor uiExecutor
  ) {
    this(
      symbol,
      interval,
      pricePoints,
      logoSession,
      Optional.of(Objects.requireNonNull(marketDataSession, "marketDataSession cannot be null")),
      uiExecutor
    );
  }

  /**
   * Creates and connects the chart's MVCI components.
   *
   * @param symbol the initial instrument symbol
   * @param interval the initial chart interval
   * @param pricePoints the initial ordered price points
   * @param logoSession the logo session owned by this chart
   * @param marketDataSession the optional market-data session owned by this chart
   * @param uiExecutor the executor used to publish asynchronous UI state
   */
  private Chart(
    String symbol,
    ChartInterval interval,
    List<PricePoint> pricePoints,
    LogoSession logoSession,
    Optional<MarketDataSession> marketDataSession,
    Executor uiExecutor
  ) {
    initialSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    ChartInterval initialInterval = Objects.requireNonNull(interval, "interval cannot be null");
    List<PricePoint> initialPricePoints = Objects.requireNonNull(pricePoints, "pricePoints cannot be null");

    this.logoSession = Objects.requireNonNull(logoSession, "logoSession cannot be null");
    Optional<MarketDataSession> availableMarketDataSession = Objects.requireNonNull(
      marketDataSession,
      "marketDataSession cannot be null"
    );
    Executor validatedUiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
    hasMarketDataSession = availableMarketDataSession.isPresent();

    model = createModel(initialSymbol);
    interactor = createInteractor(availableMarketDataSession, validatedUiExecutor);
    interactor.initialize(initialInterval);

    ChartStatusLine statusLine = new ChartStatusLine();
    canvas = new ChartCanvas(initialPricePoints, initialInterval, statusLine);
    viewBuilder = createViewBuilder(statusLine);

    connectComponents();
  }

  /**
   * Creates the chart model with its initial instrument and rendering type.
   *
   * @param symbol the initial instrument symbol
   * @return the initialized chart model
   */
  private static ChartModel createModel(String symbol) {
    ChartModel model = new ChartModel();
    model.setSymbol(symbol);
    model.setChartType(ChartType.LINE);
    return model;
  }

  /**
   * Creates the interactor for either the market-data-backed or caller-fed chart mode.
   *
   * @param marketDataSession the optional owned market-data session
   * @param uiExecutor the executor used to publish asynchronous UI state
   * @return the chart interactor
   */
  private ChartInteractor createInteractor(Optional<MarketDataSession> marketDataSession, Executor uiExecutor) {
    return marketDataSession
      .map(session -> new ChartInteractor(model, session, logoSession, uiExecutor))
      .orElseGet(() -> new ChartInteractor(model, logoSession, uiExecutor));
  }

  /**
   * Creates the chart view builder from the composed model, canvas, status line, and modal actions.
   *
   * @param statusLine the passive status-line component rendered above the canvas
   * @return the configured chart view builder
   */
  private ChartViewBuilder createViewBuilder(ChartStatusLine statusLine) {
    return new ChartViewBuilder(
      model,
      canvas.getView(),
      statusLine.getView(),
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection
    );
  }

  /** Connects callbacks shared by both chart data modes and any market-data-specific callbacks. */
  private void connectComponents() {
    interactor.onIntervalSelected(this::applySelectedInterval);
    if (hasMarketDataSession) {
      connectMarketDataComponents();
    }
  }

  /** Connects asynchronous market-data results and requests to their chart presentation handlers. */
  private void connectMarketDataComponents() {
    interactor.onInstrumentLoadStarted(interactor::cancelInstrumentLogoLoad);
    interactor.onInstrumentLoaded(this::applyLoadedInstrument);
    interactor.onEarlierHistoryLoaded(this::applyEarlierCalendarHistory);
    interactor.onInstrumentLoadFailed(Chart::reportInstrumentLoadFailure);
    interactor.onEarlierHistoryLoadFailed(Chart::reportEarlierHistoryLoadFailure);
    interactor.onHistoryLoaded(this::applyLoadedHistory);
    interactor.onEarlierSelectedHistoryLoaded(this::applyEarlierSelectedHistory);
    canvas.setOnEarlierHistoryRequested(interactor::loadEarlierSelectedHistory);
  }

  /**
   * Returns the composed chart view.
   *
   * @return the chart's root pane
   */
  public StackPane getView() {
    return viewBuilder.build();
  }

  /**
   * Returns the currently displayed instrument symbol.
   *
   * @return the provider-normalized instrument symbol
   */
  public String getSymbol() {
    return model.getSymbol();
  }

  /**
   * Returns the observable instrument symbol.
   *
   * @return the read-only symbol property
   */
  public ReadOnlyObjectProperty<String> symbolProperty() {
    return model.symbolProperty();
  }

  /**
   * Returns the currently selected chart interval.
   *
   * @return the selected interval
   */
  public ChartInterval getInterval() {
    return model.getInterval();
  }

  /**
   * Returns the observable chart interval.
   *
   * @return the read-only interval property
   */
  public ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return model.intervalProperty();
  }

  /**
   * Returns the currently selected chart type.
   *
   * @return the selected chart type
   */
  public ChartType getChartType() {
    return model.getChartType();
  }

  /**
   * Returns the observable chart type.
   *
   * @return the read-only chart-type property
   */
  public ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return model.chartTypeProperty();
  }

  /**
   * Returns the observable workspace identifier color.
   *
   * @return the read-only identifier-color property
   */
  public ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return model.identifierColorProperty();
  }

  /**
   * Returns the currently loaded instrument logo image.
   *
   * @return the observable {@link Image}, or {@code null} while no logo is available
   */
  public ObservableValue<Image> instrumentLogoImageProperty() {
    return model.instrumentLogoImageProperty();
  }

  /**
   * Reports whether either chart modal is open.
   *
   * @return the observable combined modal state
   */
  public ObservableBooleanValue modalOpenProperty() {
    return model.modalOpenProperty();
  }

  /**
   * Returns the observable instrument-search state.
   *
   * @return the observable instrument-search state
   */
  public ObservableBooleanValue instrumentSearchOpenProperty() {
    return model.instrumentSearchOpenProperty();
  }

  /**
   * Returns the observable interval-selection state.
   *
   * @return the observable interval-selection state
   */
  public ObservableBooleanValue intervalSelectionOpenProperty() {
    return model.intervalSelectionOpenProperty();
  }

  /**
   * Registers the action invoked when the canvas needs earlier history.
   *
   * @param callback the earlier-history request callback
   */
  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  /**
   * Registers the action invoked after an interval is selected.
   *
   * @param callback the selected-interval callback
   */
  public void setOnIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** Opens the instrument-search modal when no other chart modal is open. */
  public void showInstrumentSearch() {
    interactor.openInstrumentSearch();
  }

  /** Closes the instrument-search modal. */
  public void closeInstrumentSearch() {
    interactor.closeInstrumentSearch();
  }

  /** Opens the interval-selection modal when no other chart modal is open. */
  public void showIntervalSelection() {
    interactor.openIntervalSelection();
  }

  /** Closes the interval-selection modal. */
  public void closeIntervalSelection() {
    interactor.closeIntervalSelection();
  }

  /**
   * Sets the color used to identify this chart in its workspace.
   *
   * @param color the non-null identifier color
   */
  public void setIdentifierColor(Color color) {
    model.setIdentifierColor(Objects.requireNonNull(color, "color cannot be null"));
  }

  /**
   * Sets whether the workspace identifier is visible.
   *
   * @param value true to show the identifier
   */
  public void setIdentifierVisible(boolean value) {
    model.setIdentifierVisible(value);
  }

  /** Cancels any pending logo load before a caller-managed instrument load begins. */
  public void beginInstrumentLoad() {
    interactor.cancelInstrumentLogoLoad();
  }

  /**
   * Selects an instrument and requests its data when this chart owns a market-data session.
   *
   * @param symbol the instrument symbol to select
   */
  public void selectInstrument(String symbol) {
    interactor.closeInstrumentSearch();
    if (hasMarketDataSession) {
      interactor.selectInstrumentHistory(Objects.requireNonNull(symbol, "symbol cannot be null"));
    } else {
      interactor.selectInstrument(Objects.requireNonNull(symbol, "symbol cannot be null"));
    }
  }

  /**
   * Replaces the chart's instrument, price history, and optional logo.
   *
   * @param symbol the provider-normalized instrument symbol
   * @param bars the calendar price history for the instrument
   * @param logo the optional logo metadata to load for workspace-menu presentation
   */
  public void setInstrument(String symbol, List<CalendarData> bars, Optional<InstrumentLogo> logo) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    model.setSymbol(symbol);
    interactor.setInstrumentLogo(logo);
    canvas.setInstrumentPricePoints(pricePointsFromCalendarBars(bars));
  }

  /**
   * Replaces the displayed price points while preserving the current viewport semantics.
   *
   * @param pricePoints the ordered price points to display
   */
  public void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  /**
   * Replaces the displayed data with calendar bars.
   *
   * @param bars the ordered calendar bars to display
   */
  public void setBars(List<CalendarData> bars) {
    canvas.setPricePoints(pricePointsFromCalendarBars(bars));
  }

  /**
   * Changes the chart's rendering type.
   *
   * @param chartType the chart type to display
   */
  public void setChartType(ChartType chartType) {
    model.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
    canvas.setChartType(chartType);
  }

  /**
   * Selects an interval or requests interval-specific history when market data is available.
   *
   * @param interval the interval to select
   */
  public void setInterval(ChartInterval interval) {
    interactor.requestInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  /**
   * Reports whether this chart can request data for an interval.
   *
   * @param interval the proposed chart interval
   * @return true when the chart's provider can fetch bars for the interval
   */
  public boolean supportsInterval(ChartInterval interval) {
    return hasMarketDataSession ? interactor.supports(interval) : true;
  }

  /** Draws the chart immediately using its current state. */
  public void drawChart() {
    canvas.drawChart();
  }

  /** Loads the initial instrument through the configured chart data workflow. */
  public void loadInitialInstrument() {
    if (hasMarketDataSession) {
      interactor.loadInitialHistory(initialSymbol);
    } else {
      interactor.loadInitialInstrument(initialSymbol);
    }
  }

  /** Closes the chart's interactor and its owned logo session. */
  @Override
  public void close() {
    try {
      interactor.close();
    } finally {
      logoSession.close();
    }
  }

  /**
   * Applies a selected interval to the canvas and notifies the chart's external listener.
   *
   * @param interval the selected chart interval
   */
  private void applySelectedInterval(ChartInterval interval) {
    canvas.setInterval(interval);
    intervalSelectedHandler.accept(interval);
  }

  /**
   * Applies a legacy calendar-history instrument result and resolves its optional logo.
   *
   * @param instrument the loaded instrument and calendar history
   */
  private void applyLoadedInstrument(InstrumentLoadResult instrument) {
    setInstrument(instrument.symbol(), instrument.calendarData(), resolveInstrumentLogo(instrument.details()));
  }

  /**
   * Applies an interval-aware instrument-history result and resolves its optional logo.
   *
   * @param result the loaded instrument and interval-specific history
   */
  private void applyLoadedHistory(InstrumentHistoryLoadResult result) {
    model.setSymbol(result.symbol());
    interactor.setInstrumentLogo(resolveInstrumentLogo(result.details()));
    canvas.setInstrumentPricePoints(pricePointsFromHistory(result.history()));
  }

  /**
   * Adds an earlier page of calendar history to the canvas.
   *
   * @param bars the earlier ordered calendar bars
   */
  private void applyEarlierCalendarHistory(List<CalendarData> bars) {
    canvas.setPricePoints(pricePointsFromCalendarBars(bars));
  }

  /**
   * Adds an earlier interval-aware history page to the canvas.
   *
   * @param page the earlier ordered history page
   */
  private void applyEarlierSelectedHistory(HistoricalPage page) {
    canvas.setPricePoints(pricePointsFromHistory(page));
  }

  /**
   * Resolves logo metadata for an instrument while preserving the symbol fallback on failure.
   *
   * @param instrument the provider-neutral instrument metadata
   * @return the resolved logo metadata, or an empty value when unavailable
   */
  private Optional<InstrumentLogo> resolveInstrumentLogo(Instrument instrument) {
    try {
      return logoSession.findLogo(new LogoRequest(instrument.symbol(), instrument.exchange()));
    } catch (LogoException failure) {
      LOGGER.log(System.Logger.Level.WARNING, "Could not resolve instrument logo; keeping fallback icon", failure);
      return Optional.empty();
    }
  }

  /**
   * Converts an interval-aware history page into chart price points.
   *
   * @param page the history page to convert
   * @return the ordered chart price points
   */
  private static List<PricePoint> pricePointsFromHistory(HistoricalPage page) {
    return page.interval() instanceof HistoricalInterval.Calendar
      ? page.calendarData().stream().map(PricePoint::from).toList()
      : page.intradayData().stream().map(PricePoint::from).toList();
  }

  /**
   * Converts calendar bars into chart price points.
   *
   * @param bars the ordered calendar bars to convert
   * @return the ordered chart price points
   */
  private static List<PricePoint> pricePointsFromCalendarBars(List<CalendarData> bars) {
    Objects.requireNonNull(bars, "bars cannot be null");
    return bars.stream().map(PricePoint::from).toList();
  }

  /**
   * Reports a failure to load an earlier history page.
   *
   * @param failure the load failure
   */
  private static void reportEarlierHistoryLoadFailure(Throwable failure) {
    System.err.println("Unable to load earlier price history: " + failure.getMessage());
  }

  /**
   * Reports a failure to load an instrument.
   *
   * @param symbol the requested instrument symbol
   * @param failure the load failure
   */
  private static void reportInstrumentLoadFailure(String symbol, Throwable failure) {
    System.err.println("Unable to load " + symbol + ": " + failure.getMessage());
  }
}
