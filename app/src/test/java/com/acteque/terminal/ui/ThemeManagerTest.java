package com.acteque.terminal.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

class ThemeManagerTest {

  @Test
  void switchesTheRootThemeWithoutDuplicatingTheStylesheet() {
    FxTestSupport.runAndWait(() -> {
      StackPane root = new StackPane();
      Scene scene = new Scene(root);
      ThemeManager themes = new ThemeManager(scene, AppTheme.LIGHT);

      assertFalse(root.getStyleClass().contains("app-root"));
      assertTrue(root.getStyleClass().contains("theme-light"));
      assertFalse(root.getStyleClass().contains("theme-dark"));
      assertEquals(1, scene.getStylesheets().size());

      themes.setTheme(AppTheme.DARK);

      assertEquals(AppTheme.DARK, themes.getTheme());
      assertFalse(root.getStyleClass().contains("theme-light"));
      assertTrue(root.getStyleClass().contains("theme-dark"));
      assertEquals(1, scene.getStylesheets().size());
    });
  }
}
