package com.acteque.terminal;

import java.util.List;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.StackPane;

/** Composes the price chart canvas with its controls and overlays. */
final class Chart extends StackPane {

  private final BooleanProperty instrumentSearchOpen = new SimpleBooleanProperty(false);
  private final ChartCanvas canvas;

  Chart(List<PricePoint> pricePoints, String stockSymbol, ChartInterval interval) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");

    InstrumentSearchDialog instrumentSearchDialog = new InstrumentSearchDialog(stockSymbol, instrumentSearchOpen);
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

  void setOnEarlierHistoryRequested(Runnable callback) {
    canvas.setOnEarlierHistoryRequested(callback);
  }

  void setPricePoints(List<PricePoint> pricePoints) {
    canvas.setPricePoints(pricePoints);
  }

  void drawChart() {
    canvas.drawChart();
  }
}
