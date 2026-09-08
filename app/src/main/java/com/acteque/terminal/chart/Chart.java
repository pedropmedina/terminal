package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.search.InstrumentSearchDialog;
import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Composes the price chart canvas with its controls and overlays. */
public final class Chart extends StackPane {

  private final BooleanProperty instrumentSearchOpen = new SimpleBooleanProperty(false);
  private final BooleanProperty intervalSelectionOpen = new SimpleBooleanProperty(false);
  private final InstrumentSearchDialog instrumentSearchDialog;
  private final ChartIntervalSelectionDialog intervalSelectionDialog;
  private final ChartCanvas canvas;
  private final ChartStatusLine statusLine;

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    TiingoTickerCatalogApi tickerCatalog
  ) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");
    Objects.requireNonNull(tickerCatalog, "tickerCatalog");
    getStyleClass().add("chart");

    instrumentSearchDialog = new InstrumentSearchDialog(stockSymbol, instrumentSearchOpen, tickerCatalog);
    instrumentSearchDialog.onRequestClose(() -> instrumentSearchOpen.set(false));

    intervalSelectionDialog = new ChartIntervalSelectionDialog(interval, intervalSelectionOpen);
    intervalSelectionDialog.onRequestClose(() -> intervalSelectionOpen.set(false));

    statusLine = new ChartStatusLine(stockSymbol, interval);
    statusLine.onInstrumentClick(() -> instrumentSearchOpen.set(true));
    statusLine.onIntervalClick(() -> intervalSelectionOpen.set(true));
    intervalSelectionDialog.onIntervalSelected(statusLine::setInterval);

    ChartMenu menu = new ChartMenu();

    canvas = new ChartCanvas(pricePoints, interval, statusLine);

    VBox statusContent = new VBox(statusLine.logoAttribution(), statusLine);
    statusContent.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    statusContent.setPickOnBounds(false);
    StackPane statusOverlay = new StackPane(statusContent);
    statusOverlay.getStyleClass().add("chart-status-overlay");
    statusOverlay.setPickOnBounds(false);

    getChildren().setAll(canvas, menu, statusOverlay, instrumentSearchDialog, intervalSelectionDialog);

    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  public void setOnInstrumentSelected(Consumer<String> callback) {
    instrumentSearchDialog.onInstrumentSelected(callback);
  }

  public void setOnIntervalSelected(Consumer<ChartInterval> callback) {
    Objects.requireNonNull(callback, "callback");
    intervalSelectionDialog.onIntervalSelected(interval -> {
      statusLine.setInterval(interval);
      callback.accept(interval);
    });
  }

  public void setInstrument(String symbol, String displayName, List<PricePoint> pricePoints) {
    Objects.requireNonNull(symbol, "symbol");
    statusLine.setInstrumentName(displayName);
    instrumentSearchDialog.setCurrentSymbol(symbol);
    canvas.setInstrumentPricePoints(pricePoints);
  }

  public void setInstrumentLogo(InstrumentLogo logo, Image image) {
    statusLine.setInstrumentLogo(logo, image);
  }

  public void setOnOpenLink(Consumer<URI> callback) {
    statusLine.onOpenLink(callback);
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  public void drawChart() {
    canvas.drawChart();
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    instrumentSearchDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
    intervalSelectionDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
  }
}
