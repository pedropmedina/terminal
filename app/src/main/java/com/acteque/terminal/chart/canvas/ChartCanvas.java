package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.chart.statusline.ChartStatusLine;
import java.util.List;
import java.util.Objects;
import javafx.scene.canvas.Canvas;

/** Composes and exposes the chart canvas's MVCI feature. */
public final class ChartCanvas {

  private final ChartCanvasInteractor interactor;
  private final ChartCanvasViewBuilder viewBuilder;

  public ChartCanvas(List<PricePoint> pricePoints, ChartInterval interval, ChartStatusLine statusLine) {
    Objects.requireNonNull(statusLine, "statusLine cannot be null");
    ChartCanvasModel model = new ChartCanvasModel();
    interactor = new ChartCanvasInteractor(model);
    interactor.initialize(pricePoints, interval);
    viewBuilder = new ChartCanvasViewBuilder(model, interactor);
    model.displayedPricePointProperty().addListener((ignored, oldPoint, point) -> {
      if (point == null) {
        statusLine.clearPricePoint();
      } else {
        statusLine.setPricePoint(point);
      }
    });
    PricePoint initialPoint = model.displayedPricePointProperty().get();
    if (initialPoint != null) {
      statusLine.setPricePoint(initialPoint);
    }
  }

  public Canvas getView() {
    return viewBuilder.build();
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    interactor.onEarlierHistoryRequested(callback);
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    interactor.setPricePoints(pricePoints);
  }

  public void setInstrumentPricePoints(List<PricePoint> pricePoints) {
    interactor.setInstrumentPricePoints(pricePoints);
  }

  public void setInterval(ChartInterval interval) {
    interactor.setInterval(interval);
  }

  public void setChartType(ChartType chartType) {
    interactor.setChartType(chartType);
  }

  public void drawChart() {
    viewBuilder.drawChart();
  }

  RenderStyle renderStyle() {
    return viewBuilder.renderStyle();
  }
}
