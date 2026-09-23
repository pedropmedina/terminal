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

  /**
   * Creates and connects a chart canvas.
   *
   * <p>The public parameter order is retained for source compatibility.
   *
   * @param pricePoints the initial ordered price points
   * @param interval the initial chart interval
   * @param statusLine the status line that displays the active price point
   */
  public ChartCanvas(List<PricePoint> pricePoints, ChartInterval interval, ChartStatusLine statusLine) {
    List<PricePoint> initialPricePoints = List.copyOf(pricePoints);
    ChartInterval initialInterval = Objects.requireNonNull(interval, "interval cannot be null");
    ChartStatusLine connectedStatusLine = Objects.requireNonNull(statusLine, "statusLine cannot be null");

    ChartCanvasModel model = createModel();
    interactor = new ChartCanvasInteractor(model);
    interactor.initialize(initialPricePoints, initialInterval);
    viewBuilder = new ChartCanvasViewBuilder(model, interactor);

    connectStatusLine(model, connectedStatusLine);
  }

  /**
   * Creates the observable canvas model.
   *
   * @return a new model with default viewport state
   */
  private static ChartCanvasModel createModel() {
    return new ChartCanvasModel();
  }

  /**
   * Forwards the currently displayed point from the canvas model to the status line.
   *
   * @param model the canvas model to observe
   * @param statusLine the status line receiving point changes
   */
  private static void connectStatusLine(ChartCanvasModel model, ChartStatusLine statusLine) {
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

  /**
   * Returns the canvas node.
   *
   * @return the reusable chart canvas
   */
  public Canvas getView() {
    return viewBuilder.build();
  }

  /**
   * Registers the callback invoked when panning approaches the oldest loaded data.
   *
   * @param callback the earlier-history request callback
   */
  public void setOnEarlierHistoryRequested(Runnable callback) {
    interactor.onEarlierHistoryRequested(callback);
  }

  /**
   * Replaces data while retaining the current viewport where possible.
   *
   * @param pricePoints the replacement ordered price points
   */
  public void setPricePoints(List<PricePoint> pricePoints) {
    interactor.setPricePoints(pricePoints);
  }

  /**
   * Replaces data for a newly selected instrument and resets the viewport.
   *
   * @param pricePoints the new instrument's ordered price points
   */
  public void setInstrumentPricePoints(List<PricePoint> pricePoints) {
    interactor.setInstrumentPricePoints(pricePoints);
  }

  /**
   * Sets the interval used for date-axis projections.
   *
   * @param interval the selected interval
   */
  public void setInterval(ChartInterval interval) {
    interactor.setInterval(interval);
  }

  /**
   * Sets the series rendering type.
   *
   * @param chartType the selected chart type
   */
  public void setChartType(ChartType chartType) {
    interactor.setChartType(chartType);
  }

  /** Draws the canvas immediately from the current model state. */
  public void drawChart() {
    viewBuilder.drawChart();
  }

  /**
   * Returns the currently resolved CSS drawing style for tests and rendering.
   *
   * @return the resolved render style
   */
  RenderStyle renderStyle() {
    return viewBuilder.renderStyle();
  }
}
