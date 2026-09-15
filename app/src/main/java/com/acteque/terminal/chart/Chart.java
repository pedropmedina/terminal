package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvas;
import com.acteque.terminal.chart.intervalselection.ChartIntervalSelection;
import com.acteque.terminal.chart.menu.ChartMenu;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.search.InstrumentSearchDialog;
import com.acteque.terminal.ui.core.dialog.Dialog;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.scene.canvas.Canvas;
import javafx.scene.layout.StackPane;

/** Composes and exposes the chart's MVCI feature. */
public final class Chart {

  private final ChartInteractor interactor;
  private final InstrumentSearchDialog instrumentSearchDialog;
  private final ChartCanvas canvas;
  private final ChartStatusLine statusLine;
  private final ChartViewBuilder viewBuilder;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    TiingoTickerCatalogApi tickerCatalog
  ) {
    this(
      pricePoints,
      stockSymbol,
      interval,
      tickerCatalog,
      ignored -> java.util.concurrent.CompletableFuture.completedFuture(Optional.empty()),
      Runnable::run
    );
  }

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    TiingoTickerCatalogApi tickerCatalog,
    ChartLogoSource logoSource,
    Executor uiExecutor
  ) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");
    Objects.requireNonNull(tickerCatalog, "tickerCatalog");
    ChartModel model = new ChartModel();
    interactor = new ChartInteractor(model);
    interactor.initialize(interval);

    instrumentSearchDialog = new InstrumentSearchDialog(
      stockSymbol,
      model.instrumentSearchOpenProperty(),
      tickerCatalog
    );
    instrumentSearchDialog.onRequestClose(interactor::closeInstrumentSearch);

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
      instrumentSearchDialog,
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
    instrumentSearchDialog.onInstrumentSelected(callback);
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
    instrumentSearchDialog.setCurrentSymbol(symbol);
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
