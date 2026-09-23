package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.DragMode;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.PriceRange;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.VisibleWindow;
import java.util.List;
import java.util.Objects;

/** Applies chart-canvas state transitions without depending on JavaFX layout or drawing. */
final class ChartCanvasInteractor {

  static final int MIN_VISIBLE_POINTS = 8;
  static final int INITIAL_INTRADAY_VISIBLE_BARS = 500;
  static final double MAX_Y_ZOOM_SCALE = 5.0;
  static final double X_ZOOM_PIXELS_PER_POINT = 8.0;
  static final double Y_ZOOM_PIXELS_PER_STEP = 96.0;

  private final ChartCanvasModel model;
  private Runnable earlierHistoryRequested = () -> {};

  /** @param model the canvas state mutated by this interactor */
  ChartCanvasInteractor(ChartCanvasModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  /**
   * Initializes interval, data, and viewport state.
   *
   * @param pricePoints the initial ordered points
   * @param interval the initial interval
   */
  void initialize(List<PricePoint> pricePoints, ChartInterval interval) {
    model.interval = Objects.requireNonNull(interval, "interval cannot be null");
    model.pricePoints = List.copyOf(pricePoints);
    model.visiblePricePointCount = model.pricePoints.size();
    publishCurrentPoint();
  }

  /** @param callback the callback invoked near the oldest loaded point */
  void onEarlierHistoryRequested(Runnable callback) {
    earlierHistoryRequested = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param updatedPoints replacement points that retain viewport semantics */
  void setPricePoints(List<PricePoint> updatedPoints) {
    List<PricePoint> replacement = List.copyOf(updatedPoints);
    if (replacement.equals(model.pricePoints)) {
      return;
    }

    boolean establishingInitialData = model.pricePoints.isEmpty();
    model.pricePoints = replacement;
    if (establishingInitialData) {
      model.visiblePricePointCount = model.pricePoints.size();
    } else {
      model.visiblePricePointCount = clampVisiblePointCount(model.visiblePricePointCount);
    }
    model.visiblePricePointOffset = clampVisiblePointOffset(model.visiblePricePointOffset);
    publishCurrentPoint();
  }

  /** @param updatedPoints points for a new instrument that reset viewport state */
  void setInstrumentPricePoints(List<PricePoint> updatedPoints) {
    model.pricePoints = List.copyOf(updatedPoints);
    model.visiblePricePointCount = model.pricePoints.stream().anyMatch(point -> point.timestamp().isPresent())
      ? Math.min(INITIAL_INTRADAY_VISIBLE_BARS, model.pricePoints.size())
      : model.pricePoints.size();
    model.visiblePricePointOffset = 0;
    model.yZoomScale = 1.0;
    model.lockedPriceRange = null;
    model.hoveredVisiblePointIndex = null;
    model.crosshairX = null;
    model.crosshairY = null;
    model.dragMode = DragMode.NONE;
    model.priceAxisHovered = false;
    model.autoscaleButtonPressed = false;
    publishCurrentPoint();
  }

  /** @param interval the interval used by axis presentation */
  void setInterval(ChartInterval interval) {
    ChartInterval replacement = Objects.requireNonNull(interval, "interval cannot be null");
    if (replacement.equals(model.interval)) {
      return;
    }
    model.interval = replacement;
    model.publish(model.displayedPricePointProperty().get());
  }

  /** @param chartType the series rendering type */
  void setChartType(ChartType chartType) {
    ChartType replacement = Objects.requireNonNull(chartType, "chartType cannot be null");
    if (replacement == model.chartType) {
      return;
    }
    model.chartType = replacement;
    model.publish(model.displayedPricePointProperty().get());
  }

  /**
   * Publishes current pointer and hover state.
   *
   * @param pointIndex the hovered visible point index, or {@code null}
   * @param crosshairX the crosshair x coordinate, or {@code null}
   * @param crosshairY the crosshair y coordinate, or {@code null}
   * @param priceAxisHovered true when the price-axis controls are hovered
   */
  void movePointer(Integer pointIndex, Double crosshairX, Double crosshairY, boolean priceAxisHovered) {
    if (
      Objects.equals(pointIndex, model.hoveredVisiblePointIndex) &&
      Objects.equals(crosshairX, model.crosshairX) &&
      Objects.equals(crosshairY, model.crosshairY) &&
      priceAxisHovered == model.priceAxisHovered
    ) {
      return;
    }
    model.hoveredVisiblePointIndex = pointIndex;
    model.crosshairX = crosshairX;
    model.crosshairY = crosshairY;
    model.priceAxisHovered = priceAxisHovered;
    publishCurrentPoint();
  }

  /** Clears pointer state after the pointer exits. */
  void exitPointer() {
    if (
      model.hoveredVisiblePointIndex == null &&
      model.crosshairX == null &&
      model.crosshairY == null &&
      !model.priceAxisHovered
    ) {
      return;
    }
    model.hoveredVisiblePointIndex = null;
    model.crosshairX = null;
    model.crosshairY = null;
    model.priceAxisHovered = false;
    publishCurrentPoint();
  }

  /** Resets interaction state for a press outside actionable regions. */
  void beginEmptyPress() {
    model.dragMode = DragMode.NONE;
    model.autoscaleButtonPressed = false;
  }

  /** Starts a possible autoscale-button activation. */
  void beginAutoscalePress() {
    model.autoscaleButtonPressed = true;
    model.dragMode = DragMode.NONE;
  }

  /**
   * Starts price-axis zooming.
   *
   * @param y the initial pointer y coordinate
   * @param displayedPriceRange the range displayed at drag start
   */
  void beginYAxisZoom(double y, PriceRange displayedPriceRange) {
    model.autoscaleButtonPressed = false;
    model.dragMode = DragMode.ZOOM_PRICE;
    model.yZoomDragStart = y;
    model.yZoomDragStartZoomScale = model.yZoomScale;
    model.yZoomDragStartPriceRange = displayedPriceRange;
    model.yZoomDragDefaultPriceSpan = displayedPriceRange.span() / model.yZoomScale;
  }

  /** @param x the initial pointer x coordinate */
  void beginXAxisZoom(double x) {
    model.autoscaleButtonPressed = false;
    model.dragMode = DragMode.ZOOM_DATE;
    model.xZoomDragStart = x;
    model.xZoomDragStartVisiblePricePointCount = model.visiblePricePointCount;
  }

  /**
   * Starts chart panning.
   *
   * @param x the initial pointer x coordinate
   * @param y the initial pointer y coordinate
   */
  void beginPan(double x, double y) {
    model.autoscaleButtonPressed = false;
    model.dragMode = DragMode.PAN;
    model.xPanDragStart = x;
    model.xPanDragStartOffset = model.visiblePricePointOffset;
    model.yPanDragStart = y;
    model.yPanDragStartPriceRange = model.lockedPriceRange;
  }

  /**
   * Ends the current press and optionally toggles autoscaling.
   *
   * @param toggleAutoscale true when release activates the autoscale button
   * @param displayedPriceRange the currently displayed price range
   */
  void endPress(boolean toggleAutoscale, PriceRange displayedPriceRange) {
    model.autoscaleButtonPressed = false;
    model.dragMode = DragMode.NONE;
    if (toggleAutoscale) {
      togglePriceRangeAutoscale(displayedPriceRange);
    }
  }

  /** @param displayedPriceRange the range locked when disabling autoscale */
  private void togglePriceRangeAutoscale(PriceRange displayedPriceRange) {
    if (model.lockedPriceRange == null) {
      model.lockedPriceRange = Objects.requireNonNull(displayedPriceRange, "displayedPriceRange cannot be null");
    } else {
      model.yZoomScale = 1.0;
      model.lockedPriceRange = null;
    }
    publishCurrentPoint();
  }

  /** @param x the current pointer x coordinate */
  void handleXAxisZoom(double x) {
    double xDelta = x - model.xZoomDragStart;
    int pointDelta = (int) Math.round(xDelta / X_ZOOM_PIXELS_PER_POINT);
    int requestedVisiblePointCount = model.xZoomDragStartVisiblePricePointCount - pointDelta;
    int clampedVisiblePointCount = clampVisiblePointCount(requestedVisiblePointCount);
    if (clampedVisiblePointCount == model.visiblePricePointCount) {
      return;
    }
    model.visiblePricePointCount = clampedVisiblePointCount;
    model.visiblePricePointOffset = clampVisiblePointOffset(model.visiblePricePointOffset);
    publishCurrentPoint();
  }

  /**
   * Applies horizontal and optional vertical panning.
   *
   * @param x the current pointer x coordinate
   * @param y the current pointer y coordinate
   * @param chartWidth the drawable chart width
   * @param chartHeight the drawable chart height
   */
  void handlePan(double x, double y, double chartWidth, double chartHeight) {
    double pointSpacing = chartWidth / Math.max(1, model.visiblePricePointCount - 1);
    int pointDelta = (int) Math.round((x - model.xPanDragStart) / pointSpacing);
    int clampedOffset = clampVisiblePointOffset(model.xPanDragStartOffset + pointDelta);
    boolean changed = false;
    if (clampedOffset != model.visiblePricePointOffset) {
      model.visiblePricePointOffset = clampedOffset;
      changed = true;
    }
    if (model.yPanDragStartPriceRange != null) {
      double priceDelta = ((y - model.yPanDragStart) * model.yPanDragStartPriceRange.span()) / chartHeight;
      PriceRange pannedPriceRange = model.yPanDragStartPriceRange.translate(priceDelta);
      if (!pannedPriceRange.equals(model.lockedPriceRange)) {
        model.lockedPriceRange = pannedPriceRange;
        changed = true;
      }
    }
    if (changed) {
      publishCurrentPoint();
    }
    requestEarlierHistoryIfNeeded();
  }

  /**
   * Applies price-axis zooming.
   *
   * @param y the current pointer y coordinate
   * @param chartHeight the drawable chart height
   */
  void handleYAxisZoom(double y, double chartHeight) {
    double deltaY = y - model.yZoomDragStart;
    double requestedScale = model.yZoomDragStartZoomScale * Math.exp(deltaY / Y_ZOOM_PIXELS_PER_STEP);
    double clampedScale = clampYZoomScale(requestedScale, model.yZoomDragDefaultPriceSpan, chartHeight);
    if (Double.compare(clampedScale, model.yZoomScale) == 0) {
      return;
    }
    double scaleChange = clampedScale / model.yZoomDragStartZoomScale;
    model.yZoomScale = clampedScale;
    model.lockedPriceRange = model.yZoomDragStartPriceRange.scale(scaleChange);
    publishCurrentPoint();
  }

  /** @return the clamped visible data window and its source index */
  VisibleWindow visibleWindow() {
    if (model.pricePoints.isEmpty()) {
      return new VisibleWindow(List.of(), 0);
    }
    int pointCount = clampVisiblePointCount(model.visiblePricePointCount);
    int offset = clampVisiblePointOffset(model.visiblePricePointOffset);
    int lastExclusive = Math.min(model.pricePoints.size(), model.pricePoints.size() - offset);
    int displayedCount = pointCount + Math.min(0, offset);
    int first = lastExclusive - displayedCount;
    return new VisibleWindow(model.pricePoints.subList(first, lastExclusive), first);
  }

  /**
   * @param requestedPointCount the proposed visible slot count
   * @return the count clamped to available data and minimum zoom
   */
  int clampVisiblePointCount(int requestedPointCount) {
    int minimum = Math.min(MIN_VISIBLE_POINTS, model.pricePoints.size());
    return Math.max(minimum, Math.min(model.pricePoints.size(), requestedPointCount));
  }

  /**
   * @param requestedOffset the proposed viewport offset
   * @return the offset clamped to loaded data and projected future slots
   */
  int clampVisiblePointOffset(int requestedOffset) {
    if (model.pricePoints.isEmpty()) {
      return 0;
    }
    int minimumOffset = -(model.visiblePricePointCount - 1);
    int maximumOffset = model.pricePoints.size() - model.visiblePricePointCount;
    return Math.max(minimumOffset, Math.min(maximumOffset, requestedOffset));
  }

  /**
   * @param requestedScale the proposed vertical scale
   * @param defaultPriceSpan the automatic-range span
   * @param chartHeight the drawable chart height
   * @return a collision-safe bounded scale
   */
  double clampYZoomScale(double requestedScale, double defaultPriceSpan, double chartHeight) {
    double minimumScale = YAxisPolicy.minimumPriceSpan(chartHeight) / defaultPriceSpan;
    return Math.max(minimumScale, Math.min(MAX_Y_ZOOM_SCALE, requestedScale));
  }

  /**
   * @param chartHeight the drawable chart height
   * @return the locked or automatically calculated displayed price range
   */
  PriceRange displayedPriceRange(double chartHeight) {
    if (model.lockedPriceRange != null) {
      return model.lockedPriceRange;
    }
    PriceRange defaultRange = calculateDefaultPriceRange(visibleWindow().points());
    model.yZoomScale = clampYZoomScale(model.yZoomScale, defaultRange.span(), chartHeight);
    return scalePriceRange(defaultRange, model.yZoomScale);
  }

  /**
   * @param points the visible price points
   * @return a padded range based on closes or full OHLC values for bar types
   */
  PriceRange calculateDefaultPriceRange(List<PricePoint> points) {
    boolean usesHighLowRange = model.chartType == ChartType.BAR || model.chartType == ChartType.CANDLESTICK;
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (PricePoint point : points) {
      min = Math.min(min, usesHighLowRange ? point.low() : point.close());
      max = Math.max(max, usesHighLowRange ? point.high() : point.close());
    }
    double range = max - min;
    double padding = range == 0 ? Math.max(1.0, max * 0.05) : range * 0.08;
    return new PriceRange(min - padding, max + padding);
  }

  /**
   * @param defaultRange the automatic range
   * @param zoomScale the scale applied around its midpoint
   * @return the scaled price range
   */
  PriceRange scalePriceRange(PriceRange defaultRange, double zoomScale) {
    double midpoint = (defaultRange.min() + defaultRange.max()) / 2.0;
    double zoomedSpan = defaultRange.span() * zoomScale;
    return new PriceRange(midpoint - zoomedSpan / 2.0, midpoint + zoomedSpan / 2.0);
  }

  /** Requests more history when the viewport approaches the oldest loaded point. */
  private void requestEarlierHistoryIfNeeded() {
    if (model.pricePoints.isEmpty() || visibleWindow().firstDataIndex() > model.visiblePricePointCount) {
      return;
    }
    earlierHistoryRequested.run();
  }

  /** Publishes the hovered point or newest visible point. */
  private void publishCurrentPoint() {
    VisibleWindow window = visibleWindow();
    if (window.points().isEmpty()) {
      model.publish(null);
      return;
    }
    int index =
      model.hoveredVisiblePointIndex == null
        ? window.points().size() - 1
        : Math.min(model.hoveredVisiblePointIndex, window.points().size() - 1);
    model.publish(window.points().get(index));
  }
}
