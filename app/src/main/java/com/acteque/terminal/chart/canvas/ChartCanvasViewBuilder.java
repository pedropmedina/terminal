package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.canvas.CanvasRenderer.ChartBounds;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.DragMode;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.scene.Cursor;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.Builder;

final class ChartCanvasViewBuilder extends Canvas implements Builder<Canvas>, ReloadTarget {

  private static final StyleablePropertyFactory<ChartCanvasViewBuilder> STYLEABLES = new StyleablePropertyFactory<>(
    Canvas.getClassCssMetaData()
  );

  private final ChartCanvasModel model;
  private final ChartCanvasInteractor interactor;
  private final CanvasRenderer renderer;

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
  private final StyleableProperty<Paint> chartLine = paintProperty(
    "chartLine",
    "-chart-line",
    Color.BLACK,
    canvas -> canvas.chartLine
  );
  private final StyleableProperty<Paint> chartBar = paintProperty(
    "chartBar",
    "-chart-bar",
    Color.BLACK,
    canvas -> canvas.chartBar
  );
  private final StyleableProperty<Paint> chartCandleUp = paintProperty(
    "chartCandleUp",
    "-chart-candle-up",
    Color.BLACK,
    canvas -> canvas.chartCandleUp
  );
  private final StyleableProperty<Paint> chartCandleDown = paintProperty(
    "chartCandleDown",
    "-chart-candle-down",
    Color.WHITE,
    canvas -> canvas.chartCandleDown
  );
  private final StyleableProperty<Paint> chartCandleBorder = paintProperty(
    "chartCandleBorder",
    "-chart-candle-border",
    Color.BLACK,
    canvas -> canvas.chartCandleBorder
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
  private final StyleableProperty<Number> chartLineWidth = numberProperty(
    "chartLineWidth",
    "-chart-line-width",
    2.2,
    canvas -> canvas.chartLineWidth
  );
  private final StyleableProperty<Number> chartBarStrokeWidth = numberProperty(
    "chartBarStrokeWidth",
    "-chart-bar-stroke-width",
    1.0,
    canvas -> canvas.chartBarStrokeWidth
  );
  private final StyleableProperty<Number> chartBarTickMaxWidth = numberProperty(
    "chartBarTickMaxWidth",
    "-chart-bar-tick-max-width",
    6.0,
    canvas -> canvas.chartBarTickMaxWidth
  );
  private final StyleableProperty<Number> chartMarkerDiameter = numberProperty(
    "chartMarkerDiameter",
    "-chart-marker-diameter",
    6.0,
    canvas -> canvas.chartMarkerDiameter
  );
  private final StyleableProperty<Number> chartAreaOpacity = numberProperty(
    "chartAreaOpacity",
    "-chart-area-opacity",
    0.2,
    canvas -> canvas.chartAreaOpacity
  );
  private final StyleableProperty<Number> chartCandleBodyMaxWidth = numberProperty(
    "chartCandleBodyMaxWidth",
    "-chart-candle-body-max-width",
    12.0,
    canvas -> canvas.chartCandleBodyMaxWidth
  );
  private final StyleableProperty<Number> chartCandleStrokeWidth = numberProperty(
    "chartCandleStrokeWidth",
    "-chart-candle-stroke-width",
    1.0,
    canvas -> canvas.chartCandleStrokeWidth
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
  private final StyleableProperty<Number> chartAxisLabelSpacing = numberProperty(
    "chartAxisLabelSpacing",
    "-chart-axis-label-spacing",
    56.0,
    canvas -> canvas.chartAxisLabelSpacing
  );
  private final StyleableProperty<Number> chartLeftMargin = numberProperty(
    "chartLeftMargin",
    "-chart-left-margin",
    0.0,
    canvas -> canvas.chartLeftMargin
  );
  private final StyleableProperty<Number> chartRightMargin = numberProperty(
    "chartRightMargin",
    "-chart-right-margin",
    64.0,
    canvas -> canvas.chartRightMargin
  );
  private final StyleableProperty<Number> chartTopMargin = numberProperty(
    "chartTopMargin",
    "-chart-top-margin",
    0.0,
    canvas -> canvas.chartTopMargin
  );
  private final StyleableProperty<Number> chartBottomMargin = numberProperty(
    "chartBottomMargin",
    "-chart-bottom-margin",
    32.0,
    canvas -> canvas.chartBottomMargin
  );
  private final StyleableProperty<Number> chartCurrentPriceTextOffset = numberProperty(
    "chartCurrentPriceTextOffset",
    "-chart-current-price-text-offset",
    10.0,
    canvas -> canvas.chartCurrentPriceTextOffset
  );
  private final StyleableProperty<Number> chartAutoscaleButtonSize = numberProperty(
    "chartAutoscaleButtonSize",
    "-chart-autoscale-button-size",
    24.0,
    canvas -> canvas.chartAutoscaleButtonSize
  );
  private final StyleableProperty<Number> chartAutoscaleButtonXOffset = numberProperty(
    "chartAutoscaleButtonXOffset",
    "-chart-autoscale-button-x-offset",
    8.0,
    canvas -> canvas.chartAutoscaleButtonXOffset
  );
  private final StyleableProperty<Number> chartAutoscaleButtonYOffset = numberProperty(
    "chartAutoscaleButtonYOffset",
    "-chart-autoscale-button-y-offset",
    0.0,
    canvas -> canvas.chartAutoscaleButtonYOffset
  );
  private final StyleableProperty<Number> chartAxisTickLength = numberProperty(
    "chartAxisTickLength",
    "-chart-axis-tick-length",
    5.0,
    canvas -> canvas.chartAxisTickLength
  );
  private final StyleableProperty<Number> chartAxisTextOffset = numberProperty(
    "chartAxisTextOffset",
    "-chart-axis-text-offset",
    10.0,
    canvas -> canvas.chartAxisTextOffset
  );
  private final StyleableProperty<Number> chartCalendarBadgeWidth = numberProperty(
    "chartCalendarBadgeWidth",
    "-chart-calendar-badge-width",
    120.0,
    canvas -> canvas.chartCalendarBadgeWidth
  );
  private final StyleableProperty<Number> chartIntradayBadgeWidth = numberProperty(
    "chartIntradayBadgeWidth",
    "-chart-intraday-badge-width",
    180.0,
    canvas -> canvas.chartIntradayBadgeWidth
  );
  private final StyleableProperty<Number> chartBadgeTextOffset = numberProperty(
    "chartBadgeTextOffset",
    "-chart-badge-text-offset",
    10.0,
    canvas -> canvas.chartBadgeTextOffset
  );
  private final StyleableProperty<Number> chartCrosshairDashLength = numberProperty(
    "chartCrosshairDashLength",
    "-chart-crosshair-dash-length",
    4.0,
    canvas -> canvas.chartCrosshairDashLength
  );

  private boolean redrawScheduled;

  /**
   * Creates a canvas view connected to its model and interactor.
   *
   * @param model the observable canvas state
   * @param interactor the canvas workflow and transition owner
   */
  ChartCanvasViewBuilder(ChartCanvasModel model, ChartCanvasInteractor interactor) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.interactor = Objects.requireNonNull(interactor, "interactor cannot be null");
    renderer = new CanvasRenderer(this, model);

    getStyleClass().add("chart-canvas");

    ReloadHooks.register(this); // Development runs refresh this view after class redefinition.
    widthProperty().addListener((ignored, oldWidth, newWidth) -> drawChart());
    heightProperty().addListener((ignored, oldHeight, newHeight) -> drawChart());
    model.revisionProperty().addListener(ignored -> drawChart());
    connectEventHandlers();
  }

  /** @return this canvas view */
  @Override
  public Canvas build() {
    return this;
  }

  /** Draws all canvas layers synchronously from current model and CSS state. */
  void drawChart() {
    RenderStyle style = renderStyle();
    renderer.draw(
      style,
      interactor.visibleWindow(),
      model.pricePoints.isEmpty() ? null : interactor.displayedPriceRange(renderer.chartBounds(style).height())
    );
  }

  /** Redraws the canvas after development-time class redefinition. */
  @Override
  public void refreshView() {
    drawChart();
  }

  /** @return CSS metadata for the canvas's custom drawing properties */
  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

  /** @return an immutable snapshot of CSS-resolved drawing values */
  RenderStyle renderStyle() {
    return new RenderStyle(
      chartBackground.getValue(),
      chartAxis.getValue(),
      chartGrid.getValue(),
      chartMutedForeground.getValue(),
      chartLine.getValue(),
      chartBar.getValue(),
      chartCandleUp.getValue(),
      chartCandleDown.getValue(),
      chartCandleBorder.getValue(),
      chartCrosshair.getValue(),
      chartBadgeBackground.getValue(),
      chartBadgeForeground.getValue(),
      chartPrimary.getValue(),
      chartPrimaryForeground.getValue(),
      chartAxisFont.getValue(),
      chartBadgeFont.getValue(),
      chartAxisLineWidth.getValue().doubleValue(),
      chartGridLineWidth.getValue().doubleValue(),
      chartLineWidth.getValue().doubleValue(),
      chartBarStrokeWidth.getValue().doubleValue(),
      chartBarTickMaxWidth.getValue().doubleValue(),
      chartMarkerDiameter.getValue().doubleValue(),
      chartAreaOpacity.getValue().doubleValue(),
      chartCandleBodyMaxWidth.getValue().doubleValue(),
      chartCandleStrokeWidth.getValue().doubleValue(),
      chartBadgeHeight.getValue().doubleValue(),
      chartControlRadius.getValue().doubleValue(),
      chartAxisLabelSpacing.getValue().doubleValue(),
      chartLeftMargin.getValue().doubleValue(),
      chartRightMargin.getValue().doubleValue(),
      chartTopMargin.getValue().doubleValue(),
      chartBottomMargin.getValue().doubleValue(),
      chartCurrentPriceTextOffset.getValue().doubleValue(),
      chartAutoscaleButtonSize.getValue().doubleValue(),
      chartAutoscaleButtonXOffset.getValue().doubleValue(),
      chartAutoscaleButtonYOffset.getValue().doubleValue(),
      chartAxisTickLength.getValue().doubleValue(),
      chartAxisTextOffset.getValue().doubleValue(),
      chartCalendarBadgeWidth.getValue().doubleValue(),
      chartIntradayBadgeWidth.getValue().doubleValue(),
      chartBadgeTextOffset.getValue().doubleValue(),
      chartCrosshairDashLength.getValue().doubleValue()
    );
  }

  /**
   * Creates a paint-valued CSS property that schedules redraws when changed.
   *
   * @param name the JavaFX property name
   * @param cssProperty the CSS property name
   * @param initialValue the fallback paint
   * @param accessor the metadata accessor
   * @return the created styleable property
   */
  private StyleableProperty<Paint> paintProperty(
    String name,
    String cssProperty,
    Paint initialValue,
    Function<ChartCanvasViewBuilder, StyleableProperty<Paint>> accessor
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

  /**
   * Creates a font-valued CSS property that schedules redraws when changed.
   *
   * @param name the JavaFX property name
   * @param cssProperty the CSS property name
   * @param initialValue the fallback font
   * @param accessor the metadata accessor
   * @return the created styleable property
   */
  private StyleableProperty<Font> fontProperty(
    String name,
    String cssProperty,
    Font initialValue,
    Function<ChartCanvasViewBuilder, StyleableProperty<Font>> accessor
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

  /**
   * Creates a numeric CSS property that schedules redraws when changed.
   *
   * @param name the JavaFX property name
   * @param cssProperty the CSS property name
   * @param initialValue the fallback number
   * @param accessor the metadata accessor
   * @return the created styleable property
   */
  private StyleableProperty<Number> numberProperty(
    String name,
    String cssProperty,
    Number initialValue,
    Function<ChartCanvasViewBuilder, StyleableProperty<Number>> accessor
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

  /** Coalesces CSS-triggered redraws onto the JavaFX application thread. */
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

  /** Connects JavaFX pointer events to intent handlers. */
  private void connectEventHandlers() {
    setOnMouseMoved(this::handlePointerMoved);
    setOnMouseExited(this::handlePointerExited);
    setOnMousePressed(this::handlePointerPressed);
    setOnMouseDragged(this::handlePointerDragged);
    setOnMouseReleased(this::handlePointerReleased);
    // Keep this no-op handler for hot-reload compatibility. A live canvas may still reference the
    // generated lambda method from an earlier class definition. Press/release performs activation,
    // so handling the subsequent clicked event would toggle the state a second time.
    setOnMouseClicked(event -> keepHotReloadClickHandler());
  }

  /**
   * Updates hover state and cursor feedback for pointer movement.
   *
   * @param event the pointer movement event
   */
  private void handlePointerMoved(MouseEvent event) {
    updateStatusLinePoint(event.getX(), event.getY());
    updateCursor(event.getX(), event.getY());
  }

  /**
   * Clears hover state after the pointer leaves the canvas.
   *
   * @param event the pointer exit event
   */
  private void handlePointerExited(MouseEvent event) {
    interactor.exitPointer();
    if (model.dragMode == DragMode.NONE) {
      setCursor(Cursor.DEFAULT);
    }
  }

  /**
   * Selects the interaction mode for a pointer press.
   *
   * @param event the pointer press event
   */
  private void handlePointerPressed(MouseEvent event) {
    if (model.pricePoints.isEmpty()) {
      interactor.beginEmptyPress();
      return;
    }

    RenderStyle style = renderStyle();
    ChartBounds bounds = renderer.chartBounds(style);
    if (renderer.isOverAutoscaleButton(event.getX(), event.getY(), bounds, style)) {
      interactor.beginAutoscalePress();
      setCursor(Cursor.HAND);
    } else if (renderer.isOverPriceAxisArea(event.getX(), event.getY(), bounds)) {
      interactor.beginYAxisZoom(event.getY(), displayedPriceRange());
      setCursor(Cursor.V_RESIZE);
    } else if (renderer.isOverDateAxisArea(event.getY(), bounds)) {
      interactor.beginXAxisZoom(event.getX());
      setCursor(Cursor.H_RESIZE);
    } else if (renderer.isOverChartArea(event.getX(), event.getY(), bounds)) {
      interactor.beginPan(event.getX(), event.getY());
      setCursor(Cursor.CLOSED_HAND);
    }
  }

  /**
   * Applies zooming or panning while a pointer drag is active.
   *
   * @param event the pointer drag event
   */
  private void handlePointerDragged(MouseEvent event) {
    if (model.pricePoints.isEmpty()) {
      return;
    }

    ChartBounds bounds = renderer.chartBounds(renderStyle());
    if (model.dragMode == DragMode.ZOOM_DATE) {
      interactor.handleXAxisZoom(event.getX());
    } else if (model.dragMode == DragMode.ZOOM_PRICE) {
      interactor.handleYAxisZoom(event.getY(), bounds.height());
    } else if (model.dragMode == DragMode.PAN) {
      interactor.handlePan(event.getX(), event.getY(), bounds.width(), bounds.height());
      updateStatusLinePoint(event.getX(), event.getY());
    }
  }

  /**
   * Completes the active interaction and refreshes hover feedback.
   *
   * @param event the pointer release event
   */
  private void handlePointerReleased(MouseEvent event) {
    RenderStyle style = renderStyle();
    ChartBounds bounds = renderer.chartBounds(style);
    boolean toggleAutoscale =
      model.autoscaleButtonPressed && renderer.isOverAutoscaleButton(event.getX(), event.getY(), bounds, style);
    interactor.endPress(toggleAutoscale, displayedPriceRange());
    updateStatusLinePoint(event.getX(), event.getY());
    updateCursor(event.getX(), event.getY());
  }

  /** Preserves the lambda target used by canvases that survived hot reload. */
  private void keepHotReloadClickHandler() {}

  /** @return the currently displayed vertical price range */
  private ChartCanvasModel.PriceRange displayedPriceRange() {
    return interactor.displayedPriceRange(renderer.chartBounds(renderStyle()).height());
  }

  /**
   * Updates the pointer cursor for the canvas region under the coordinates.
   *
   * @param x the canvas x coordinate
   * @param y the canvas y coordinate
   */
  private void updateCursor(double x, double y) {
    ChartBounds bounds = renderer.chartBounds(renderStyle());
    if (renderer.isOverAutoscaleButton(x, y, bounds, renderStyle())) {
      setCursor(Cursor.HAND);
    } else if (renderer.isOverPriceAxisArea(x, y, bounds)) {
      setCursor(Cursor.V_RESIZE);
    } else if (renderer.isOverDateAxisArea(y, bounds)) {
      setCursor(Cursor.H_RESIZE);
    } else if (renderer.isOverChartArea(x, y, bounds)) {
      setCursor(Cursor.CROSSHAIR);
    } else {
      setCursor(Cursor.DEFAULT);
    }
  }

  /**
   * Publishes the hovered point and crosshair coordinates to the interactor.
   *
   * @param x the canvas x coordinate
   * @param y the canvas y coordinate
   */
  private void updateStatusLinePoint(double x, double y) {
    if (model.pricePoints.isEmpty()) {
      interactor.movePointer(null, null, null, false);
      return;
    }

    ChartBounds bounds = renderer.chartBounds(renderStyle());
    Integer pointIndex = null;
    Double nextCrosshairX = null;
    Double nextCrosshairY = null;

    if (renderer.isOverChartArea(x, y, bounds)) {
      nextCrosshairX = x;
      nextCrosshairY = y;
      int slotIndex = renderer.slotIndexForX(x, bounds);
      if (slotIndex < interactor.visibleWindow().points().size()) {
        pointIndex = slotIndex;
      }
    }

    boolean hovered =
      renderer.isOverPriceAxisArea(x, y, bounds) || renderer.isOverAutoscaleButton(x, y, bounds, renderStyle());
    interactor.movePointer(pointIndex, nextCrosshairX, nextCrosshairY, hovered);
  }
}
