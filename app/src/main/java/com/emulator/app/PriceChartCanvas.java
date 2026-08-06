package com.emulator.app;

import java.time.format.DateTimeFormatter;
import java.util.List;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

final class PriceChartCanvas extends Canvas {

  private static final DateTimeFormatter AXIS_DATE_FORMAT = DateTimeFormatter.ofPattern("MM/dd");

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

  // Tick counts
  private static final int Y_TICK_COUNT = 5;
  private static final int X_TICK_COUNT = 6;

  // X zoom state
  private double xZoomDragStart;
  private int xZoomDragStartVisiblePricePointCount;

  // Y zoom state
  private double yZoomScale = 1.0;
  private double yZoomDragStart;
  private double yZoomDragStartZoomScale;

  // Zoom drag mode
  private AxisZoomDragMode zoomDragMode = AxisZoomDragMode.NONE;

  // Size of 'visible' price points list as determined by x zoom
  private int visiblePricePointCount;

  // Track initial price points
  private final List<PricePoint> pricePoints;

  // NOTE: No sure if title should be kept at the canvas level?
  private final String title;

  PriceChartCanvas(List<PricePoint> pricePoints, String title) {
    this.visiblePricePointCount = pricePoints.size();
    this.pricePoints = List.copyOf(pricePoints);
    this.title = title;

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
    List<PricePoint> visiblePricePoints = visiblePricePoints();
    PriceRange priceRange = calculatePriceRange(visiblePricePoints);

    drawTitle(graphics, width);
    drawHorizontalGridLines(graphics, bounds);
    drawVerticalGridLines(graphics, bounds, visiblePricePoints);
    drawAxes(graphics, bounds);
    drawYAxisTicks(graphics, bounds, priceRange);
    drawXAxisTicks(graphics, bounds, visiblePricePoints);
    drawPriceLine(graphics, bounds, priceRange, visiblePricePoints);
  }

  private void setEventsListeners() {
    setOnMouseMoved(event -> {
      ChartBounds bounds = chartBounds();
      if (isOverPriceAxisArea(event.getX(), event.getY(), bounds)) {
        setCursor(Cursor.V_RESIZE);
      } else if (isOverDateAxisArea(event.getY(), bounds)) {
        setCursor(Cursor.H_RESIZE);
      } else {
        setCursor(Cursor.DEFAULT);
      }
    });

    setOnMouseExited(event -> {
      if (zoomDragMode == AxisZoomDragMode.NONE) {
        setCursor(Cursor.DEFAULT);
      }
    });

    setOnMousePressed(event -> {
      ChartBounds bounds = chartBounds();

      if (isOverPriceAxisArea(event.getX(), event.getY(), bounds)) {
        zoomDragMode = AxisZoomDragMode.PRICE;
        yZoomDragStart = event.getY();
        yZoomDragStartZoomScale = yZoomScale;
        setCursor(Cursor.V_RESIZE);
      } else if (isOverDateAxisArea(event.getY(), bounds)) {
        zoomDragMode = AxisZoomDragMode.DATE;
        xZoomDragStart = event.getX();
        xZoomDragStartVisiblePricePointCount = visiblePricePointCount;
        setCursor(Cursor.H_RESIZE);
      }
    });

    setOnMouseDragged(event -> {
      if (zoomDragMode == AxisZoomDragMode.DATE) {
        updateDateAxisZoom(event.getX());
      } else if (zoomDragMode == AxisZoomDragMode.PRICE) {
        updatePriceAxisZoom(event.getY());
      }
    });

    setOnMouseReleased(event -> zoomDragMode = AxisZoomDragMode.NONE);
  }

  private void updateDateAxisZoom(double x) {
    double deltaX = x - xZoomDragStart;
    int pointDelta = (int) Math.round(deltaX / X_ZOOM_PIXELS_PER_POINT);
    int requestedVisiblePointCount = xZoomDragStartVisiblePricePointCount - pointDelta;
    int clampedVisiblePointCount = clampVisiblePointCount(requestedVisiblePointCount);

    if (clampedVisiblePointCount != visiblePricePointCount) {
      visiblePricePointCount = clampedVisiblePointCount;
      drawChart();
    }
  }

  private void updatePriceAxisZoom(double y) {
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

  private void drawVerticalGridLines(GraphicsContext graphics, ChartBounds bounds, List<PricePoint> visiblePoints) {
    int ticks = xTickCount(visiblePoints);

    graphics.setStroke(Color.rgb(223, 228, 236));
    graphics.setLineWidth(1.0);

    for (int tick = 0; tick <= ticks; tick++) {
      double x = xForTick(tick, ticks, bounds);
      graphics.strokeLine(x, bounds.top(), x, bounds.bottom());
    }
  }

  private void drawXAxisTicks(GraphicsContext graphics, ChartBounds bounds, List<PricePoint> visiblePoints) {
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);

    int lastIndex = visiblePoints.size() - 1;
    int ticks = xTickCount(visiblePoints);
    for (int tick = 0; tick <= ticks; tick++) {
      double ratio = xTickRatio(tick, ticks);
      int index = (int) Math.round(ratio * lastIndex);
      double x = xForTick(tick, ticks, bounds);
      PricePoint point = visiblePoints.get(index);

      graphics.setStroke(Color.rgb(40, 44, 52));
      graphics.setLineWidth(1.0);
      graphics.strokeLine(x, bounds.bottom(), x, bounds.bottom() + 5.0);

      graphics.setFill(Color.rgb(74, 82, 94));
      graphics.fillText(point.date().format(AXIS_DATE_FORMAT), x, bounds.bottom() + 10.0);
    }
  }

  private int xTickCount(List<PricePoint> visiblePoints) {
    return Math.min(X_TICK_COUNT, visiblePoints.size() - 1);
  }

  private double xForTick(int tick, int ticks, ChartBounds bounds) {
    return bounds.left() + xTickRatio(tick, ticks) * bounds.width();
  }

  private double xTickRatio(int tick, int ticks) {
    return ticks == 0 ? 0.5 : (double) tick / ticks;
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
        xForIndex(index - 1, visiblePoints.size(), bounds),
        yForPrice(previous.price(), bounds, priceRange),
        xForIndex(index, visiblePoints.size(), bounds),
        yForPrice(current.price(), bounds, priceRange)
      );
    }
    graphics.restore();
  }

  private double xForIndex(int index, int pointCount, ChartBounds bounds) {
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

  private boolean isOverPriceAxisArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.right() && x <= getWidth() && y >= bounds.top() && y <= bounds.bottom();
  }

  private List<PricePoint> visiblePricePoints() {
    int clampedPointCount = clampVisiblePointCount(visiblePricePointCount);
    int firstVisibleIndex = Math.max(0, pricePoints.size() - clampedPointCount);
    return pricePoints.subList(firstVisibleIndex, pricePoints.size());
  }

  private int clampVisiblePointCount(int requestedPointCount) {
    int minimum = Math.min(MIN_VISIBLE_POINTS, pricePoints.size());
    return Math.max(minimum, Math.min(pricePoints.size(), requestedPointCount));
  }

  private double clampYZoomScale(double requestedYZoomScale) {
    return Math.max(MIN_Y_ZOOM_SCALE, Math.min(MAX_Y_ZOOM_SCALE, requestedYZoomScale));
  }

  private enum AxisZoomDragMode {
    NONE,
    DATE,
    PRICE,
  }

  private record PriceRange(double min, double max) {
    double span() {
      return max - min;
    }
  }

  private record ChartBounds(double left, double top, double width, double height) {
    double right() {
      return left + width;
    }

    double bottom() {
      return top + height;
    }
  }
}
