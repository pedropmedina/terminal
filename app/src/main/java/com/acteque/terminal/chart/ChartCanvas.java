package com.acteque.terminal.chart;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Function;
import com.acteque.terminal.chart.XAxisTickCalculator.XAxisTick;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.VPos;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

final class ChartCanvas extends Canvas implements RefreshableView {

  private static final StyleablePropertyFactory<ChartCanvas> STYLEABLES = new StyleablePropertyFactory<>(
    Canvas.getClassCssMetaData()
  );

  // Padding
  private static final double LEFT_MARGIN = 0.0;
  private static final double RIGHT_MARGIN = 64.0;
  private static final double TOP_MARGIN = 0.0;
  private static final double BOTTOM_MARGIN = 32.0;

  // Latest-price badge in the price column.
  private static final double CURRENT_PRICE_TEXT_OFFSET = 10.0;

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

  // Price points are ordered oldest to newest and replaced when the controller publishes history.
  private List<PricePoint> pricePoints;

  private Runnable onEarlierHistoryRequested = () -> {};
  private final ChartInterval interval;

  private final ChartCrosshair crosshair;

  private final ChartStatusLine statusLine;

  private final StyleableProperty<Paint> chartBackground = paintProperty(
    "chartBackground",
    "-chart-background",
    Color.WHITE,
    canvas -> canvas.chartBackground
  );
  private final StyleableProperty<Paint> chartAxis = paintProperty(
    "chartAxis",
    "-chart-axis",
    Color.rgb(40, 44, 52),
    canvas -> canvas.chartAxis
  );
  private final StyleableProperty<Paint> chartGrid = paintProperty(
    "chartGrid",
    "-chart-grid",
    Color.rgb(223, 228, 236),
    canvas -> canvas.chartGrid
  );
  private final StyleableProperty<Paint> chartMutedForeground = paintProperty(
    "chartMutedForeground",
    "-chart-muted-foreground",
    Color.rgb(74, 82, 94),
    canvas -> canvas.chartMutedForeground
  );
  private final StyleableProperty<Paint> chartSeries = paintProperty(
    "chartSeries",
    "-chart-series",
    Color.rgb(26, 115, 232),
    canvas -> canvas.chartSeries
  );
  private final StyleableProperty<Paint> chartCrosshair = paintProperty(
    "chartCrosshair",
    "-chart-crosshair",
    Color.rgb(120, 126, 136),
    canvas -> canvas.chartCrosshair
  );
  private final StyleableProperty<Paint> chartBadgeBackground = paintProperty(
    "chartBadgeBackground",
    "-chart-badge-background",
    Color.rgb(232, 234, 237),
    canvas -> canvas.chartBadgeBackground
  );
  private final StyleableProperty<Paint> chartBadgeForeground = paintProperty(
    "chartBadgeForeground",
    "-chart-badge-foreground",
    Color.rgb(40, 44, 52),
    canvas -> canvas.chartBadgeForeground
  );
  private final StyleableProperty<Paint> chartPrimary = paintProperty(
    "chartPrimary",
    "-chart-primary",
    Color.BLACK,
    canvas -> canvas.chartPrimary
  );
  private final StyleableProperty<Paint> chartPrimaryForeground = paintProperty(
    "chartPrimaryForeground",
    "-chart-primary-foreground",
    Color.WHITE,
    canvas -> canvas.chartPrimaryForeground
  );
  private final StyleableProperty<Font> chartAxisFont = fontProperty(
    "chartAxisFont",
    "-chart-axis-font",
    Font.font("System", 12),
    canvas -> canvas.chartAxisFont
  );
  private final StyleableProperty<Font> chartBadgeFont = fontProperty(
    "chartBadgeFont",
    "-chart-badge-font",
    Font.font("System", 12),
    canvas -> canvas.chartBadgeFont
  );
  private final StyleableProperty<Number> chartAxisLineWidth = numberProperty(
    "chartAxisLineWidth",
    "-chart-axis-line-width",
    1.4,
    canvas -> canvas.chartAxisLineWidth
  );
  private final StyleableProperty<Number> chartGridLineWidth = numberProperty(
    "chartGridLineWidth",
    "-chart-grid-line-width",
    1.0,
    canvas -> canvas.chartGridLineWidth
  );
  private final StyleableProperty<Number> chartSeriesLineWidth = numberProperty(
    "chartSeriesLineWidth",
    "-chart-series-line-width",
    2.2,
    canvas -> canvas.chartSeriesLineWidth
  );
  private final StyleableProperty<Number> chartBadgeHeight = numberProperty(
    "chartBadgeHeight",
    "-chart-badge-height",
    24.0,
    canvas -> canvas.chartBadgeHeight
  );
  private final StyleableProperty<Number> chartControlRadius = numberProperty(
    "chartControlRadius",
    "-chart-control-radius",
    4.0,
    canvas -> canvas.chartControlRadius
  );

  private boolean redrawScheduled;

  ChartCanvas(List<PricePoint> pricePoints, String stockSymbol) {
    this(pricePoints, stockSymbol, ChartInterval.DAILY);
  }

  ChartCanvas(List<PricePoint> pricePoints, String stockSymbol, ChartInterval interval) {
    this(pricePoints, interval, new ChartStatusLine(stockSymbol, interval));
  }

  ChartCanvas(List<PricePoint> pricePoints, ChartInterval interval, ChartStatusLine statusLine) {
    this.interval = Objects.requireNonNull(interval);
    this.crosshair = new ChartCrosshair(interval);
    this.statusLine = Objects.requireNonNull(statusLine, "statusLine");
    this.visiblePricePointCount = pricePoints.size();
    this.pricePoints = List.copyOf(pricePoints);

    getStyleClass().add("chart-canvas");

    ChartReloadHooks.register(this); // Development runs refresh this view after class redefinition.
    widthProperty().addListener((ignored, oldWidth, newWidth) -> drawChart());
    heightProperty().addListener((ignored, oldHeight, newHeight) -> drawChart());
    setEventsListeners();
  }

  void setOnEarlierHistoryRequested(Runnable callback) {
    onEarlierHistoryRequested = Objects.requireNonNull(callback, "callback");
  }

  void setPricePoints(List<PricePoint> updatedPoints) {
    List<PricePoint> replacement = List.copyOf(updatedPoints);
    if (replacement.equals(pricePoints)) {
      return;
    }

    boolean establishingInitialData = pricePoints.isEmpty();
    pricePoints = replacement;
    if (pricePoints.isEmpty()) {
      statusLine.clearPricePoint();
    }
    if (establishingInitialData) {
      visiblePricePointCount = pricePoints.size();
    } else {
      visiblePricePointCount = clampVisiblePointCount(visiblePricePointCount);
    }
    visiblePricePointOffset = clampVisiblePointOffset(visiblePricePointOffset);
    drawChart();
  }

  void drawChart() {
    double width = getWidth();
    double height = getHeight();
    if (width <= 0 || height <= 0) {
      return;
    }

    GraphicsContext graphics = getGraphicsContext2D();
    ChartRenderStyle style = renderStyle();
    graphics.setFill(style.background());
    graphics.fillRect(0, 0, width, height);
    if (pricePoints.isEmpty()) {
      statusLine.clearPricePoint();
      return;
    }

    ChartBounds bounds = chartBounds();
    VisibleWindow visibleWindow = visibleWindow();
    PricePoint currentPricePoint = currentPricePoint(visibleWindow.points());
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

    drawHorizontalGridLines(graphics, bounds, priceRange, yAxisTicks, style);
    drawVerticalGridLines(graphics, bounds, xAxisTicks, style);
    drawAxes(graphics, bounds, style);
    drawYAxisTicks(graphics, bounds, priceRange, yAxisTicks, style);
    drawXAxisTicks(graphics, bounds, xAxisTicks, style);
    drawPriceLine(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCurrentPriceBadge(graphics, bounds, priceRange, visibleWindow.points(), style);
    drawCrosshair(graphics, bounds, priceRange, visibleWindow, style);
    drawAutoscaleButton(graphics, bounds, style);
    statusLine.setPricePoint(currentPricePoint);
  }

  @Override
  public void refreshView() {
    drawChart();
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

  ChartRenderStyle renderStyle() {
    return new ChartRenderStyle(
      chartBackground.getValue(),
      chartAxis.getValue(),
      chartGrid.getValue(),
      chartMutedForeground.getValue(),
      chartSeries.getValue(),
      chartCrosshair.getValue(),
      chartBadgeBackground.getValue(),
      chartBadgeForeground.getValue(),
      chartPrimary.getValue(),
      chartPrimaryForeground.getValue(),
      chartAxisFont.getValue(),
      chartBadgeFont.getValue(),
      chartAxisLineWidth.getValue().doubleValue(),
      chartGridLineWidth.getValue().doubleValue(),
      chartSeriesLineWidth.getValue().doubleValue(),
      chartBadgeHeight.getValue().doubleValue(),
      chartControlRadius.getValue().doubleValue()
    );
  }

  private StyleableProperty<Paint> paintProperty(
    String name,
    String cssProperty,
    Paint initialValue,
    Function<ChartCanvas, StyleableProperty<Paint>> accessor
  ) {
    StyleableProperty<Paint> property = STYLEABLES.createStyleablePaintProperty(
      this,
      name,
      cssProperty,
      accessor,
      initialValue
    );
    ((Observable) property).addListener(ignored -> requestRedraw());
    return property;
  }

  private StyleableProperty<Font> fontProperty(
    String name,
    String cssProperty,
    Font initialValue,
    Function<ChartCanvas, StyleableProperty<Font>> accessor
  ) {
    StyleableProperty<Font> property = STYLEABLES.createStyleableFontProperty(
      this,
      name,
      cssProperty,
      accessor,
      initialValue
    );
    ((Observable) property).addListener(ignored -> requestRedraw());
    return property;
  }

  private StyleableProperty<Number> numberProperty(
    String name,
    String cssProperty,
    Number initialValue,
    Function<ChartCanvas, StyleableProperty<Number>> accessor
  ) {
    StyleableProperty<Number> property = STYLEABLES.createStyleableNumberProperty(
      this,
      name,
      cssProperty,
      accessor,
      initialValue
    );
    ((Observable) property).addListener(ignored -> requestRedraw());
    return property;
  }

  private void requestRedraw() {
    if (redrawScheduled) {
      return;
    }
    redrawScheduled = true;
    Platform.runLater(() -> {
      redrawScheduled = false;
      drawChart();
    });
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
      ChartBounds bounds = chartBounds();
      boolean toggleAutoscale = autoscaleButtonPressed && isOverAutoscaleButton(event.getX(), event.getY(), bounds);
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
    requestEarlierHistoryIfNeeded();
  }

  private void requestEarlierHistoryIfNeeded() {
    if (pricePoints.isEmpty() || visibleWindow().firstDataIndex() > visiblePricePointCount) {
      return;
    }
    onEarlierHistoryRequested.run();
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

  private PricePoint currentPricePoint(List<PricePoint> visiblePoints) {
    int pointIndex =
      hoveredVisiblePointIndex == null
        ? visiblePoints.size() - 1
        : Math.min(hoveredVisiblePointIndex, visiblePoints.size() - 1);
    return visiblePoints.get(pointIndex);
  }

  private void drawAxes(GraphicsContext graphics, ChartBounds bounds, ChartRenderStyle style) {
    graphics.setStroke(style.axis());
    graphics.setLineWidth(style.axisLineWidth());
    graphics.strokeLine(bounds.right(), bounds.top(), bounds.right(), bounds.bottom());
    graphics.strokeLine(bounds.left(), bounds.bottom(), bounds.right(), bounds.bottom());
  }

  private void drawAutoscaleButton(GraphicsContext graphics, ChartBounds bounds, ChartRenderStyle style) {
    if (!priceAxisHovered) {
      return;
    }

    ButtonBounds button = autoscaleButtonBounds(bounds);
    boolean autoscaleActive = lockedPriceRange == null;

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
    ChartRenderStyle style
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
    ChartRenderStyle style
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
    ChartRenderStyle style
  ) {
    double currentPrice = visiblePoints.get(visiblePoints.size() - 1).price();
    double y = yForPrice(currentPrice, bounds, priceRange);
    if (y < bounds.top() || y > bounds.bottom()) {
      return;
    }

    double badgeWidth = getWidth() - bounds.right();
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
    ChartRenderStyle style
  ) {
    if (crosshairX == null || crosshairY == null) {
      return;
    }

    double x = crosshairX;
    double y = crosshairY;
    double price = priceForY(y, bounds, priceRange);
    int slotIndex = slotIndexForX(x, bounds);
    LocalDate date = dateForSlot(slotIndex, visibleWindow);
    crosshair.draw(
      graphics,
      bounds.left(),
      bounds.top(),
      bounds.right(),
      bounds.bottom(),
      getWidth(),
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
    ChartRenderStyle style
  ) {
    graphics.setStroke(style.grid());
    graphics.setLineWidth(style.gridLineWidth());

    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), visiblePricePointCount, bounds);
      graphics.strokeLine(x, bounds.top(), x, bounds.bottom());
    }
  }

  private void drawXAxisTicks(
    GraphicsContext graphics,
    ChartBounds bounds,
    List<XAxisTick> ticks,
    ChartRenderStyle style
  ) {
    graphics.setFont(style.axisFont());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);

    for (XAxisTick tick : ticks) {
      double x = xForSlot(tick.slotIndex(), visiblePricePointCount, bounds);

      graphics.setStroke(style.axis());
      graphics.setLineWidth(style.gridLineWidth());
      graphics.strokeLine(x, bounds.bottom(), x, bounds.bottom() + 5.0);

      graphics.setFill(style.mutedForeground());
      graphics.fillText(tick.label(), x, bounds.bottom() + 10.0);
    }
  }

  private List<XAxisTick> xAxisTicks(ChartBounds bounds, VisibleWindow visibleWindow) {
    return XAxisTickCalculator.calculate(
      pricePoints.stream().map(PricePoint::date).toList(),
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
    List<PricePoint> visiblePoints,
    ChartRenderStyle style
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
