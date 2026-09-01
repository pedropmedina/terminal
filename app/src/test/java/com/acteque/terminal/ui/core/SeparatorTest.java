package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class SeparatorTest {

  @Test
  void defaultsToHorizontalAndProvidesTheCoreStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Separator separator = new Separator();

      assertEquals(Orientation.HORIZONTAL, separator.getOrientation());
      assertTrue(separator.getStyleClass().contains("separator"));
      assertTrue(separator.getStyleClass().contains("core-separator"));
    });
  }

  @Test
  void supportsVerticalOrientationAndRejectsNullConstruction() {
    FxTestSupport.runAndWait(() -> {
      Separator separator = new Separator(Orientation.VERTICAL);

      assertEquals(Orientation.VERTICAL, separator.getOrientation());
      assertThrows(NullPointerException.class, () -> new Separator(null));
    });
  }

  @Test
  void mapsHorizontalDimensionsAndBorderColorFromCss() {
    FxTestSupport.runAndWait(() -> {
      Separator separator = new Separator();
      StackPane root = themedRoot(separator, AppTheme.LIGHT, 240.0, 80.0);

      root.applyCss();
      root.layout();

      assertEquals(240.0, separator.getWidth());
      assertEquals(1.0, separator.getHeight());
      assertEquals(Color.web("#e5e5e5"), line(separator).getBackground().getFills().getFirst().getFill());
    });
  }

  @Test
  void mapsVerticalDimensionsAndDarkBorderColorFromCss() {
    FxTestSupport.runAndWait(() -> {
      Separator separator = new Separator(Orientation.VERTICAL);
      StackPane root = themedRoot(separator, AppTheme.DARK, 240.0, 80.0);

      root.applyCss();
      root.layout();

      assertEquals(1.0, separator.getWidth());
      assertEquals(80.0, separator.getHeight());
      assertEquals(
        Color.web("rgba(255, 255, 255, 0.1)"),
        line(separator).getBackground().getFills().getFirst().getFill()
      );
    });
  }

  private static StackPane themedRoot(Separator separator, AppTheme theme, double width, double height) {
    StackPane root = new StackPane(separator);
    new ThemeManager(new Scene(root, width, height), theme);
    root.resize(width, height);
    return root;
  }

  private static Region line(Separator separator) {
    return (Region) separator.lookup(".line");
  }
}
