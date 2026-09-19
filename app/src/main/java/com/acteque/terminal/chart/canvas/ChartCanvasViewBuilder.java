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

  private boolean redrawScheduled;

  ChartCanvasViewBuilder(ChartCanvasModel model, ChartCanvasInteractor interactor) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.interactor = Objects.requireNonNull(interactor, "interactor cannot be null");
    renderer = new CanvasRenderer(this, model);

    getStyleClass().add("chart-canvas");

    ReloadHooks.register(this); // Development runs refresh this view after class redefinition.
    widthProperty().addListener((ignored, oldWidth, newWidth) -> drawChart());
    heightProperty().addListener((ignored, oldHeight, newHeight) -> drawChart());
    model.revisionProperty().addListener(ignored -> drawChart());
    setEventsListeners();
  }

  @Override
  public Canvas build() {
    return this;
  }

  void drawChart() {
    renderer.draw(
      renderStyle(),
      interactor.visibleWindow(),
      model.pricePoints.isEmpty() ? null : interactor.displayedPriceRange(renderer.chartBounds().height())
    );
  }

  @Override
  public void refreshView() {
    drawChart();
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

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
      chartAxisLabelSpacing.getValue().doubleValue()
    );
  }

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
      updateCursor(event.getX(), event.getY());
    });

    setOnMouseExited(event -> {
      interactor.exitPointer();
      if (model.dragMode == DragMode.NONE) {
        setCursor(Cursor.DEFAULT);
      }
    });

    setOnMousePressed(event -> {
      if (model.pricePoints.isEmpty()) {
        interactor.beginEmptyPress();
        return;
      }

      ChartBounds bounds = renderer.chartBounds();
      if (renderer.isOverAutoscaleButton(event.getX(), event.getY(), bounds)) {
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
    });

    setOnMouseDragged(event -> {
      if (model.pricePoints.isEmpty()) {
        return;
      }

      if (model.dragMode == DragMode.ZOOM_DATE) {
        interactor.handleXAxisZoom(event.getX());
      } else if (model.dragMode == DragMode.ZOOM_PRICE) {
        interactor.handleYAxisZoom(event.getY(), renderer.chartBounds().height());
      } else if (model.dragMode == DragMode.PAN) {
        ChartBounds bounds = renderer.chartBounds();
        interactor.handlePan(event.getX(), event.getY(), bounds.width(), bounds.height());
        updateStatusLinePoint(event.getX(), event.getY());
      }
    });

    setOnMouseReleased(event -> {
      ChartBounds bounds = renderer.chartBounds();
      boolean toggleAutoscale =
        model.autoscaleButtonPressed && renderer.isOverAutoscaleButton(event.getX(), event.getY(), bounds);
      interactor.endPress(toggleAutoscale, displayedPriceRange());
      updateStatusLinePoint(event.getX(), event.getY());
      updateCursor(event.getX(), event.getY());
    });

    // Keep this no-op handler for hot-reload compatibility. A live canvas may still reference the
    // generated lambda method from an earlier class definition. Press/release performs activation,
    // so handling the subsequent clicked event would toggle the state a second time.
    setOnMouseClicked(event -> keepHotReloadClickHandler());
  }

  private void keepHotReloadClickHandler() {}

  private ChartCanvasModel.PriceRange displayedPriceRange() {
    return interactor.displayedPriceRange(renderer.chartBounds().height());
  }

  private void updateCursor(double x, double y) {
    ChartBounds bounds = renderer.chartBounds();
    if (renderer.isOverAutoscaleButton(x, y, bounds)) {
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

  private void updateStatusLinePoint(double x, double y) {
    if (model.pricePoints.isEmpty()) {
      interactor.movePointer(null, null, null, false);
      return;
    }

    ChartBounds bounds = renderer.chartBounds();
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

    boolean hovered = renderer.isOverPriceAxisArea(x, y, bounds) || renderer.isOverAutoscaleButton(x, y, bounds);
    interactor.movePointer(pointIndex, nextCrosshairX, nextCrosshairY, hovered);
  }
}
