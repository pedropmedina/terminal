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
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class InputTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  @Test
  void providesJavaFxTextFieldBehaviorAndCoreStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Input input = new Input("AAPL");
      input.setPromptText("Symbol");

      assertEquals("AAPL", input.getText());
      assertEquals("Symbol", input.getPromptText());
      assertTrue(input.getStyleClass().contains("text-field"));
      assertTrue(input.getStyleClass().contains("core-input"));
      assertFalse(input.isInvalid());
    });
  }

  @Test
  void exposesBindableInvalidStateAsAPseudoClass() {
    FxTestSupport.runAndWait(() -> {
      Input input = new Input();
      SimpleBooleanProperty invalid = new SimpleBooleanProperty();
      input.invalidProperty().bind(invalid);

      invalid.set(true);
      assertTrue(input.isInvalid());
      assertTrue(input.getPseudoClassStates().contains(INVALID));

      invalid.set(false);
      assertFalse(input.isInvalid());
      assertFalse(input.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void resolvesShadcnDimensionsAndFoundationStylesFromCss() {
    FxTestSupport.runAndWait(() -> {
      Input input = new Input();
      StackPane root = themedRoot(input, AppTheme.LIGHT);

      root.applyCss();

      assertEquals(32.0, input.prefHeight(-1.0));
      assertEquals(0.0, input.minWidth(-1.0));
      assertEquals(14.0, input.getFont().getSize());
      assertEquals(Pos.CENTER_LEFT, input.getAlignment());
      assertEquals(Color.TRANSPARENT, input.getBackground().getFills().getFirst().getFill());
      assertEquals(Color.web("#e5e5e5"), input.getBorder().getStrokes().getFirst().getTopStroke());
    });
  }

  @Test
  void mapsDisabledAndDarkBackgrounds() {
    FxTestSupport.runAndWait(() -> {
      Input lightInput = new Input();
      StackPane lightRoot = themedRoot(lightInput, AppTheme.LIGHT);
      lightInput.setDisable(true);
      lightRoot.applyCss();

      assertEquals(Color.web("rgba(229, 229, 229, 0.5)"), background(lightInput));
      assertEquals(0.5, lightInput.getOpacity());

      Input darkInput = new Input();
      StackPane darkRoot = themedRoot(darkInput, AppTheme.DARK);
      darkRoot.applyCss();
      assertEquals(Color.web("rgba(255, 255, 255, 0.045)"), background(darkInput));

      darkInput.setDisable(true);
      darkRoot.applyCss();
      assertEquals(Color.web("rgba(255, 255, 255, 0.12)"), background(darkInput));
    });
  }

  @Test
  void rendersShadcnRingWheneverTheInputIsFocused() {
    FxTestSupport.runAndWait(() -> {
      Input input = new Input();
      StackPane root = themedRoot(input, AppTheme.LIGHT);
      input.pseudoClassStateChanged(PseudoClass.getPseudoClass("focused"), true);

      root.applyCss();

      assertEquals(2, input.getBorder().getStrokes().size());
      assertEquals(Color.web("#a1a1a1"), input.getBorder().getStrokes().get(0).getTopStroke());
      assertEquals(Color.web("rgba(161, 161, 161, 0.5)"), input.getBorder().getStrokes().get(1).getTopStroke());
      assertEquals(1.0, input.getBorder().getStrokes().get(0).getWidths().getTop());
      assertEquals(3.0, input.getBorder().getStrokes().get(1).getWidths().getTop());
      assertEquals(-3.0, input.getBorder().getStrokes().get(1).getInsets().getTop());
      assertNull(input.getEffect());
    });
  }

  @Test
  void resolvesInvalidBordersForBothThemes() {
    FxTestSupport.runAndWait(() -> {
      assertEquals(Color.web("#e7000b"), invalidBorder(AppTheme.LIGHT));
      assertEquals(Color.web("rgba(255, 100, 103, 0.5)"), invalidBorder(AppTheme.DARK));
    });
  }

  private static Color invalidBorder(AppTheme theme) {
    Input input = new Input();
    StackPane root = themedRoot(input, theme);
    input.setInvalid(true);
    root.applyCss();
    return (Color) input.getBorder().getStrokes().getFirst().getTopStroke();
  }

  private static Color background(Input input) {
    return (Color) input.getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(Input input, AppTheme theme) {
    StackPane root = new StackPane(input);
    new ThemeManager(new Scene(root), theme);
    return root;
  }
}
