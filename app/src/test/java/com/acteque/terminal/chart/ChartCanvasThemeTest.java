package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
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
      ChartCanvas canvas = new ChartCanvas(
        List.of(new PricePoint(LocalDate.of(2026, 8, 24), 100, 101, 99, 100, 1_000)),
        "ACME"
      );
      StackPane root = new StackPane(canvas);
      Scene scene = new Scene(root, 800, 500);
      ThemeManager themes = new ThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();

      assertEquals(Color.web("#ffffff"), canvas.renderStyle().background());
      assertEquals(Color.web("#e76e00"), canvas.renderStyle().series());
      assertEquals(24.0, canvas.renderStyle().badgeHeight());

      themes.setTheme(AppTheme.DARK);
      root.applyCss();

      assertEquals(Color.web("#0a0a0a"), canvas.renderStyle().background());
      assertEquals(Color.web("#7c3aed"), canvas.renderStyle().series());
    });
  }
}
