package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.search.InstrumentSearchDialog;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.StackPane;

/** Composes the price chart canvas with its controls and overlays. */
public final class Chart extends StackPane {

  private final BooleanProperty instrumentSearchOpen = new SimpleBooleanProperty(false);
  private final ChartCanvas canvas;

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    TiingoTickerCatalogApi tickerCatalog
  ) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");
    Objects.requireNonNull(tickerCatalog, "tickerCatalog");

    InstrumentSearchDialog instrumentSearchDialog = new InstrumentSearchDialog(
      stockSymbol,
      instrumentSearchOpen,
      tickerCatalog
    );
    instrumentSearchDialog.onOverlayClick(() -> instrumentSearchOpen.set(false));
    instrumentSearchDialog.onRequestClose(() -> instrumentSearchOpen.set(false));

    ChartStatusLine statusLine = new ChartStatusLine(stockSymbol, interval);
    statusLine.onInstrumentClick(() -> instrumentSearchOpen.set(true));

    ChartMenu menu = new ChartMenu();

    canvas = new ChartCanvas(pricePoints, interval, statusLine);

    getChildren().setAll(canvas, menu, statusLine, instrumentSearchDialog);

    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  public void drawChart() {
    canvas.drawChart();
  }
}
