package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.PriceRange;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.VisibleWindow;
import com.acteque.terminal.chart.canvas.XAxisTickCalculator.XAxisTick;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

/** Renders chart-canvas state and provides its drawing-coordinate geometry. */
final class CanvasRenderer {

  private static final double LEFT_MARGIN = 0.0;
  private static final double RIGHT_MARGIN = 64.0;
  private static final double TOP_MARGIN = 0.0;
  private static final double BOTTOM_MARGIN = 32.0;
  private static final double CURRENT_PRICE_TEXT_OFFSET = 10.0;
  private static final double AUTOSCALE_BUTTON_SIZE = 24.0;
  private static final double AUTOSCALE_BUTTON_X_OFFSET = 8.0;
  private static final double AUTOSCALE_BUTTON_Y_OFFSET = 0.0;

  private final Canvas canvas;
  private final ChartCanvasModel model;
  private final CrosshairRenderer crosshair;

  CanvasRenderer(Canvas canvas, ChartCanvasModel model) {
    this.canvas = Objects.requireNonNull(canvas, "canvas");
    this.model = Objects.requireNonNull(model, "model");
    crosshair = new CrosshairRenderer();
  }

  void draw(RenderStyle style, VisibleWindow visibleWindow, PriceRange priceRange) {
    double width = canvas.getWidth();
    double height = canvas.getHeight();
    if (width <= 0 || height <= 0) {
      return;
    }

    GraphicsContext graphics = canvas.getGraphicsContext2D();
    graphics.setFill(style.background());
    graphics.fillRect(0, 0, width, height);
    if (model.pricePoints.isEmpty()) {
      return;
    }

    ChartBounds bounds = chartBounds();
    List<XAxisTick> xAxisTicks = xAxisTicks(bounds, visibleWindow, style);
    List<Double> yAxisTicks = YAxisTickCalculator.calculate(
      priceRange.min(),
      priceRange.max(),
      bounds.height(),
      model.yZoomScale
    );

    drawHorizontalGridLines(graphics, bounds, priceRange, yAxisTicks, style);
    drawVerticalGridLines(graphics, bounds, xAxisTicks, style);
    drawAxes(graphics, bounds, style);
    drawYAxisTicks(graphics, bounds, priceRange, yAxisTicks, style);
    drawXAxisTicks(graphics, bounds, xAxisTicks, style);
    drawPriceLine(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCurrentPriceBadge(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCrosshair(graphics, bounds, priceRange, visibleWindow, style);
    drawAutoscaleButton(graphics, bounds, style);
  }

  ChartBounds chartBounds() {
    return new ChartBounds(
      LEFT_MARGIN,
      TOP_MARGIN,
      Math.max(1.0, canvas.getWidth() - LEFT_MARGIN - RIGHT_MARGIN),
      Math.max(1.0, canvas.getHeight() - TOP_MARGIN - BOTTOM_MARGIN)
    );
  }

  boolean isOverDateAxisArea(double y, ChartBounds bounds) {
    return y >= bounds.bottom() && y <= canvas.getHeight();
  }

  boolean isOverChartArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.left() && x <= bounds.right() && y >= bounds.top() && y <= bounds.bottom();
  }

  boolean isOverPriceAxisArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.right() && x <= canvas.getWidth() && y >= bounds.top() && y <= bounds.bottom();
  }

  boolean isOverAutoscaleButton(double x, double y, ChartBounds bounds) {
    ButtonBounds button = autoscaleButtonBounds(bounds);
    return x >= button.x() && x <= button.x() + button.width() && y >= button.y() && y <= button.y() + button.height();
  }

  int slotIndexForX(double x, ChartBounds bounds) {
    return (int) Math.round(((x - bounds.left()) / bounds.width()) * Math.max(0, model.visiblePricePointCount - 1));
  }

  private void drawAxes(GraphicsContext graphics, ChartBounds bounds, RenderStyle style) {
    graphics.setStroke(style.axis());
    graphics.setLineWidth(style.axisLineWidth());
    graphics.strokeLine(bounds.right(), bounds.top(), bounds.right(), bounds.bottom());
    graphics.strokeLine(bounds.left(), bounds.bottom(), bounds.right(), bounds.bottom());
  }

  private void drawAutoscaleButton(GraphicsContext graphics, ChartBounds bounds, RenderStyle style) {
    if (!model.priceAxisHovered) {
      return;
    }

    ButtonBounds button = autoscaleButtonBounds(bounds);
    boolean autoscaleActive = model.lockedPriceRange == null;
    graphics.setFill(autoscaleActive ? style.primary() : style.background());
    graphics.fillRoundRect(
      button.x(),
      button.y(),
      button.width(),
      button.height(),
      style.controlRadius(),
      style.controlRadius()
    );
    graphics.setStroke(style.primary());
    graphics.setLineWidth(style.gridLineWidth());
    graphics.strokeRoundRect(
      button.x(),
      button.y(),
      button.width(),
      button.height(),
      style.controlRadius(),
      style.controlRadius()
    );

    graphics.setFill(autoscaleActive ? style.primaryForeground() : style.primary());
    graphics.setFont(style.badgeFont());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText("A", button.x() + button.width() / 2.0, button.y() + button.height() / 2.0);
  }

  private void drawHorizontalGridLines(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<Double> ticks,
    RenderStyle style
  ) {
    graphics.setStroke(style.grid());
    graphics.setLineWidth(style.gridLineWidth());
    for (double price : ticks) {
      double y = yForPrice(price, bounds, priceRange);
      graphics.strokeLine(bounds.left(), y, bounds.right(), y);
    }
  }

  private void drawYAxisTicks(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<Double> ticks,
    RenderStyle style
  ) {
    graphics.setFont(style.axisFont());
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.setStroke(style.axis());
    graphics.setLineWidth(style.gridLineWidth());
    graphics.setFill(style.mutedForeground());
    for (double price : ticks) {
      double y = yForPrice(price, bounds, priceRange);
      graphics.strokeLine(bounds.right(), y, bounds.right() + 5.0, y);
      graphics.fillText(String.format(Locale.US, "%.2f", price), bounds.right() + 10.0, y);
    }
  }

  private void drawCurrentPriceBadge(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    double currentPrice = visiblePoints.get(visiblePoints.size() - 1).price();
    double y = yForPrice(currentPrice, bounds, priceRange);
    if (y < bounds.top() || y > bounds.bottom()) {
      return;
    }
    double badgeWidth = canvas.getWidth() - bounds.right();
    double badgeTop = y - style.badgeHeight() / 2.0;
    graphics.setFill(style.primary());
    graphics.fillRect(bounds.right(), badgeTop, badgeWidth, style.badgeHeight());
    graphics.setFill(style.primaryForeground());
    graphics.setFont(style.badgeFont());
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(String.format(Locale.US, "%.2f", currentPrice), bounds.right() + CURRENT_PRICE_TEXT_OFFSET, y);
  }

  private void drawCrosshair(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    VisibleWindow visibleWindow,
    RenderStyle style
  ) {
    if (model.crosshairX == null || model.crosshairY == null) {
      return;
    }
    double x = model.crosshairX;
    double y = model.crosshairY;
    double price = priceForY(y, bounds, priceRange);
    int slotIndex = slotIndexForX(x, bounds);
    LocalDate date = dateForSlot(slotIndex, visibleWindow);
    crosshair.draw(
      graphics,
      bounds.left(),
      bounds.top(),
      bounds.right(),
      bounds.bottom(),
      canvas.getWidth(),
      x,
      y,
      price,
      date,
      style
    );
  }

  private void drawVerticalGridLines(
    GraphicsContext graphics,
    ChartBounds bounds,
    List<XAxisTick> ticks,
    RenderStyle style
  ) {
    graphics.setStroke(style.grid());
    graphics.setLineWidth(style.gridLineWidth());
    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), model.visiblePricePointCount, bounds);
      graphics.strokeLine(x, bounds.top(), x, bounds.bottom());
    }
  }

  private void drawXAxisTicks(GraphicsContext graphics, ChartBounds bounds, List<XAxisTick> ticks, RenderStyle style) {
    graphics.setFont(style.axisFont());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), model.visiblePricePointCount, bounds);
      graphics.setStroke(style.axis());
      graphics.setLineWidth(style.gridLineWidth());
      graphics.strokeLine(x, bounds.bottom(), x, bounds.bottom() + 5.0);
      graphics.setFill(style.mutedForeground());
      graphics.fillText(tick.label(), x, bounds.bottom() + 10.0);
    }
  }

  private List<XAxisTick> xAxisTicks(ChartBounds bounds, VisibleWindow visibleWindow, RenderStyle style) {
    return XAxisTickCalculator.calculate(
      model.pricePoints.stream().map(PricePoint::date).toList(),
      visibleWindow.firstDataIndex(),
      model.visiblePricePointCount,
      bounds.width(),
      style.axisLabelSpacing()
    );
  }

  private void drawPriceLine(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    graphics.setStroke(style.series());
    graphics.setLineWidth(style.seriesLineWidth());
    graphics.save();
    graphics.beginPath();
    graphics.rect(bounds.left(), bounds.top(), bounds.width(), bounds.height());
    graphics.clip();
    for (int index = 1; index < visiblePoints.size(); index++) {
      PricePoint previous = visiblePoints.get(index - 1);
      PricePoint current = visiblePoints.get(index);
      graphics.strokeLine(
        xForSlot(index - 1, model.visiblePricePointCount, bounds),
        yForPrice(previous.price(), bounds, priceRange),
        xForSlot(index, model.visiblePricePointCount, bounds),
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

  private double priceForY(double y, ChartBounds bounds, PriceRange priceRange) {
    double normalized = (bounds.bottom() - y) / bounds.height();
    return priceRange.min() + normalized * priceRange.span();
  }

  private LocalDate dateForSlot(int slotIndex, VisibleWindow visibleWindow) {
    int dataIndex = visibleWindow.firstDataIndex() + slotIndex;
    if (dataIndex < model.pricePoints.size()) {
      return model.pricePoints.get(dataIndex).date();
    }
    PricePoint newestPoint = model.pricePoints.get(model.pricePoints.size() - 1);
    return newestPoint.date().plusDays(dataIndex - model.pricePoints.size() + 1L);
  }

  private ButtonBounds autoscaleButtonBounds(ChartBounds bounds) {
    return new ButtonBounds(
      bounds.right() + AUTOSCALE_BUTTON_X_OFFSET,
      bounds.bottom() + AUTOSCALE_BUTTON_Y_OFFSET,
      AUTOSCALE_BUTTON_SIZE,
      AUTOSCALE_BUTTON_SIZE
    );
  }

  private record ButtonBounds(double x, double y, double width, double height) {}

  record ChartBounds(double left, double top, double width, double height) {
    double right() {
      return left + width;
    }

    double bottom() {
      return top + height;
    }
  }
}
