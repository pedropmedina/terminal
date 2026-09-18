package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.PixelReader;
import org.junit.jupiter.api.Test;

class ChartCanvasRenderingTest {

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
    });
  }

  private static PricePoint point(int day, double open, double high, double low, double close) {
    return new PricePoint(LocalDate.of(2026, 1, day), open, high, low, close, 1_000);
  }
}
