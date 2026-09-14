package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.DragMode;
import com.acteque.terminal.chart.canvas.ChartCanvasModel.PriceRange;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class ChartCanvasInteractorTest {

  @Test
  void publishesTheHoveredPointAndFallsBackToTheLatestVisiblePoint() {
    ChartCanvasModel model = new ChartCanvasModel();
    ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
    List<PricePoint> points = points(12);
    interactor.initialize(points, ChartInterval.DAILY);

    assertSame(points.getLast(), model.displayedPricePointProperty().get());

    interactor.movePointer(3, 120.0, 80.0, false);

    assertSame(points.get(3), model.displayedPricePointProperty().get());

    interactor.exitPointer();

    assertSame(points.getLast(), model.displayedPricePointProperty().get());
  }

  @Test
  void resettingTheInstrumentClearsViewportInteractionState() {
    ChartCanvasModel model = new ChartCanvasModel();
    ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
    interactor.initialize(points(12), ChartInterval.DAILY);
    model.visiblePricePointOffset = 2;
    model.yZoomScale = 2.0;
    model.lockedPriceRange = new PriceRange(90.0, 110.0);
    interactor.movePointer(2, 20.0, 30.0, true);
    interactor.beginPan(20.0, 30.0);

    interactor.setInstrumentPricePoints(List.of());

    assertEquals(0, model.visiblePricePointCount);
    assertEquals(0, model.visiblePricePointOffset);
    assertEquals(1.0, model.yZoomScale);
    assertNull(model.lockedPriceRange);
    assertNull(model.crosshairX);
    assertEquals(DragMode.NONE, model.dragMode);
    assertNull(model.displayedPricePointProperty().get());
  }

  @Test
  void horizontalZoomAndPanAreClampedAndRequestEarlierHistory() {
    ChartCanvasModel model = new ChartCanvasModel();
    ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
    AtomicInteger requests = new AtomicInteger();
    interactor.initialize(points(20), ChartInterval.DAILY);
    interactor.onEarlierHistoryRequested(requests::incrementAndGet);

    interactor.beginXAxisZoom(0.0);
    interactor.handleXAxisZoom(96.0);
    assertEquals(8, model.visiblePricePointCount);

    interactor.beginPan(0.0, 0.0);
    interactor.handlePan(2_000.0, 0.0, 700.0, 400.0);

    assertEquals(12, model.visiblePricePointOffset);
    assertEquals(0, interactor.visibleWindow().firstDataIndex());
    assertEquals(1, requests.get());
  }

  @Test
  void autoscaleToggleLocksAndRestoresTheAutomaticRange() {
    ChartCanvasModel model = new ChartCanvasModel();
    ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
    interactor.initialize(points(10), ChartInterval.DAILY);
    PriceRange range = new PriceRange(90.0, 120.0);

    interactor.beginAutoscalePress();
    interactor.endPress(true, range);
    assertEquals(range, model.lockedPriceRange);

    interactor.beginAutoscalePress();
    interactor.endPress(true, range);
    assertNull(model.lockedPriceRange);
    assertEquals(1.0, model.yZoomScale);
  }

  @Test
  void verticalZoomLocksTheScaledRange() {
    ChartCanvasModel model = new ChartCanvasModel();
    ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
    interactor.initialize(points(10), ChartInterval.DAILY);
    PriceRange range = new PriceRange(90.0, 120.0);

    interactor.beginYAxisZoom(0.0, range);
    interactor.handleYAxisZoom(ChartCanvasInteractor.Y_ZOOM_PIXELS_PER_STEP, 400.0);

    assertEquals(Math.E, model.yZoomScale, 0.000_001);
    assertEquals(range.span() * Math.E, model.lockedPriceRange.span(), 0.000_001);
  }

  private static List<PricePoint> points(int count) {
    LocalDate start = LocalDate.of(2026, 1, 1);
    return IntStream.range(0, count)
      .mapToObj(index ->
        new PricePoint(start.plusDays(index), 100 + index, 101 + index, 99 + index, 100 + index, 1_000)
      )
      .toList();
  }
}
