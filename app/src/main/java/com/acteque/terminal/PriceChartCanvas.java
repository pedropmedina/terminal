package com.acteque.terminal;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.acteque.terminal.XAxisTickCalculator.XAxisTick;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

final class PriceChartCanvas extends Canvas {

  // Padding
  private static final double LEFT_MARGIN = 28.0;
  private static final double RIGHT_MARGIN = 72.0;
  private static final double TOP_MARGIN = 52.0;
  private static final double BOTTOM_MARGIN = 64.0;

  // Pixels per x and y zoom adjustment
  private static final double X_ZOOM_PIXELS_PER_POINT = 8.0;
  private static final double Y_ZOOM_PIXELS_PER_STEP = 96.0;

  // Y zoom scale limits
  private static final double MIN_Y_ZOOM_SCALE = 0.2;
  private static final double MAX_Y_ZOOM_SCALE = 5.0;

  // X zoom minimum price points to display
  private static final int MIN_VISIBLE_POINTS = 8;

  // Tick count
  private static final int Y_TICK_COUNT = 5;

  // X zoom state
  private double xZoomDragStart;
  private int xZoomDragStartVisiblePricePointCount;

  // X pan state
  private double xPanDragStart;
  private int xPanDragStartOffset;

  // Y zoom state
  private double yZoomScale = 1.0;
  private double yZoomDragStart;
  private double yZoomDragStartZoomScale;

  // Mouse drag mode
  private DragMode dragMode = DragMode.NONE;

  // Size of 'visible' price points list as determined by x zoom
  private int visiblePricePointCount;

  // Positive values show older data; negative values leave empty space after the newest point.
  private int visiblePricePointOffset;

  // Track initial price points
  private final List<PricePoint> pricePoints;

  private final ChartInterval interval;

  // NOTE: No sure if title should be kept at the canvas level?
  private final String title;

  PriceChartCanvas(List<PricePoint> pricePoints, String title) {
    this(pricePoints, title, ChartInterval.DAILY);
  }

  PriceChartCanvas(List<PricePoint> pricePoints, String title, ChartInterval interval) {
    this.visiblePricePointCount = pricePoints.size();
    this.pricePoints = List.copyOf(pricePoints);
    this.title = title;
    this.interval = Objects.requireNonNull(interval);

    ChartReloadHooks.register(this);
    widthProperty().addListener((ignored, oldWidth, newWidth) -> drawChart());
    heightProperty().addListener((ignored, oldHeight, newHeight) -> drawChart());
    setEventsListeners();
  }

  void drawChart() {
    double width = getWidth();
    double height = getHeight();
    if (width <= 0 || height <= 0 || pricePoints.isEmpty()) {
      return;
    }

    GraphicsContext graphics = getGraphicsContext2D();
    graphics.setFill(Color.WHITE);
    graphics.fillRect(0, 0, width, height);

    ChartBounds bounds = chartBounds();
    VisibleWindow visibleWindow = visibleWindow();
    List<XAxisTick> xAxisTicks = xAxisTicks(bounds, visibleWindow);
    PriceRange priceRange = calculatePriceRange(visibleWindow.points());

    drawTitle(graphics, width);
    drawHorizontalGridLines(graphics, bounds);
    drawVerticalGridLines(graphics, bounds, xAxisTicks);
    drawAxes(graphics, bounds);
    drawYAxisTicks(graphics, bounds, priceRange);
    drawXAxisTicks(graphics, bounds, visibleWindow, xAxisTicks);
    drawPriceLine(graphics, bounds, priceRange, visibleWindow.points());
  }

  private void setEventsListeners() {
    setOnMouseMoved(event -> {
      updateCursor(event.getX(), event.getY());
    });

    setOnMouseExited(event -> {
      if (dragMode == DragMode.NONE) {
        setCursor(Cursor.DEFAULT);
      }
    });

    setOnMousePressed(event -> {
      ChartBounds bounds = chartBounds();

      if (isOverPriceAxisArea(event.getX(), event.getY(), bounds)) {
        dragMode = DragMode.ZOOM_PRICE;
        yZoomDragStart = event.getY();
        yZoomDragStartZoomScale = yZoomScale;
        setCursor(Cursor.V_RESIZE);
      } else if (isOverDateAxisArea(event.getY(), bounds)) {
        dragMode = DragMode.ZOOM_DATE;
        xZoomDragStart = event.getX();
        xZoomDragStartVisiblePricePointCount = visiblePricePointCount;
        setCursor(Cursor.H_RESIZE);
      } else if (isOverChartArea(event.getX(), event.getY(), bounds)) {
        dragMode = DragMode.PAN;
        xPanDragStart = event.getX();
        xPanDragStartOffset = visiblePricePointOffset;
        setCursor(Cursor.CLOSED_HAND);
      }
    });

    setOnMouseDragged(event -> {
      if (dragMode == DragMode.ZOOM_DATE) {
        handleXAxisZoom(event.getX());
      } else if (dragMode == DragMode.ZOOM_PRICE) {
        handleYAxisZoom(event.getY());
      } else if (dragMode == DragMode.PAN) {
        handleHorizontalPan(event.getX());
      }
    });

    setOnMouseReleased(event -> {
      dragMode = DragMode.NONE;
      updateCursor(event.getX(), event.getY());
    });
  }

  private void handleXAxisZoom(double x) {
    double xDelta = x - xZoomDragStart;
    int pointDelta = (int) Math.round(xDelta / X_ZOOM_PIXELS_PER_POINT);
    int requestedVisiblePointCount = xZoomDragStartVisiblePricePointCount - pointDelta;
    int clampedVisiblePointCount = clampVisiblePointCount(requestedVisiblePointCount);

    if (clampedVisiblePointCount != visiblePricePointCount) {
      visiblePricePointCount = clampedVisiblePointCount;
      visiblePricePointOffset = clampVisiblePointOffset(visiblePricePointOffset);
      drawChart();
    }
  }

  private void handleHorizontalPan(double x) {
    ChartBounds bounds = chartBounds();
    double pointSpacing = bounds.width() / Math.max(1, visiblePricePointCount - 1);
    int pointDelta = (int) Math.round((x - xPanDragStart) / pointSpacing);
    int clampedOffset = clampVisiblePointOffset(xPanDragStartOffset + pointDelta);

    if (clampedOffset != visiblePricePointOffset) {
      visiblePricePointOffset = clampedOffset;
      drawChart();
    }
  }

  private void handleYAxisZoom(double y) {
    double deltaY = y - yZoomDragStart;
    double requestedYZoomScale = yZoomDragStartZoomScale * Math.exp(deltaY / Y_ZOOM_PIXELS_PER_STEP);
    double clampedYZoomScale = clampYZoomScale(requestedYZoomScale);

    if (Double.compare(clampedYZoomScale, yZoomScale) != 0) {
      yZoomScale = clampedYZoomScale;
      drawChart();
    }
  }

  private PriceRange calculatePriceRange(List<PricePoint> points) {
    double min = Double.MAX_VALUE;
    double max = -Double.MAX_VALUE;
    for (PricePoint point : points) {
      min = Math.min(min, point.price());
      max = Math.max(max, point.price());
    }

    double range = max - min;
    double padding = range == 0 ? Math.max(1.0, max * 0.05) : range * 0.08;
    double defaultMin = min - padding;
    double defaultMax = max + padding;
    double midpoint = (defaultMin + defaultMax) / 2.0;
    double zoomedSpan = (defaultMax - defaultMin) * yZoomScale;
    return new PriceRange(midpoint - zoomedSpan / 2.0, midpoint + zoomedSpan / 2.0);
  }

  private void drawTitle(GraphicsContext graphics, double width) {
    graphics.setFill(Color.rgb(28, 32, 38));
    graphics.setFont(Font.font("System", 20));
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(title, width / 2.0, 24.0);
  }

  private void drawAxes(GraphicsContext graphics, ChartBounds bounds) {
    graphics.setStroke(Color.rgb(40, 44, 52));
    graphics.setLineWidth(1.4);
    graphics.strokeLine(bounds.right(), bounds.top(), bounds.right(), bounds.bottom());
    graphics.strokeLine(bounds.left(), bounds.bottom(), bounds.right(), bounds.bottom());

    graphics.setFill(Color.rgb(54, 61, 72));
    graphics.setFont(Font.font("System", 13));
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText("Time", bounds.left() + bounds.width() / 2.0, bounds.bottom() + 46.0);

    graphics.save();
    graphics.translate(bounds.right() + 54.0, bounds.top() + bounds.height() / 2.0);
    graphics.rotate(90.0);
    graphics.fillText("Price", 0, 0);
    graphics.restore();
  }

  private void drawHorizontalGridLines(GraphicsContext graphics, ChartBounds bounds) {
    graphics.setStroke(Color.rgb(223, 228, 236));
    graphics.setLineWidth(1.0);

    for (int tick = 0; tick <= Y_TICK_COUNT; tick++) {
      double y = yForTick(tick, bounds);
      graphics.strokeLine(bounds.left(), y, bounds.right(), y);
    }
  }

  private void drawYAxisTicks(GraphicsContext graphics, ChartBounds bounds, PriceRange priceRange) {
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.setStroke(Color.rgb(40, 44, 52));
    graphics.setLineWidth(1.0);
    graphics.setFill(Color.rgb(74, 82, 94));

    for (int tick = 0; tick <= Y_TICK_COUNT; tick++) {
      double ratio = (double) tick / Y_TICK_COUNT;
      double y = yForTick(tick, bounds);
      double price = priceRange.min() + ratio * priceRange.span();

      graphics.strokeLine(bounds.right(), y, bounds.right() + 5.0, y);
      graphics.fillText(String.format("$%.2f", price), bounds.right() + 10.0, y);
    }
  }

  private double yForTick(int tick, ChartBounds bounds) {
    double ratio = (double) tick / Y_TICK_COUNT;
    return bounds.bottom() - ratio * bounds.height();
  }

  private void drawVerticalGridLines(GraphicsContext graphics, ChartBounds bounds, List<XAxisTick> ticks) {
    graphics.setStroke(Color.rgb(223, 228, 236));
    graphics.setLineWidth(1.0);

    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), visiblePricePointCount, bounds);
      graphics.strokeLine(x, bounds.top(), x, bounds.bottom());
    }
  }

  private void drawXAxisTicks(
    GraphicsContext graphics,
    ChartBounds bounds,
    VisibleWindow visibleWindow,
    List<XAxisTick> ticks
  ) {
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);

    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), visiblePricePointCount, bounds);

      graphics.setStroke(Color.rgb(40, 44, 52));
      graphics.setLineWidth(1.0);
      graphics.strokeLine(x, bounds.bottom(), x, bounds.bottom() + 5.0);

      int pointIndex = tick.dataIndex() - visibleWindow.firstDataIndex();
      LocalDate tickDate;
      if (pointIndex >= 0 && pointIndex < visibleWindow.points().size()) {
        PricePoint point = visibleWindow.points().get(pointIndex);
        tickDate = point.date();
      } else {
        PricePoint newestPoint = pricePoints.get(pricePoints.size() - 1);
        int daysAfterNewestPoint = tick.dataIndex() - pricePoints.size() + 1;
        tickDate = newestPoint.date().plusDays(daysAfterNewestPoint);
      }

      graphics.setFill(Color.rgb(74, 82, 94));
      graphics.fillText(interval.format(tickDate), x, bounds.bottom() + 10.0);
    }
  }

  private List<XAxisTick> xAxisTicks(ChartBounds bounds, VisibleWindow visibleWindow) {
    return XAxisTickCalculator.calculate(
      pricePoints.size(),
      visibleWindow.firstDataIndex(),
      visiblePricePointCount,
      bounds.width(),
      interval
    );
  }

  private void drawPriceLine(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints
  ) {
    graphics.setStroke(Color.rgb(26, 115, 232));
    graphics.setLineWidth(2.2);
    graphics.save();
    graphics.beginPath();
    graphics.rect(bounds.left(), bounds.top(), bounds.width(), bounds.height());
    graphics.clip();

    for (int index = 1; index < visiblePoints.size(); index++) {
      PricePoint previous = visiblePoints.get(index - 1);
      PricePoint current = visiblePoints.get(index);

      graphics.strokeLine(
        xForSlot(index - 1, visiblePricePointCount, bounds),
        yForPrice(previous.price(), bounds, priceRange),
        xForSlot(index, visiblePricePointCount, bounds),
        yForPrice(current.price(), bounds, priceRange)
      );
    }
    graphics.restore();
  }

  private double xForSlot(int index, int pointCount, ChartBounds bounds) {
    if (pointCount == 1) {
      return bounds.left() + bounds.width() / 2.0;
    }
    return bounds.left() + ((double) index / (pointCount - 1)) * bounds.width();
  }

  private double yForPrice(double price, ChartBounds bounds, PriceRange priceRange) {
    double normalized = (price - priceRange.min()) / priceRange.span();
    return bounds.bottom() - normalized * bounds.height();
  }

  private ChartBounds chartBounds() {
    return new ChartBounds(
      LEFT_MARGIN,
      TOP_MARGIN,
      Math.max(1.0, getWidth() - LEFT_MARGIN - RIGHT_MARGIN),
      Math.max(1.0, getHeight() - TOP_MARGIN - BOTTOM_MARGIN)
    );
  }

  private boolean isOverDateAxisArea(double y, ChartBounds bounds) {
    return y >= bounds.bottom() && y <= getHeight();
  }

  private boolean isOverChartArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.left() && x <= bounds.right() && y >= bounds.top() && y <= bounds.bottom();
  }

  private boolean isOverPriceAxisArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.right() && x <= getWidth() && y >= bounds.top() && y <= bounds.bottom();
  }

  private VisibleWindow visibleWindow() {
    int clampedPointCount = clampVisiblePointCount(visiblePricePointCount);
    int clampedOffset = clampVisiblePointOffset(visiblePricePointOffset);
    int lastVisibleIndexExclusive = Math.min(pricePoints.size(), pricePoints.size() - clampedOffset);
    int displayedPointCount = clampedPointCount + Math.min(0, clampedOffset);
    int firstVisibleIndex = lastVisibleIndexExclusive - displayedPointCount;
    return new VisibleWindow(pricePoints.subList(firstVisibleIndex, lastVisibleIndexExclusive), firstVisibleIndex);
  }

  private int clampVisiblePointCount(int requestedPointCount) {
    int minimum = Math.min(MIN_VISIBLE_POINTS, pricePoints.size());
    return Math.max(minimum, Math.min(pricePoints.size(), requestedPointCount));
  }

  private int clampVisiblePointOffset(int requestedOffset) {
    int minimumOffset = -(visiblePricePointCount - 1);
    int maximumOffset = pricePoints.size() - visiblePricePointCount;
    return Math.max(minimumOffset, Math.min(maximumOffset, requestedOffset));
  }

  private double clampYZoomScale(double requestedYZoomScale) {
    return Math.max(MIN_Y_ZOOM_SCALE, Math.min(MAX_Y_ZOOM_SCALE, requestedYZoomScale));
  }

  private void updateCursor(double x, double y) {
    ChartBounds bounds = chartBounds();
    if (isOverPriceAxisArea(x, y, bounds)) {
      setCursor(Cursor.V_RESIZE);
    } else if (isOverDateAxisArea(y, bounds)) {
      setCursor(Cursor.H_RESIZE);
    } else if (isOverChartArea(x, y, bounds)) {
      setCursor(Cursor.OPEN_HAND);
    } else {
      setCursor(Cursor.DEFAULT);
    }
  }

  private enum DragMode {
    NONE,
    ZOOM_DATE,
    ZOOM_PRICE,
    PAN,
  }

  private record PriceRange(double min, double max) {
    double span() {
      return max - min;
    }
  }

  private record VisibleWindow(List<PricePoint> points, int firstDataIndex) {}

  private record ChartBounds(double left, double top, double width, double height) {
    double right() {
      return left + width;
    }

    double bottom() {
      return top + height;
    }
  }
}
