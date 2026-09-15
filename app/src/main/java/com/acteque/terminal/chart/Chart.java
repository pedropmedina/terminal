package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvas;
import com.acteque.terminal.chart.intervalselection.ChartIntervalSelection;
import com.acteque.terminal.chart.menu.ChartMenu;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.search.InstrumentSearch;
import com.acteque.terminal.ui.core.dialog.Dialog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

/** Composes and exposes the chart's MVCI feature. */
public final class Chart {

  private final ChartInteractor interactor;
  private final InstrumentSearch instrumentSearch;
  private final ChartCanvas canvas;
  private final ChartStatusLine statusLine;
  private final ChartViewBuilder viewBuilder;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog
  ) {
    this(
      pricePoints,
      stockSymbol,
      interval,
      instrumentCatalog,
      ignored -> java.util.concurrent.CompletableFuture.completedFuture(Optional.empty()),
      ForkJoinPool.commonPool(),
      Runnable::run
    );
  }

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    ChartLogoSource logoSource,
    Executor uiExecutor
  ) {
    this(pricePoints, stockSymbol, interval, instrumentCatalog, logoSource, ForkJoinPool.commonPool(), uiExecutor);
  }

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    InstrumentCatalog instrumentCatalog,
    ChartLogoSource logoSource,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");
    Objects.requireNonNull(instrumentCatalog, "instrumentCatalog");
    ChartModel model = new ChartModel();
    interactor = new ChartInteractor(model);
    interactor.initialize(interval);

    instrumentSearch = new InstrumentSearch(
      stockSymbol,
      model.instrumentSearchOpenProperty(),
      instrumentCatalog,
      Objects.requireNonNull(backgroundExecutor, "backgroundExecutor"),
      Objects.requireNonNull(uiExecutor, "uiExecutor")
    );
    instrumentSearch.onRequestClose(interactor::closeInstrumentSearch);

    ChartIntervalSelection intervalSelection = new ChartIntervalSelection(
      interval,
      model.intervalSelectionOpenProperty()
    );
    intervalSelection.onRequestClose(interactor::closeIntervalSelection);
    Dialog intervalSelectionDialog = intervalSelection.getView();

    statusLine = new ChartStatusLine(
      stockSymbol,
      interval,
      model.modalOpenProperty(),
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection,
      logoSource,
      uiExecutor
    );
    intervalSelection.onIntervalSelected(interactor::selectInterval);

    ChartMenu menu = new ChartMenu();
    menu.onInstrumentSelectionRequested(interactor::openInstrumentSearch);
    menu.onIntervalSelectionRequested(interactor::openIntervalSelection);

    canvas = new ChartCanvas(pricePoints, interval, statusLine);
    Canvas canvasView = canvas.getView();
    viewBuilder = new ChartViewBuilder(
      model,
      canvasView,
      menu.getView(),
      statusLine.getView(),
      instrumentSearch.getView(),
      intervalSelectionDialog,
      interactor::openInstrumentSearch,
      interactor::openIntervalSelection
    );
    interactor.onIntervalSelected(selectedInterval -> applySelectedInterval(selectedInterval, intervalSelection));
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
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback");
  }

  public void beginInstrumentLoad() {
    statusLine.cancelLogoLoad();
  }

  public void setInstrument(String symbol, String displayName, List<DailyBar> bars, Optional<InstrumentLogo> logo) {
    Objects.requireNonNull(symbol, "symbol");
    statusLine.setInstrument(displayName, logo);
    instrumentSearch.setCurrentSymbol(symbol);
    canvas.setInstrumentPricePoints(toPricePoints(bars));
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  public void setBars(List<DailyBar> bars) {
    canvas.setPricePoints(toPricePoints(bars));
  }

  public void drawChart() {
    canvas.drawChart();
  }

  private void applySelectedInterval(ChartInterval interval, ChartIntervalSelection intervalSelection) {
    intervalSelection.setCurrentInterval(interval);
    statusLine.setInterval(interval);
    canvas.setInterval(interval);
    intervalSelectedHandler.accept(interval);
  }

  private static List<PricePoint> toPricePoints(List<DailyBar> bars) {
    Objects.requireNonNull(bars, "bars");
    return bars.stream().map(PricePoint::from).toList();
  }
}
