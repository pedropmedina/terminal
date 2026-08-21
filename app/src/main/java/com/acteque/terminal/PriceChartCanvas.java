package com.acteque.terminal;

import com.acteque.terminal.XAxisTickCalculator.XAxisTick;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
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
  private static final double RIGHT_MARGIN = 64.0;
  private static final double TOP_MARGIN = 52.0;
  private static final double BOTTOM_MARGIN = 32.0;

  // Latest-price badge in the price column.
  private static final double CURRENT_PRICE_BADGE_HEIGHT = 24.0;
  private static final double CURRENT_PRICE_TEXT_OFFSET = 10.0;

  // Crosshair and axis value badges.
  private static final double CROSSHAIR_BADGE_HEIGHT = 24.0;
  private static final double CROSSHAIR_DATE_BADGE_WIDTH = 120.0;
  private static final double CROSSHAIR_PRICE_TEXT_OFFSET = 10.0;

  // Autoscale button, shown at the foot of the price axis while that axis is hovered.
  private static final double AUTOSCALE_BUTTON_SIZE = 24.0;
  private static final double AUTOSCALE_BUTTON_X_OFFSET = 8.0;
  private static final double AUTOSCALE_BUTTON_Y_OFFSET = 0.0;

  // Pixels per x and y zoom adjustment
  private static final double X_ZOOM_PIXELS_PER_POINT = 8.0;
  private static final double Y_ZOOM_PIXELS_PER_STEP = 96.0;

  // Y zoom scale limit
  private static final double MAX_Y_ZOOM_SCALE = 5.0;

  // X zoom minimum price points to display
  private static final int MIN_VISIBLE_POINTS = 8;

  // X zoom state
  private double xZoomDragStart;
  private int xZoomDragStartVisiblePricePointCount;

  // X pan state
  private double xPanDragStart;
  private int xPanDragStartOffset;

  // Y pan state. A range only exists after the user has manually scaled the price axis.
  private double yPanDragStart;
  private PriceRange yPanDragStartPriceRange;

  // Y zoom state
  private double yZoomScale = 1.0;
  private double yZoomDragStart;
  private double yZoomDragStartZoomScale;
  private double yZoomDragDefaultPriceSpan;
  private PriceRange yZoomDragStartPriceRange;

  // Once set, this range is independent of the visible date window. This keeps the user's
  // selected price coordinates stable while horizontally panning or zooming.
  private PriceRange lockedPriceRange;

  // Mouse drag mode
  private DragMode dragMode = DragMode.NONE;

  // Price-axis hover state controls the visibility of the autoscale button.
  private boolean priceAxisHovered;
  private boolean autoscaleButtonPressed;

  // Visible point currently aligned with the cursor. Null displays the latest visible point.
  private Integer hoveredVisiblePointIndex;

  // Exact cursor position while it is inside the plot area.
  private Double crosshairX;
  private Double crosshairY;

  // Size of 'visible' price points list as determined by x zoom
  private int visiblePricePointCount;

  // Positive values show older data; negative values leave empty space after the newest point.
  private int visiblePricePointOffset;

  // Track initial price points
  private final List<PricePoint> pricePoints;

  private final ChartInterval interval;

  private final String stockSymbol;

  PriceChartCanvas(List<PricePoint> pricePoints, String stockSymbol) {
    this(pricePoints, stockSymbol, ChartInterval.DAILY);
  }

  PriceChartCanvas(List<PricePoint> pricePoints, String stockSymbol, ChartInterval interval) {
    this.stockSymbol = Objects.requireNonNull(stockSymbol);
    this.interval = Objects.requireNonNull(interval);
    this.visiblePricePointCount = pricePoints.size();
    this.pricePoints = List.copyOf(pricePoints);

    ChartReloadHooks.register(this); // <- Dev only hook to for canvas hot reloading
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
    PriceRange defaultPriceRange = calculateDefaultPriceRange(visibleWindow.points());
    PriceRange priceRange;
    if (lockedPriceRange == null) {
      yZoomScale = clampYZoomScale(yZoomScale, defaultPriceRange.span(), bounds.height());
      priceRange = scalePriceRange(defaultPriceRange, yZoomScale);
    } else {
      priceRange = lockedPriceRange;
    }
    List<Double> yAxisTicks = YAxisTickCalculator.calculate(
      priceRange.min(),
      priceRange.max(),
      bounds.height(),
      yZoomScale
    );

    drawHorizontalGridLines(graphics, bounds, priceRange, yAxisTicks);
    drawVerticalGridLines(graphics, bounds, xAxisTicks);
    drawAxes(graphics, bounds);
    drawYAxisTicks(graphics, bounds, priceRange, yAxisTicks);
    drawXAxisTicks(graphics, bounds, visibleWindow, xAxisTicks);
    drawPriceLine(graphics, bounds, priceRange, visibleWindow.points());
    drawCurrentPriceBadge(graphics, bounds, priceRange, visibleWindow.points());
    drawCrosshair(graphics, bounds, priceRange, visibleWindow);
    drawAutoscaleButton(graphics, bounds);
    drawStatusLine(graphics, bounds, visibleWindow.points(), hoveredVisiblePointIndex);
  }

  private void setEventsListeners() {
    setOnMouseMoved(event -> {
      updateStatusLinePoint(event.getX(), event.getY());
      updatePriceAxisHover(event.getX(), event.getY());
      updateCursor(event.getX(), event.getY());
    });

    setOnMouseExited(event -> {
      boolean redrawNeeded = hoveredVisiblePointIndex != null || crosshairX != null;
      hoveredVisiblePointIndex = null;
      crosshairX = null;
      crosshairY = null;
      if (priceAxisHovered) {
        priceAxisHovered = false;
        redrawNeeded = true;
      }
      if (redrawNeeded) {
        drawChart();
      }
      if (dragMode == DragMode.NONE) {
        setCursor(Cursor.DEFAULT);
      }
    });

    setOnMousePressed(event -> {
      ChartBounds bounds = chartBounds();
      autoscaleButtonPressed = isOverAutoscaleButton(event.getX(), event.getY(), bounds);

      if (autoscaleButtonPressed) {
        dragMode = DragMode.NONE;
        setCursor(Cursor.HAND);
      } else if (isOverPriceAxisArea(event.getX(), event.getY(), bounds)) {
        dragMode = DragMode.ZOOM_PRICE;
        yZoomDragStart = event.getY();
        yZoomDragStartZoomScale = yZoomScale;
        yZoomDragStartPriceRange = displayedPriceRange();
        yZoomDragDefaultPriceSpan = yZoomDragStartPriceRange.span() / yZoomScale;
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
        yPanDragStart = event.getY();
        yPanDragStartPriceRange = lockedPriceRange;
        setCursor(Cursor.CLOSED_HAND);
      }
    });

    setOnMouseDragged(event -> {
      if (dragMode == DragMode.ZOOM_DATE) {
        handleXAxisZoom(event.getX());
      } else if (dragMode == DragMode.ZOOM_PRICE) {
        handleYAxisZoom(event.getY());
      } else if (dragMode == DragMode.PAN) {
        handlePan(event.getX(), event.getY());
        updateStatusLinePoint(event.getX(), event.getY());
      }
    });

    setOnMouseReleased(event -> {
      boolean toggleAutoscale =
        autoscaleButtonPressed && isOverAutoscaleButton(event.getX(), event.getY(), chartBounds());
      autoscaleButtonPressed = false;
      dragMode = DragMode.NONE;

      if (toggleAutoscale) {
        togglePriceRangeAutoscale();
      }

      updatePriceAxisHover(event.getX(), event.getY());
      updateCursor(event.getX(), event.getY());
    });

    // Keep this no-op handler for hot-reload compatibility. A live canvas may still reference the
    // generated lambda method from an earlier class definition. Press/release performs activation,
    // so handling the subsequent clicked event would toggle the state a second time.
    setOnMouseClicked(event -> keepHotReloadClickHandler());
  }

  private void keepHotReloadClickHandler() {}

  private void togglePriceRangeAutoscale() {
    if (lockedPriceRange == null) {
      lockedPriceRange = displayedPriceRange();
    } else {
      yZoomScale = 1.0;
      lockedPriceRange = null;
    }
    drawChart();
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

  private void handlePan(double x, double y) {
    ChartBounds bounds = chartBounds();
    double pointSpacing = bounds.width() / Math.max(1, visiblePricePointCount - 1);
    int pointDelta = (int) Math.round((x - xPanDragStart) / pointSpacing);
    int clampedOffset = clampVisiblePointOffset(xPanDragStartOffset + pointDelta);
    boolean changed = false;

    if (clampedOffset != visiblePricePointOffset) {
      visiblePricePointOffset = clampedOffset;
      changed = true;
    }

    if (yPanDragStartPriceRange != null) {
      double priceDelta = ((y - yPanDragStart) * yPanDragStartPriceRange.span()) / bounds.height();
      PriceRange pannedPriceRange = yPanDragStartPriceRange.translate(priceDelta);
      if (!pannedPriceRange.equals(lockedPriceRange)) {
        lockedPriceRange = pannedPriceRange;
        changed = true;
      }
    }

    if (changed) {
      drawChart();
    }
  }

  private void handleYAxisZoom(double y) {
    double deltaY = y - yZoomDragStart;
    double requestedYZoomScale = yZoomDragStartZoomScale * Math.exp(deltaY / Y_ZOOM_PIXELS_PER_STEP);
    double clampedYZoomScale = clampYZoomScale(requestedYZoomScale, yZoomDragDefaultPriceSpan, chartBounds().height());

    if (Double.compare(clampedYZoomScale, yZoomScale) != 0) {
      double scaleChange = clampedYZoomScale / yZoomDragStartZoomScale;
      yZoomScale = clampedYZoomScale;
      lockedPriceRange = yZoomDragStartPriceRange.scale(scaleChange);
      drawChart();
    }
  }

  private PriceRange displayedPriceRange() {
    if (lockedPriceRange != null) {
      return lockedPriceRange;
    }

    PriceRange defaultPriceRange = calculateDefaultPriceRange(visibleWindow().points());
    double clampedScale = clampYZoomScale(yZoomScale, defaultPriceRange.span(), chartBounds().height());
    return scalePriceRange(defaultPriceRange, clampedScale);
  }

  private PriceRange calculateDefaultPriceRange(List<PricePoint> points) {
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
    return new PriceRange(defaultMin, defaultMax);
  }

  private PriceRange scalePriceRange(PriceRange defaultPriceRange, double zoomScale) {
    double midpoint = (defaultPriceRange.min() + defaultPriceRange.max()) / 2.0;
    double zoomedSpan = defaultPriceRange.span() * zoomScale;
    return new PriceRange(midpoint - zoomedSpan / 2.0, midpoint + zoomedSpan / 2.0);
  }

  private void drawStatusLine(
    GraphicsContext graphics,
    ChartBounds bounds,
    List<PricePoint> visiblePoints,
    Integer hoveredPointIndex
  ) {
    int pointIndex =
      hoveredPointIndex == null ? visiblePoints.size() - 1 : Math.min(hoveredPointIndex, visiblePoints.size() - 1);
    PricePoint statusLinePoint = visiblePoints.get(pointIndex);
    String statusLine = String.format(
      Locale.US,
      "%s  %s   O%,.2f  H%,.2f  L%,.2f  C%,.2f  Vol%,.2f M",
      stockSymbol,
      interval.displayName(),
      statusLinePoint.open(),
      statusLinePoint.high(),
      statusLinePoint.low(),
      statusLinePoint.close(),
      statusLinePoint.volume() / 1_000_000.0
    );

    graphics.setFill(Color.rgb(28, 32, 38));
    graphics.setFont(Font.font("System", 14));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(statusLine, bounds.left(), bounds.bottom() - 14.0);
  }

  private void drawAxes(GraphicsContext graphics, ChartBounds bounds) {
    graphics.setStroke(Color.rgb(40, 44, 52));
    graphics.setLineWidth(1.4);
    graphics.strokeLine(bounds.right(), bounds.top(), bounds.right(), bounds.bottom());
    graphics.strokeLine(bounds.left(), bounds.bottom(), bounds.right(), bounds.bottom());
  }

  private void drawAutoscaleButton(GraphicsContext graphics, ChartBounds bounds) {
    if (!priceAxisHovered) {
      return;
    }

    ButtonBounds button = autoscaleButtonBounds(bounds);
    boolean autoscaleActive = lockedPriceRange == null;

    graphics.setFill(autoscaleActive ? Color.BLACK : Color.WHITE);
    graphics.fillRoundRect(button.x(), button.y(), button.width(), button.height(), 4.0, 4.0);
    graphics.setStroke(Color.BLACK);
    graphics.setLineWidth(1.0);
    graphics.strokeRoundRect(button.x(), button.y(), button.width(), button.height(), 4.0, 4.0);

    graphics.setFill(autoscaleActive ? Color.WHITE : Color.BLACK);
    graphics.setFont(Font.font("System", 13));
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText("A", button.x() + button.width() / 2.0, button.y() + button.height() / 2.0);
  }

  private void drawHorizontalGridLines(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    List<Double> ticks
  ) {
    graphics.setStroke(Color.rgb(223, 228, 236));
    graphics.setLineWidth(1.0);

    for (double price : ticks) {
      double y = yForPrice(price, bounds, priceRange);
      graphics.strokeLine(bounds.left(), y, bounds.right(), y);
    }
  }

  private void drawYAxisTicks(GraphicsContext graphics, ChartBounds bounds, PriceRange priceRange, List<Double> ticks) {
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.setStroke(Color.rgb(40, 44, 52));
    graphics.setLineWidth(1.0);
    graphics.setFill(Color.rgb(74, 82, 94));

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
    List<PricePoint> visiblePoints
  ) {
    double currentPrice = visiblePoints.get(visiblePoints.size() - 1).price();
    double y = yForPrice(currentPrice, bounds, priceRange);
    if (y < bounds.top() || y > bounds.bottom()) {
      return;
    }

    double badgeWidth = getWidth() - bounds.right();
    double badgeTop = y - CURRENT_PRICE_BADGE_HEIGHT / 2.0;

    graphics.setFill(Color.BLACK);
    graphics.fillRect(bounds.right(), badgeTop, badgeWidth, CURRENT_PRICE_BADGE_HEIGHT);

    graphics.setFill(Color.WHITE);
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(String.format(Locale.US, "%.2f", currentPrice), bounds.right() + CURRENT_PRICE_TEXT_OFFSET, y);
  }

  private void drawCrosshair(
    GraphicsContext graphics,
    ChartBounds bounds,
    PriceRange priceRange,
    VisibleWindow visibleWindow
  ) {
    if (crosshairX == null || crosshairY == null) {
      return;
    }

    double x = crosshairX;
    double y = crosshairY;
    double price = priceForY(y, bounds, priceRange);
    int slotIndex = slotIndexForX(x, bounds);
    LocalDate date = dateForSlot(slotIndex, visibleWindow);

    graphics.save();
    graphics.setStroke(Color.rgb(120, 126, 136));
    graphics.setLineWidth(1.0);
    graphics.setLineDashes(4.0, 4.0);
    graphics.strokeLine(x, bounds.top(), x, bounds.bottom());
    graphics.strokeLine(bounds.left(), y, bounds.right(), y);
    graphics.restore();

    Color badgeColor = Color.rgb(232, 234, 237);
    Color badgeTextColor = Color.rgb(40, 44, 52);

    double priceBadgeTop = y - CROSSHAIR_BADGE_HEIGHT / 2.0;
    graphics.setFill(badgeColor);
    graphics.fillRect(bounds.right(), priceBadgeTop, getWidth() - bounds.right(), CROSSHAIR_BADGE_HEIGHT);
    graphics.setFill(badgeTextColor);
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(String.format(Locale.US, "%.2f", price), bounds.right() + CROSSHAIR_PRICE_TEXT_OFFSET, y);

    double dateBadgeLeft = Math.max(
      bounds.left(),
      Math.min(bounds.right() - CROSSHAIR_DATE_BADGE_WIDTH, x - CROSSHAIR_DATE_BADGE_WIDTH / 2.0)
    );
    graphics.setFill(badgeColor);
    graphics.fillRect(dateBadgeLeft, bounds.bottom(), CROSSHAIR_DATE_BADGE_WIDTH, CROSSHAIR_BADGE_HEIGHT);
    graphics.setFill(badgeTextColor);
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    graphics.fillText(
      interval.formatCrosshair(date),
      dateBadgeLeft + CROSSHAIR_DATE_BADGE_WIDTH / 2.0,
      bounds.bottom() + 10.0
    );
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

  private double priceForY(double y, ChartBounds bounds, PriceRange priceRange) {
    double normalized = (bounds.bottom() - y) / bounds.height();
    return priceRange.min() + normalized * priceRange.span();
  }

  private int slotIndexForX(double x, ChartBounds bounds) {
    return (int) Math.round(((x - bounds.left()) / bounds.width()) * Math.max(0, visiblePricePointCount - 1));
  }

  private LocalDate dateForSlot(int slotIndex, VisibleWindow visibleWindow) {
    int dataIndex = visibleWindow.firstDataIndex() + slotIndex;
    if (dataIndex < pricePoints.size()) {
      return pricePoints.get(dataIndex).date();
    }

    PricePoint newestPoint = pricePoints.get(pricePoints.size() - 1);
    return newestPoint.date().plusDays(dataIndex - pricePoints.size() + 1L);
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

  private boolean isOverAutoscaleButton(double x, double y, ChartBounds bounds) {
    ButtonBounds button = autoscaleButtonBounds(bounds);
    return x >= button.x() && x <= button.x() + button.width() && y >= button.y() && y <= button.y() + button.height();
  }

  private ButtonBounds autoscaleButtonBounds(ChartBounds bounds) {
    return new ButtonBounds(
      bounds.right() + AUTOSCALE_BUTTON_X_OFFSET,
      bounds.bottom() + AUTOSCALE_BUTTON_Y_OFFSET,
      AUTOSCALE_BUTTON_SIZE,
      AUTOSCALE_BUTTON_SIZE
    );
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

  private double clampYZoomScale(double requestedYZoomScale, double defaultPriceSpan, double chartHeight) {
    double minimumPriceSpan = YAxisTickCalculator.minimumPriceSpan(chartHeight);
    double minimumYZoomScale = minimumPriceSpan / defaultPriceSpan;
    return Math.max(minimumYZoomScale, Math.min(MAX_Y_ZOOM_SCALE, requestedYZoomScale));
  }

  private void updateCursor(double x, double y) {
    ChartBounds bounds = chartBounds();
    if (isOverAutoscaleButton(x, y, bounds)) {
      setCursor(Cursor.HAND);
    } else if (isOverPriceAxisArea(x, y, bounds)) {
      setCursor(Cursor.V_RESIZE);
    } else if (isOverDateAxisArea(y, bounds)) {
      setCursor(Cursor.H_RESIZE);
    } else if (isOverChartArea(x, y, bounds)) {
      setCursor(Cursor.CROSSHAIR);
    } else {
      setCursor(Cursor.DEFAULT);
    }
  }

  private void updateStatusLinePoint(double x, double y) {
    ChartBounds bounds = chartBounds();
    Integer pointIndex = null;
    Double nextCrosshairX = null;
    Double nextCrosshairY = null;

    if (isOverChartArea(x, y, bounds)) {
      nextCrosshairX = x;
      nextCrosshairY = y;
      int slotIndex = slotIndexForX(x, bounds);
      if (slotIndex < visibleWindow().points().size()) {
        pointIndex = slotIndex;
      }
    }

    if (
      !Objects.equals(pointIndex, hoveredVisiblePointIndex) ||
      !Objects.equals(nextCrosshairX, crosshairX) ||
      !Objects.equals(nextCrosshairY, crosshairY)
    ) {
      hoveredVisiblePointIndex = pointIndex;
      crosshairX = nextCrosshairX;
      crosshairY = nextCrosshairY;
      drawChart();
    }
  }

  private void updatePriceAxisHover(double x, double y) {
    ChartBounds bounds = chartBounds();
    boolean hovered = isOverPriceAxisArea(x, y, bounds) || isOverAutoscaleButton(x, y, bounds);
    if (hovered != priceAxisHovered) {
      priceAxisHovered = hovered;
      drawChart();
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

    PriceRange scale(double scale) {
      double midpoint = (min + max) / 2.0;
      double scaledSpan = span() * scale;
      return new PriceRange(midpoint - scaledSpan / 2.0, midpoint + scaledSpan / 2.0);
    }

    PriceRange translate(double priceDelta) {
      return new PriceRange(min + priceDelta, max + priceDelta);
    }
  }

  private record VisibleWindow(List<PricePoint> points, int firstDataIndex) {}

  private record ButtonBounds(double x, double y, double width, double height) {}

  private record ChartBounds(double left, double top, double width, double height) {
    double right() {
      return left + width;
    }

    double bottom() {
      return top + height;
    }
  }
}
