package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.ChartIntervalHistoryMapper;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.PriceRange;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.VisibleWindow;
import com.acteque.terminal.chart.canvas.XAxisTickCalculator.XAxisTick;
import com.acteque.terminal.marketdata.HistoricalInterval;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.text.TextAlignment;

/** Renders chart-canvas state and provides its drawing-coordinate geometry. */
final class CanvasRenderer {

  private final Canvas canvas;
  private final ChartCanvasModel model;
  private final CrosshairRenderer crosshair;

  /**
   * @param canvas the canvas to render and measure
   * @param model the canvas state to render
   */
  CanvasRenderer(Canvas canvas, ChartCanvasModel model) {
    this.canvas = Objects.requireNonNull(canvas, "canvas cannot be null");
    this.model = Objects.requireNonNull(model, "model cannot be null");
    crosshair = new CrosshairRenderer();
  }

  /**
   * Draws the complete chart in layer order.
   *
   * @param style CSS-resolved presentation values
   * @param visibleWindow the visible data slice
   * @param priceRange the displayed vertical range, or {@code null} for empty data
   */
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

    ChartBounds bounds = chartBounds(style);
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
    drawSeries(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCurrentPriceBadge(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCrosshair(graphics, bounds, priceRange, visibleWindow, style);
    drawAutoscaleButton(graphics, bounds, style);
  }

  /**
   * @param style CSS-resolved chart margins
   * @return drawable bounds within the canvas
   */
  ChartBounds chartBounds(RenderStyle style) {
    return new ChartBounds(
      style.leftMargin(),
      style.topMargin(),
      Math.max(1.0, canvas.getWidth() - style.leftMargin() - style.rightMargin()),
      Math.max(1.0, canvas.getHeight() - style.topMargin() - style.bottomMargin())
    );
  }

  /** @return true when {@code y} lies within the date-axis area */
  boolean isOverDateAxisArea(double y, ChartBounds bounds) {
    return y >= bounds.bottom() && y <= canvas.getHeight();
  }

  /** @return true when the coordinates lie within the plot area */
  boolean isOverChartArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.left() && x <= bounds.right() && y >= bounds.top() && y <= bounds.bottom();
  }

  /** @return true when the coordinates lie within the price-axis area */
  boolean isOverPriceAxisArea(double x, double y, ChartBounds bounds) {
    return x >= bounds.right() && x <= canvas.getWidth() && y >= bounds.top() && y <= bounds.bottom();
  }

  /** @return true when the coordinates lie within the CSS-sized autoscale button */
  boolean isOverAutoscaleButton(double x, double y, ChartBounds bounds, RenderStyle style) {
    ButtonBounds button = autoscaleButtonBounds(bounds, style);
    return x >= button.x() && x <= button.x() + button.width() && y >= button.y() && y <= button.y() + button.height();
  }

  /** @return the nearest visible slot for the supplied x coordinate */
  int slotIndexForX(double x, ChartBounds bounds) {
    return (int) Math.round(((x - bounds.left()) / bounds.width()) * Math.max(0, model.visiblePricePointCount - 1));
  }

  /** Draws plot boundary axes using the supplied graphics, bounds, and style. */
  private void drawAxes(GraphicsContext graphics, ChartBounds bounds, RenderStyle style) {
    graphics.setStroke(style.axis());
    graphics.setLineWidth(style.axisLineWidth());
    graphics.strokeLine(bounds.right(), bounds.top(), bounds.right(), bounds.bottom());
    graphics.strokeLine(bounds.left(), bounds.bottom(), bounds.right(), bounds.bottom());
  }

  /** Draws the autoscale control when the price axis is hovered. */
  private void drawAutoscaleButton(GraphicsContext graphics, ChartBounds bounds, RenderStyle style) {
    if (!model.priceAxisHovered) {
      return;
    }

    ButtonBounds button = autoscaleButtonBounds(bounds, style);
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

  /** Draws horizontal grid lines at the supplied price ticks. */
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

  /** Draws price-axis tick marks and labels. */
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
      graphics.strokeLine(bounds.right(), y, bounds.right() + style.axisTickLength(), y);
      graphics.fillText(String.format(Locale.US, "%.2f", price), bounds.right() + style.axisTextOffset(), y);
    }
  }

  /** Draws the newest visible closing-price badge when it lies within the viewport. */
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
    graphics.fillText(
      String.format(Locale.US, "%.2f", currentPrice),
      bounds.right() + style.currentPriceTextOffset(),
      y
    );
  }

  /** Draws the active crosshair and its axis badges. */
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
    String dateText = !hasIntradayBars()
      ? crosshair.dateText(dateForSlot(slotIndex, visibleWindow))
      : timeForSlot(slotIndex, visibleWindow);
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
      dateText,
      style
    );
  }

  /** Draws vertical grid lines at the supplied date ticks. */
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

  /** Draws date-axis tick marks and labels. */
  private void drawXAxisTicks(GraphicsContext graphics, ChartBounds bounds, List<XAxisTick> ticks, RenderStyle style) {
    graphics.setFont(style.axisFont());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), model.visiblePricePointCount, bounds);
      graphics.setStroke(style.axis());
      graphics.setLineWidth(style.gridLineWidth());
      graphics.strokeLine(x, bounds.bottom(), x, bounds.bottom() + style.axisTickLength());
      graphics.setFill(style.mutedForeground());
      graphics.fillText(tick.label(), x, bounds.bottom() + style.axisTextOffset());
    }
  }

  /**
   * @return calendar or intraday ticks for the visible window
   */
  private List<XAxisTick> xAxisTicks(ChartBounds bounds, VisibleWindow visibleWindow, RenderStyle style) {
    if (hasIntradayBars()) {
      return IntradayXAxisTickCalculator.calculate(
        model.pricePoints
          .stream()
          .map(point -> point.timestamp().orElseThrow())
          .toList(),
        visibleWindow.firstDataIndex(),
        model.visiblePricePointCount,
        bounds.width(),
        style.axisLabelSpacing()
      );
    }
    return XAxisTickCalculator.calculate(
      model.pricePoints.stream().map(PricePoint::date).toList(),
      visibleWindow.firstDataIndex(),
      model.visiblePricePointCount,
      bounds.width(),
      style.axisLabelSpacing(),
      calendarPeriod()
    );
  }

  /** @return the date projection period for the selected calendar interval */
  private Period calendarPeriod() {
    return switch (model.interval.classification()) {
      case WEEKS -> Period.ofWeeks(model.interval.amount());
      case MONTHS -> Period.ofMonths(model.interval.amount());
      default -> Period.ofDays(model.interval.amount());
    };
  }

  /** @return the selected intraday duration, or {@code null} for calendar intervals */
  private Duration intradayInterval() {
    return ChartIntervalHistoryMapper.map(model.interval)
      .filter(interval -> interval instanceof HistoricalInterval.Intraday)
      .map(interval -> ((HistoricalInterval.Intraday) interval).value())
      .orElse(null);
  }

  /** @return true when every loaded point belongs to an intraday interval */
  private boolean hasIntradayBars() {
    return (
      intradayInterval() != null &&
      !model.pricePoints.isEmpty() &&
      model.pricePoints.stream().allMatch(point -> point.timestamp().isPresent())
    );
  }

  /** @return the timestamp label for a visible or unavailable projected slot */
  private String timeForSlot(int slotIndex, VisibleWindow visibleWindow) {
    int dataIndex = visibleWindow.firstDataIndex() + slotIndex;
    return dataIndex < model.pricePoints.size()
      ? crosshair.dateText(model.pricePoints.get(dataIndex).timestamp().orElseThrow())
      : "No bar";
  }

  /** Draws the selected series type clipped to chart bounds. */
  private void drawSeries(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    graphics.save();
    graphics.beginPath();
    graphics.rect(bounds.left(), bounds.top(), bounds.width(), bounds.height());
    graphics.clip();
    switch (model.chartType) {
      case BAR -> drawBars(graphics, bounds, priceRange, visiblePoints, style);
      case CANDLESTICK -> drawCandlesticks(graphics, bounds, priceRange, visiblePoints, style);
      case LINE -> drawPriceLine(graphics, bounds, priceRange, visiblePoints, style);
      case LINE_WITH_MARKERS -> {
        drawPriceLine(graphics, bounds, priceRange, visiblePoints, style);
        drawPriceMarkers(graphics, bounds, priceRange, visiblePoints, style);
      }
      case STEP_LINE -> drawStepLine(graphics, bounds, priceRange, visiblePoints, style);
      case AREA -> {
        drawPriceArea(graphics, bounds, priceRange, visiblePoints, style);
        drawPriceLine(graphics, bounds, priceRange, visiblePoints, style);
      }
    }
    graphics.restore();
  }

  /** Draws OHLC bars for visible points. */
  private void drawBars(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    double slotWidth = bounds.width() / Math.max(1, model.visiblePricePointCount - 1);
    double tickWidth = Math.max(1.0, Math.min(style.barTickMaxWidth(), slotWidth * 0.35));
    graphics.setStroke(style.bar());
    graphics.setLineWidth(style.barStrokeWidth());
    for (int index = 0; index < visiblePoints.size(); index++) {
      PricePoint point = visiblePoints.get(index);
      double x = xForSlot(index, model.visiblePricePointCount, bounds);
      double openY = yForPrice(point.open(), bounds, priceRange);
      double closeY = yForPrice(point.close(), bounds, priceRange);
      graphics.strokeLine(
        x,
        yForPrice(point.high(), bounds, priceRange),
        x,
        yForPrice(point.low(), bounds, priceRange)
      );
      graphics.strokeLine(x - tickWidth, openY, x, openY);
      graphics.strokeLine(x, closeY, x + tickWidth, closeY);
    }
  }

  /** Draws the translucent area beneath visible closing prices. */
  private void drawPriceArea(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    if (visiblePoints.size() < 2) {
      return;
    }

    graphics.save();
    graphics.setGlobalAlpha(style.areaOpacity());
    graphics.setFill(style.line());
    graphics.beginPath();
    graphics.moveTo(xForSlot(0, model.visiblePricePointCount, bounds), bounds.bottom());
    for (int index = 0; index < visiblePoints.size(); index++) {
      graphics.lineTo(
        xForSlot(index, model.visiblePricePointCount, bounds),
        yForPrice(visiblePoints.get(index).price(), bounds, priceRange)
      );
    }
    graphics.lineTo(xForSlot(visiblePoints.size() - 1, model.visiblePricePointCount, bounds), bounds.bottom());
    graphics.closePath();
    graphics.fill();
    graphics.restore();
  }

  /** Draws a continuous line through visible closing prices. */
  private void drawPriceLine(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    graphics.setStroke(style.line());
    graphics.setLineWidth(style.lineWidth());
    if (visiblePoints.size() == 1) {
      drawSinglePoint(graphics, bounds, priceRange, visiblePoints.getFirst(), style);
      return;
    }
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
  }

  /** Draws point markers on visible closing prices. */
  private void drawPriceMarkers(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    double diameter = style.markerDiameter();
    graphics.setFill(style.line());
    for (int index = 0; index < visiblePoints.size(); index++) {
      double x = xForSlot(index, model.visiblePricePointCount, bounds);
      double y = yForPrice(visiblePoints.get(index).price(), bounds, priceRange);
      graphics.fillOval(x - diameter / 2.0, y - diameter / 2.0, diameter, diameter);
    }
  }

  /** Draws a step line through visible closing prices. */
  private void drawStepLine(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    graphics.setStroke(style.line());
    graphics.setLineWidth(style.lineWidth());
    if (visiblePoints.size() == 1) {
      drawSinglePoint(graphics, bounds, priceRange, visiblePoints.getFirst(), style);
      return;
    }
    for (int index = 1; index < visiblePoints.size(); index++) {
      double previousX = xForSlot(index - 1, model.visiblePricePointCount, bounds);
      double currentX = xForSlot(index, model.visiblePricePointCount, bounds);
      double previousY = yForPrice(visiblePoints.get(index - 1).close(), bounds, priceRange);
      double currentY = yForPrice(visiblePoints.get(index).close(), bounds, priceRange);
      graphics.strokeLine(previousX, previousY, currentX, previousY);
      graphics.strokeLine(currentX, previousY, currentX, currentY);
    }
  }

  /** Draws a visible glyph when a line series contains only one point. */
  private void drawSinglePoint(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    PricePoint point,
    RenderStyle style
  ) {
    double size = style.lineWidth() * 2.0;
    graphics.setFill(style.line());
    graphics.fillOval(
      xForSlot(0, model.visiblePricePointCount, bounds) - size / 2.0,
      yForPrice(point.close(), bounds, priceRange) - size / 2.0,
      size,
      size
    );
  }

  /** Draws candle bodies and wicks for visible points. */
  private void drawCandlesticks(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<PricePoint> visiblePoints,
    RenderStyle style
  ) {
    double slotWidth = bounds.width() / Math.max(1, model.visiblePricePointCount - 1);
    double bodyWidth = Math.max(1.0, Math.min(style.candleBodyMaxWidth(), slotWidth * 0.7));
    graphics.setLineWidth(style.candleStrokeWidth());
    for (int index = 0; index < visiblePoints.size(); index++) {
      PricePoint point = visiblePoints.get(index);
      double x = xForSlot(index, model.visiblePricePointCount, bounds);
      double openY = yForPrice(point.open(), bounds, priceRange);
      double closeY = yForPrice(point.close(), bounds, priceRange);
      double bodyHeight = Math.max(1.0, Math.abs(openY - closeY));
      double bodyTop = (openY + closeY - bodyHeight) / 2.0;
      Paint candlePaint = point.close() >= point.open() ? style.candleUp() : style.candleDown();
      graphics.setStroke(style.candleBorder());
      graphics.setFill(candlePaint);
      graphics.strokeLine(
        x,
        yForPrice(point.high(), bounds, priceRange),
        x,
        yForPrice(point.low(), bounds, priceRange)
      );
      graphics.fillRect(x - bodyWidth / 2.0, bodyTop, bodyWidth, bodyHeight);
      graphics.strokeRect(x - bodyWidth / 2.0, bodyTop, bodyWidth, bodyHeight);
    }
  }

  /** @return the x coordinate for a visible slot */
  private double xForSlot(int index, int pointCount, ChartBounds bounds) {
    if (pointCount == 1) {
      return bounds.left() + bounds.width() / 2.0;
    }
    return bounds.left() + ((double) index / (pointCount - 1)) * bounds.width();
  }

  /** @return the y coordinate representing a price */
  private double yForPrice(double price, ChartBounds bounds, PriceRange priceRange) {
    double normalized = (price - priceRange.min()) / priceRange.span();
    return bounds.bottom() - normalized * bounds.height();
  }

  /** @return the price represented by a y coordinate */
  private double priceForY(double y, ChartBounds bounds, PriceRange priceRange) {
    double normalized = (bounds.bottom() - y) / bounds.height();
    return priceRange.min() + normalized * priceRange.span();
  }

  /** @return the loaded or projected date for a visible slot */
  private LocalDate dateForSlot(int slotIndex, VisibleWindow visibleWindow) {
    int dataIndex = visibleWindow.firstDataIndex() + slotIndex;
    if (dataIndex < model.pricePoints.size()) {
      return model.pricePoints.get(dataIndex).date();
    }
    PricePoint newestPoint = model.pricePoints.get(model.pricePoints.size() - 1);
    return newestPoint.date().plus(calendarPeriod().multipliedBy(dataIndex - model.pricePoints.size() + 1));
  }

  /** @return CSS-sized autoscale control bounds */
  private ButtonBounds autoscaleButtonBounds(ChartBounds bounds, RenderStyle style) {
    return new ButtonBounds(
      bounds.right() + style.autoscaleButtonXOffset(),
      bounds.bottom() + style.autoscaleButtonYOffset(),
      style.autoscaleButtonSize(),
      style.autoscaleButtonSize()
    );
  }

  /** Rectangle occupied by the autoscale control. */
  private record ButtonBounds(double x, double y, double width, double height) {}

  /** Rectangle occupied by the drawable chart area. */
  record ChartBounds(double left, double top, double width, double height) {
    /** @return the right edge */
    double right() {
      return left + width;
    }

    /** @return the bottom edge */
    double bottom() {
      return top + height;
    }
  }
}
