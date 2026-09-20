package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvas;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import com.acteque.terminal.marketdata.CalendarData;
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
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

/** Composes and exposes the chart's MVCI feature. */
public final class Chart implements AutoCloseable {

  private final LogoSession logoSource;
  private final ChartInteractor interactor;
  private final ChartCanvas canvas;
  private final ChartStatusLine statusLine;
  private final ChartViewBuilder viewBuilder;
  private final ChartModel model;
  private final String initialSymbol;
  private final boolean ownsMarketData;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  public Chart(List<PricePoint> pricePoints, String symbol, ChartInterval interval) {
    this(pricePoints, symbol, interval, LogoSession.NONE, null, Runnable::run);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    LogoSession logoSource,
    Executor uiExecutor
  ) {
    this(pricePoints, symbol, interval, logoSource, null, uiExecutor);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    MarketDataSession marketData,
    Executor uiExecutor
  ) {
    this(
      pricePoints,
      symbol,
      interval,
      LogoSession.NONE,
      Objects.requireNonNull(marketData, "marketData cannot be null"),
      uiExecutor
    );
  }

  /** Owns both supplied sessions; provider instances may be shared through separate sessions. */
  public Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    MarketDataSession marketData,
    LogoSession logos,
    Executor uiExecutor
  ) {
    this(
      pricePoints,
      symbol,
      interval,
      logos,
      Objects.requireNonNull(marketData, "marketData cannot be null"),
      uiExecutor
    );
  }

  private Chart(
    List<PricePoint> pricePoints,
    String symbol,
    ChartInterval interval,
    LogoSession logoSource,
    MarketDataSession marketData,
    Executor uiExecutor
  ) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    Objects.requireNonNull(interval, "interval cannot be null");
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

    statusLine = new ChartStatusLine(
      symbol,
      interval,
      model.modalOpenProperty(),
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection,
      logoSource,
      uiExecutor
    );
    canvas = new ChartCanvas(pricePoints, interval, statusLine);
    Canvas canvasView = canvas.getView();
    viewBuilder = new ChartViewBuilder(
      model,
      canvasView,
      statusLine.getView(),
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection
    );
    interactor.onIntervalSelected(this::applySelectedInterval);
    if (ownsMarketData) {
      interactor.onInstrumentLoadStarted(statusLine::cancelLogoLoad);
      interactor.onInstrumentLoaded(this::applyLoadedInstrument);
      interactor.onEarlierHistoryLoaded(bars -> canvas.setPricePoints(toPricePoints(bars)));
      interactor.onInstrumentLoadFailed(Chart::reportInstrumentLoadFailure);
      interactor.onEarlierHistoryLoadFailed(Chart::reportEarlierHistoryLoadFailure);
      canvas.setOnEarlierHistoryRequested(interactor::loadEarlierHistory);
    }
  }

  public StackPane getView() {
    return viewBuilder.build();
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  public void setOnIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public String getSymbol() {
    return model.getSymbol();
  }

  public ReadOnlyObjectProperty<String> symbolProperty() {
    return model.symbolProperty();
  }

  public ChartInterval getInterval() {
    return model.getInterval();
  }

  public ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return model.intervalProperty();
  }

  public ChartType getChartType() {
    return model.getChartType();
  }

  public ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return model.chartTypeProperty();
  }

  public ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return model.identifierColorProperty();
  }

  public ObservableBooleanValue modalOpenProperty() {
    return model.modalOpenProperty();
  }

  public ObservableBooleanValue instrumentSearchOpenProperty() {
    return model.instrumentSearchOpenProperty();
  }

  public ObservableBooleanValue intervalSelectionOpenProperty() {
    return model.intervalSelectionOpenProperty();
  }

  public void showInstrumentSearch() {
    interactor.openInstrumentSearch();
  }

  public void closeInstrumentSearch() {
    interactor.closeInstrumentSearch();
  }

  public void showIntervalSelection() {
    interactor.openIntervalSelection();
  }

  public void closeIntervalSelection() {
    interactor.closeIntervalSelection();
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

  public void selectInstrument(String symbol) {
    interactor.closeInstrumentSearch();
    interactor.selectInstrument(Objects.requireNonNull(symbol, "symbol cannot be null"));
  }

  public void setInstrument(String symbol, String displayName, List<CalendarData> bars, Optional<InstrumentLogo> logo) {
    Objects.requireNonNull(symbol, "symbol cannot be null");
    model.setSymbol(symbol);
    statusLine.setInstrument(displayName, logo);
    canvas.setInstrumentPricePoints(toPricePoints(bars));
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
  }

  public void setInterval(ChartInterval interval) {
    interactor.selectInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  public void drawChart() {
    canvas.drawChart();
  }

  public void loadInitialInstrument() {
    interactor.loadInitialInstrument(initialSymbol);
  }

  @Override
  public void close() {
    statusLine.cancelLogoLoad();
    try {
      logoSource.close();
    } finally {
      interactor.close();
    }
  }

  private void applySelectedInterval(ChartInterval interval) {
    statusLine.setInterval(interval);
    canvas.setInterval(interval);
    intervalSelectedHandler.accept(interval);
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
