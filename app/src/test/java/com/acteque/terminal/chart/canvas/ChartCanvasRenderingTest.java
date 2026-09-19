package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ChartCanvasRenderingTest {

  @Test
  void lineWithMarkersDrawsDotsAtPricePointsAndKeepsTheLineSegments() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(point(1, 100, 120, 80, 100), point(2, 110, 120, 80, 110), point(3, 100, 120, 80, 100)),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();

      interactor.setChartType(ChartType.LINE_WITH_MARKERS);
      builder.drawChart();
      PixelReader marked = canvas.snapshot(null, null).getPixelReader();
      Color seriesColor = (Color) builder.renderStyle().line();
      assertCloserToSeriesColor(marked, line, seriesColor, 368, 29);
      assertEquals(line.getColor(184, 234), marked.getColor(184, 234));

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      assertEquals(line.getColor(368, 29), canvas.snapshot(null, null).getPixelReader().getColor(368, 29));
    });
  }

  @Test
  void lineWithMarkersDrawsOneDotForASinglePoint() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(List.of(point(1, 100, 110, 90, 100)), ChartInterval.DAILY);
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();

      interactor.setChartType(ChartType.LINE_WITH_MARKERS);
      builder.drawChart();
      PixelReader marked = canvas.snapshot(null, null).getPixelReader();
      assertEquals(builder.renderStyle().line(), marked.getColor(368, 234));
      assertCloserToSeriesColor(marked, line, (Color) builder.renderStyle().line(), 368, 231);
      assertEquals(line.getColor(368, 240), marked.getColor(368, 240));
    });
  }

  @Test
  void rendersRisingAndFallingBodiesAndSwitchesBackToTheCloseLine() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(
          point(1, 100, 110, 90, 100),
          point(2, 100, 120, 90, 110),
          point(3, 110, 120, 90, 100),
          point(4, 100, 110, 90, 100)
        ),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.CANDLESTICK);
      builder.drawChart();
      PixelReader candles = canvas.snapshot(null, null).getPixelReader();
      assertEquals(builder.renderStyle().candleUp(), candles.getColor(245, 235));
      assertEquals(builder.renderStyle().candleDown(), candles.getColor(491, 235));
      assertNotEquals(builder.renderStyle().background(), candles.getColor(485, 235));

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();
      assertNotEquals(builder.renderStyle().candleUp(), line.getColor(245, 235));
      assertNotEquals(candles.getColor(485, 235), line.getColor(485, 235));
    });
  }

  @Test
  void rendersBarRangesWithOpenTicksOnTheLeftAndCloseTicksOnTheRight() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(
          point(1, 100, 110, 90, 100),
          point(2, 100, 120, 90, 110),
          point(3, 110, 120, 90, 100),
          point(4, 100, 110, 90, 100)
        ),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();

      interactor.setChartType(ChartType.BAR);
      builder.drawChart();
      PixelReader bars = canvas.snapshot(null, null).getPixelReader();
      Color barColor = (Color) builder.renderStyle().bar();
      assertCloserToSeriesColor(bars, line, barColor, 245, 100);
      assertCloserToSeriesColor(bars, line, barColor, 240, 301);
      assertCloserToSeriesColor(bars, line, barColor, 250, 167);
      assertEquals(line.getColor(250, 301), bars.getColor(250, 301));
      assertEquals(line.getColor(240, 167), bars.getColor(240, 167));

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      assertEquals(line.getColor(240, 301), canvas.snapshot(null, null).getPixelReader().getColor(240, 301));
    });
  }

  @Test
  void rendersAFlatSinglePointBar() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(List.of(point(1, 100, 110, 90, 100)), ChartInterval.DAILY);
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.BAR);
      builder.drawChart();
      PixelReader bar = canvas.snapshot(null, null).getPixelReader();
      assertNotEquals(builder.renderStyle().background(), bar.getColor(368, 100));
      assertNotEquals(builder.renderStyle().background(), bar.getColor(363, 234));
      assertNotEquals(builder.renderStyle().background(), bar.getColor(373, 234));
    });
  }

  @Test
  void drawsAFlatCandleAndASinglePointLine() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(List.of(point(1, 100, 110, 90, 100)), ChartInterval.DAILY);
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.CANDLESTICK);
      builder.drawChart();
      PixelReader candle = canvas.snapshot(null, null).getPixelReader();
      assertNotEquals(builder.renderStyle().background(), candle.getColor(368, 234));

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();
      assertEquals(builder.renderStyle().line(), line.getColor(368, 234));

      interactor.setChartType(ChartType.STEP_LINE);
      builder.drawChart();
      PixelReader step = canvas.snapshot(null, null).getPixelReader();
      assertEquals(line.getColor(368, 234), step.getColor(368, 234));
      assertEquals(line.getColor(368, 300), step.getColor(368, 300));
    });
  }

  @Test
  void stepLineHoldsEachCloseUntilTheNextPointThenChangesVertically() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(
          point(1, 100, 120, 80, 100),
          point(2, 100, 120, 80, 110),
          point(3, 110, 120, 80, 100),
          point(4, 100, 120, 80, 100)
        ),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();

      interactor.setChartType(ChartType.STEP_LINE);
      builder.drawChart();
      PixelReader step = canvas.snapshot(null, null).getPixelReader();
      Color seriesColor = (Color) builder.renderStyle().line();
      assertCloserToSeriesColor(step, line, seriesColor, 122, 436);
      assertCloserToSeriesColor(step, line, seriesColor, 245, 234);
      assertCloserToSeriesColor(step, line, seriesColor, 368, 32);
      assertCloserToSeriesColor(step, line, seriesColor, 491, 234);
      assertCloserToSeriesColor(line, step, seriesColor, 122, 234);
      assertCloserToSeriesColor(line, step, seriesColor, 368, 234);
    });
  }

  @Test
  void areaFillsBelowTheCloseLineAndSwitchingBackClearsTheFill() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(List.of(point(1, 100, 120, 80, 100), point(2, 110, 130, 90, 110)), ChartInterval.DAILY);
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      PixelReader line = canvas.snapshot(null, null).getPixelReader();
      Color backgroundBelowLine = line.getColor(350, 330);
      Color backgroundAboveLine = line.getColor(350, 100);
      Color outsidePlot = line.getColor(750, 330);

      interactor.setChartType(ChartType.AREA);
      builder.drawChart();
      PixelReader area = canvas.snapshot(null, null).getPixelReader();
      Color expectedFill = backgroundBelowLine.interpolate((Color) builder.renderStyle().line(), 0.2);
      assertColorNear(expectedFill, area.getColor(350, 330));
      assertEquals(backgroundAboveLine, area.getColor(350, 100));
      assertEquals(outsidePlot, area.getColor(750, 330));
      assertTrue(area.getColor(368, 234).getRed() < 0.1);

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      assertEquals(backgroundBelowLine, canvas.snapshot(null, null).getPixelReader().getColor(350, 330));
    });
  }

  @Test
  void singlePointAreaKeepsOnlyTheLineDot() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(List.of(point(1, 100, 110, 90, 100)), ChartInterval.DAILY);
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);

      interactor.setChartType(ChartType.LINE);
      builder.drawChart();
      Color belowDot = canvas.snapshot(null, null).getPixelReader().getColor(368, 300);

      interactor.setChartType(ChartType.AREA);
      builder.drawChart();
      PixelReader area = canvas.snapshot(null, null).getPixelReader();
      assertEquals(builder.renderStyle().line(), area.getColor(368, 234));
      assertEquals(belowDot, area.getColor(368, 300));
    });
  }

  private static void assertColorNear(Color expected, Color actual) {
    assertEquals(expected.getRed(), actual.getRed(), 0.01);
    assertEquals(expected.getGreen(), actual.getGreen(), 0.01);
    assertEquals(expected.getBlue(), actual.getBlue(), 0.01);
  }

  private static void assertCloserToSeriesColor(
    PixelReader expected,
    PixelReader other,
    Color seriesColor,
    int x,
    int y
  ) {
    assertTrue(
      colorDistance(expected.getColor(x, y), seriesColor) < colorDistance(other.getColor(x, y), seriesColor),
      "Expected the series color at (" + x + ", " + y + ")"
    );
  }

  private static double colorDistance(Color first, Color second) {
    return (
      Math.abs(first.getRed() - second.getRed()) +
      Math.abs(first.getGreen() - second.getGreen()) +
      Math.abs(first.getBlue() - second.getBlue())
    );
  }

  private static PricePoint point(int day, double open, double high, double low, double close) {
    return new PricePoint(LocalDate.of(2026, 1, day), open, high, low, close, 1_000);
  }
}
