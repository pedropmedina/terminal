package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvas;
import com.acteque.terminal.chart.inspector.ChartInspector;
import com.acteque.terminal.chart.intervalselection.ChartIntervalSelection;
import com.acteque.terminal.chart.menu.ChartMenu;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import com.acteque.terminal.instrumentsearch.InstrumentSearch;
import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoRequest;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.drawer.Drawer;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/** Composes and exposes the chart's MVCI feature. */
public final class Chart implements AutoCloseable {

  private final LogoSession logoSource;
  private final ChartInteractor interactor;
  private final InstrumentSearch instrumentSearch;
  private final ChartCanvas canvas;
  private final ChartMenu menu;
  private final ChartInspector inspector;
  private final ChartStatusLine statusLine;
  private final ChartViewBuilder viewBuilder;
  private final ChartModel model;
  private final String initialSymbol;
  private final boolean ownsMarketData;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Consumer<ChartSplitDirection> splitRequestedHandler = ignored -> {};
  private Runnable closeRequestedHandler = () -> {};

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog
  ) {
    this(pricePoints, symbol, interval, instrumentCatalog, LogoSession.NONE, ForkJoinPool.commonPool(), Runnable::run);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    LogoSession logoSource,
    Executor uiExecutor
  ) {
    this(pricePoints, symbol, interval, instrumentCatalog, logoSource, null, ForkJoinPool.commonPool(), uiExecutor);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    LogoSession logoSource,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    this(pricePoints, symbol, interval, instrumentCatalog, logoSource, null, backgroundExecutor, uiExecutor);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    MarketDataSession marketData,
    Executor uiExecutor
  ) {
    this(
      pricePoints,
      symbol,
      interval,
      instrumentCatalog,
      LogoSession.NONE,
      Objects.requireNonNull(marketData, "marketData cannot be null"),
      ForkJoinPool.commonPool(),
      uiExecutor
    );
  }

  /** Owns both supplied sessions; provider instances may be shared through separate sessions. */
  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    MarketDataSession marketData,
    LogoSession logos,
    Executor uiExecutor
  ) {
    this(
      pricePoints,
      symbol,
      interval,
      instrumentCatalog,
      logos,
      Objects.requireNonNull(marketData, "marketData cannot be null"),
      ForkJoinPool.commonPool(),
      uiExecutor
    );
  }

  private Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    LogoSession logoSource,
    MarketDataSession marketData,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");
    Objects.requireNonNull(instrumentCatalog, "instrumentCatalog cannot be null");
    this.logoSource = Objects.requireNonNull(logoSource, "logoSource cannot be null");
    initialSymbol = symbol;
    ownsMarketData = marketData != null;
    model = new ChartModel();
    model.setSymbol(symbol);
    model.setChartType(ChartType.LINE);
    interactor = ownsMarketData
      ? new ChartInteractor(model, marketData, Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null"))
      : new ChartInteractor(model);
    interactor.initialize(interval);

    instrumentSearch = new InstrumentSearch(
      symbol,
      model.instrumentSearchOpenProperty(),
      instrumentCatalog,
      Objects.requireNonNull(backgroundExecutor, "backgroundExecutor cannot be null"),
      Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null")
    );
    instrumentSearch.onRequestClose(interactor::closeInstrumentSearch);

    ChartIntervalSelection intervalSelection = new ChartIntervalSelection(
      interval,
      model.intervalSelectionOpenProperty()
    );
    intervalSelection.onRequestClose(interactor::closeIntervalSelection);
    Dialog intervalSelectionDialog = intervalSelection.getView();

    statusLine = new ChartStatusLine(
      symbol,
      interval,
      model.modalOpenProperty(),
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection,
      logoSource,
      uiExecutor
    );
    intervalSelection.onIntervalSelected(interactor::selectInterval);

    menu = new ChartMenu(symbol, interval);
    menu.onInstrumentSelectionRequested(interactor::openInstrumentSearch);
    menu.onIntervalSelectionRequested(interactor::openIntervalSelection);
    inspector = new ChartInspector();
    inspector.onChartTypeSelected(this::setChartType);
    menu.onChartTypeSelectionRequested(inspector::showChartTypes);
    menu.onSplitRequested(this::requestSplit);
    menu.onCloseRequested(this::requestClose);
    Drawer inspectorDrawer = inspector.getView();
    inspector.openProperty().addListener((ignored, wasOpen, isOpen) -> menu.setChartTypeSelectionOpen(isOpen));
    model.modalOpenProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        inspector.close();
      }
    });

    canvas = new ChartCanvas(pricePoints, interval, statusLine);
    Canvas canvasView = canvas.getView();
    viewBuilder = new ChartViewBuilder(
      model,
      canvasView,
      menu.getView(),
      inspectorDrawer,
      statusLine.getView(),
      instrumentSearch.getView(),
      intervalSelectionDialog,
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection
    );
    interactor.onIntervalSelected(selectedInterval -> applySelectedInterval(selectedInterval, intervalSelection));
    if (ownsMarketData) {
      interactor.onInstrumentLoadStarted(statusLine::cancelLogoLoad);
      interactor.onInstrumentLoaded(this::applyLoadedInstrument);
      interactor.onEarlierHistoryLoaded(bars -> canvas.setPricePoints(toPricePoints(bars)));
      interactor.onInstrumentLoadFailed(Chart::reportInstrumentLoadFailure);
      interactor.onEarlierHistoryLoadFailed(Chart::reportEarlierHistoryLoadFailure);
      instrumentSearch.onInstrumentSelected(interactor::selectInstrument);
      canvas.setOnEarlierHistoryRequested(interactor::loadEarlierHistory);
    }
  }

  public StackPane getView() {
    return viewBuilder.build();
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  public void setOnInstrumentSelected(Consumer<String> callback) {
    instrumentSearch.onInstrumentSelected(callback);
  }

  public void setOnIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public String getSymbol() {
    return model.getSymbol();
  }

  public ChartInterval getInterval() {
    return model.getInterval();
  }

  public ChartType getChartType() {
    return model.getChartType();
  }

  public void onSplitRequested(Consumer<ChartSplitDirection> callback) {
    splitRequestedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onCloseRequested(Runnable callback) {
    closeRequestedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void setCloseAvailable(boolean value) {
    menu.setCloseAvailable(value);
  }

  public void setIdentifierColor(Color color) {
    model.setIdentifierColor(Objects.requireNonNull(color, "color cannot be null"));
  }

  public void setIdentifierVisible(boolean value) {
    model.setIdentifierVisible(value);
  }

  public void beginInstrumentLoad() {
    statusLine.cancelLogoLoad();
  }

  public void setInstrument(String symbol, String displayName, List<CalendarData> bars, Optional<InstrumentLogo> logo) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    model.setSymbol(symbol);
    statusLine.setInstrument(displayName, logo);
    instrumentSearch.setCurrentSymbol(symbol);
    canvas.setInstrumentPricePoints(toPricePoints(bars));
    menu.setInstrumentSymbol(symbol);
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  public void setBars(List<CalendarData> bars) {
    canvas.setPricePoints(toPricePoints(bars));
  }

  public void setChartType(ChartType chartType) {
    model.setChartType(Objects.requireNonNull(chartType, "chartType cannot be null"));
    canvas.setChartType(chartType);
    menu.setChartType(chartType);
    inspector.setChartType(chartType);
  }

  public void drawChart() {
    canvas.drawChart();
  }

  public void loadInitialInstrument() {
    interactor.loadInitialInstrument(initialSymbol);
  }

  @Override
  public void close() {
    menu.close();
    statusLine.cancelLogoLoad();
    try {
      logoSource.close();
    } finally {
      interactor.close();
    }
  }

  private void applySelectedInterval(ChartInterval interval, ChartIntervalSelection intervalSelection) {
    intervalSelection.setCurrentInterval(interval);
    statusLine.setInterval(interval);
    canvas.setInterval(interval);
    menu.setInterval(interval);
    intervalSelectedHandler.accept(interval);
  }

  private void requestSplit(ChartSplitDirection direction) {
    splitRequestedHandler.accept(direction);
  }

  private void requestClose() {
    closeRequestedHandler.run();
  }

  private void applyLoadedInstrument(InstrumentLoadResult instrument) {
    Optional<InstrumentLogo> logo = Optional.empty();
    try {
      logo = logoSource.findLogo(new LogoRequest(instrument.details().symbol(), instrument.details().exchange()));
    } catch (LogoException failure) {
      System.getLogger(Chart.class.getName()).log(
        System.Logger.Level.WARNING,
        "Could not resolve instrument logo; keeping fallback icon",
        failure
      );
    }
    setInstrument(instrument.symbol(), instrument.displayName(), instrument.calendarData(), logo);
  }

  private static void reportEarlierHistoryLoadFailure(Throwable failure) {
    System.err.println("Unable to load earlier price history: " + failure.getMessage());
  }

  private static void reportInstrumentLoadFailure(String symbol, Throwable failure) {
    System.err.println("Unable to load " + symbol + ": " + failure.getMessage());
  }

  private static List<PricePoint> toPricePoints(List<CalendarData> bars) {
    Objects.requireNonNull(bars, "bars cannot be null");
    return bars.stream().map(PricePoint::from).toList();
  }
}
