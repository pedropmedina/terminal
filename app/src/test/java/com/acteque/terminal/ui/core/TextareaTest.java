package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class TextareaTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  @Test
  void providesJavaFxTextAreaBehaviorAndCoreStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Textarea textarea = new Textarea("Market notes");
      textarea.setPromptText("Add notes");

      assertEquals("Market notes", textarea.getText());
      assertEquals("Add notes", textarea.getPromptText());
      assertEquals(2, textarea.getPrefRowCount());
      assertTrue(textarea.isWrapText());
      assertTrue(textarea.getStyleClass().contains("text-area"));
      assertTrue(textarea.getStyleClass().contains("core-textarea"));
      assertFalse(textarea.isInvalid());
    });
  }

  @Test
  void exposesBindableInvalidStateAsAPseudoClass() {
    FxTestSupport.runAndWait(() -> {
      Textarea textarea = new Textarea();
      SimpleBooleanProperty invalid = new SimpleBooleanProperty();
      textarea.invalidProperty().bind(invalid);

      invalid.set(true);
      assertTrue(textarea.isInvalid());
      assertTrue(textarea.getPseudoClassStates().contains(INVALID));

      invalid.set(false);
      assertFalse(textarea.isInvalid());
      assertFalse(textarea.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void resolvesShadcnDimensionsAndFoundationStylesFromCss() {
    FxTestSupport.runAndWait(() -> {
      Textarea textarea = new Textarea();
      StackPane root = themedRoot(textarea, AppTheme.LIGHT);

      root.applyCss();

      assertEquals(64.0, textarea.minHeight(-1.0));
      assertEquals(0.0, textarea.minWidth(-1.0));
      assertEquals(14.0, textarea.getFont().getSize());
      assertEquals(Color.TRANSPARENT, background(textarea));
      assertEquals(Color.web("#e5e5e5"), textarea.getBorder().getStrokes().getFirst().getTopStroke());

      Node content = textarea.lookup(".content");
      assertTrue(content instanceof Region);
      assertEquals(new Insets(8.0, 10.0, 8.0, 10.0), ((Region) content).getPadding());
      assertEquals(Color.TRANSPARENT, ((Region) content).getBackground().getFills().getFirst().getFill());
    });
  }

  @Test
  void mapsDisabledAndDarkBackgrounds() {
    FxTestSupport.runAndWait(() -> {
      Textarea lightTextarea = new Textarea();
      StackPane lightRoot = themedRoot(lightTextarea, AppTheme.LIGHT);
      lightTextarea.setDisable(true);
      lightRoot.applyCss();

      assertEquals(Color.web("rgba(229, 229, 229, 0.5)"), background(lightTextarea));
      assertEquals(0.5, lightTextarea.getOpacity());

      Textarea darkTextarea = new Textarea();
      StackPane darkRoot = themedRoot(darkTextarea, AppTheme.DARK);
      darkRoot.applyCss();
      assertEquals(Color.web("rgba(255, 255, 255, 0.045)"), background(darkTextarea));

      darkTextarea.setDisable(true);
      darkRoot.applyCss();
      assertEquals(Color.web("rgba(255, 255, 255, 0.12)"), background(darkTextarea));
    });
  }

  @Test
  void rendersShadcnRingWhenFocusIsVisible() {
    FxTestSupport.runAndWait(() -> {
      Textarea textarea = new Textarea();
      StackPane root = themedRoot(textarea, AppTheme.LIGHT);
      textarea.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-visible"), true);

      root.applyCss();

      assertEquals(2, textarea.getBorder().getStrokes().size());
      assertEquals(Color.web("#a1a1a1"), textarea.getBorder().getStrokes().get(0).getTopStroke());
      assertEquals(Color.web("rgba(161, 161, 161, 0.5)"), textarea.getBorder().getStrokes().get(1).getTopStroke());
      assertEquals(1.0, textarea.getBorder().getStrokes().get(0).getWidths().getTop());
      assertEquals(3.0, textarea.getBorder().getStrokes().get(1).getWidths().getTop());
      assertEquals(-3.0, textarea.getBorder().getStrokes().get(1).getInsets().getTop());
      assertNull(textarea.getEffect());
    });
  }

  @Test
  void resolvesInvalidBorderAndRingForBothThemes() {
    FxTestSupport.runAndWait(() -> {
      assertInvalidColors(AppTheme.LIGHT, Color.web("#e7000b"), Color.web("rgba(231, 0, 11, 0.2)"));
      assertInvalidColors(AppTheme.DARK, Color.web("rgba(255, 100, 103, 0.5)"), Color.web("rgba(255, 100, 103, 0.4)"));
    });
  }

  private static void assertInvalidColors(AppTheme theme, Color border, Color ring) {
    Textarea textarea = new Textarea();
    StackPane root = themedRoot(textarea, theme);
    textarea.setInvalid(true);
    root.applyCss();

    assertEquals(2, textarea.getBorder().getStrokes().size());
    assertEquals(border, textarea.getBorder().getStrokes().get(0).getTopStroke());
    assertEquals(ring, textarea.getBorder().getStrokes().get(1).getTopStroke());
  }

  private static Color background(Textarea textarea) {
    return (Color) textarea.getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(Textarea textarea, AppTheme theme) {
    StackPane root = new StackPane(textarea);
    new ThemeManager(new Scene(root), theme);
    return root;
  }
}
