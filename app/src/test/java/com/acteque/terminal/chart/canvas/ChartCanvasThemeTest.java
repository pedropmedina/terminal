package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class ChartCanvasThemeTest {

  @Test
  void resolvesLightAndDarkCanvasPaintsFromTheSameSemanticTheme() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(
          new PricePoint(LocalDate.of(2026, 8, 24), 100, 120, 80, 100, 1_000),
          new PricePoint(LocalDate.of(2026, 8, 25), 110, 130, 90, 110, 1_000)
        ),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      javafx.scene.canvas.Canvas canvas = builder.build();
      canvas.setWidth(800);
      canvas.setHeight(500);
      StackPane root = new StackPane(canvas);
      Scene scene = new Scene(root, 800, 500);
      AppThemeManager themes = new AppThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();

      assertEquals(Color.web("#ffffff"), builder.renderStyle().background());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().line());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().bar());
      assertEquals(1.0, builder.renderStyle().barStrokeWidth());
      assertEquals(6.0, builder.renderStyle().barTickMaxWidth());
      assertEquals(6.0, builder.renderStyle().markerDiameter());
      assertEquals(0.2, builder.renderStyle().areaOpacity());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleUp());
      assertEquals(Color.web("#ffffff"), builder.renderStyle().candleDown());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleBorder());
      assertEquals(24.0, builder.renderStyle().badgeHeight());
      interactor.setChartType(ChartType.AREA);
      builder.drawChart();
      assertAreaFillMatchesTheme(builder, canvas);

      themes.setTheme(AppTheme.DARK);
      root.applyCss();

      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().background());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().line());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().bar());
      assertEquals(0.2, builder.renderStyle().areaOpacity());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().candleUp());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleDown());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().candleBorder());
      builder.drawChart();
      assertAreaFillMatchesTheme(builder, canvas);
    });
  }

  private static void assertAreaFillMatchesTheme(ChartCanvasViewBuilder builder, javafx.scene.canvas.Canvas canvas) {
    RenderStyle style = builder.renderStyle();
    Color expected = ((Color) style.background()).interpolate((Color) style.line(), style.areaOpacity());
    Color actual = canvas.snapshot(null, null).getPixelReader().getColor(350, 330);
    assertEquals(expected.getRed(), actual.getRed(), 0.01);
    assertEquals(expected.getGreen(), actual.getGreen(), 0.01);
    assertEquals(expected.getBlue(), actual.getBlue(), 0.01);
  }
}
