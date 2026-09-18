package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartInterval;
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
        List.of(new PricePoint(LocalDate.of(2026, 8, 24), 100, 101, 99, 100, 1_000)),
        ChartInterval.DAILY
      );
      ChartCanvasViewBuilder builder = new ChartCanvasViewBuilder(model, interactor);
      javafx.scene.canvas.Canvas canvas = builder.build();
      StackPane root = new StackPane(canvas);
      Scene scene = new Scene(root, 800, 500);
      AppThemeManager themes = new AppThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();

      assertEquals(Color.web("#ffffff"), builder.renderStyle().background());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().line());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleUp());
      assertEquals(Color.web("#ffffff"), builder.renderStyle().candleDown());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleBorder());
      assertEquals(24.0, builder.renderStyle().badgeHeight());

      themes.setTheme(AppTheme.DARK);
      root.applyCss();

      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().background());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().line());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().candleUp());
      assertEquals(Color.web("#0a0a0a"), builder.renderStyle().candleDown());
      assertEquals(Color.web("#fafafa"), builder.renderStyle().candleBorder());
    });
  }
}
