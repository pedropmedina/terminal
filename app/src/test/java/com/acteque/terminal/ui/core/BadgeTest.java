package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Badge.Variant;
import java.util.Locale;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

class BadgeTest {

  private static final PseudoClass INTERACTIVE = PseudoClass.getPseudoClass("interactive");
  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass ICON_INLINE_START = PseudoClass.getPseudoClass("icon-inline-start");
  private static final PseudoClass ICON_INLINE_END = PseudoClass.getPseudoClass("icon-inline-end");

  @Test
  void defaultsToTheDefaultNonInteractiveVariant() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge("Badge");

      assertEquals(Variant.DEFAULT, badge.getVariant());
      assertTrue(badge.getStyleClass().contains("label"));
      assertTrue(badge.getStyleClass().contains("core-badge"));
      assertTrue(badge.getStyleClass().contains("badge-variant-default"));
      assertFalse(badge.isInteractive());
      assertFalse(badge.isFocusTraversable());
      assertFalse(badge.isInvalid());
    });
  }

  @Test
  void preservesTextAndGraphicAndTracksTheInlineIconSide() {
    FxTestSupport.runAndWait(() -> {
      Circle graphic = new Circle(4.0);
      Badge badge = new Badge("New", graphic, Variant.SECONDARY);

      assertEquals("New", badge.getText());
      assertSame(graphic, badge.getGraphic());
      assertTrue(badge.getPseudoClassStates().contains(ICON_INLINE_START));

      badge.setContentDisplay(ContentDisplay.RIGHT);

      assertFalse(badge.getPseudoClassStates().contains(ICON_INLINE_START));
      assertTrue(badge.getPseudoClassStates().contains(ICON_INLINE_END));
    });
  }

  @Test
  void keepsExactlyOneVariantStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge();

      for (Variant variant : Variant.values()) {
        badge.setVariant(variant);
        assertEquals(variant, badge.getVariant());
        assertEquals(
          1,
          badge
            .getStyleClass()
            .stream()
            .filter(style -> style.startsWith("badge-variant-"))
            .count()
        );
        assertTrue(badge.getStyleClass().contains(styleClass(variant)));
      }
    });
  }

  @Test
  void supportsBindingVariantInteractiveAndInvalidState() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge();
      SimpleObjectProperty<Variant> variant = new SimpleObjectProperty<>(Variant.DEFAULT);
      SimpleBooleanProperty interactive = new SimpleBooleanProperty();
      SimpleBooleanProperty invalid = new SimpleBooleanProperty();
      badge.variantProperty().bind(variant);
      badge.interactiveProperty().bind(interactive);
      badge.invalidProperty().bind(invalid);

      variant.set(Variant.OUTLINE);
      interactive.set(true);
      invalid.set(true);

      assertEquals(Variant.OUTLINE, badge.getVariant());
      assertTrue(badge.getPseudoClassStates().contains(INTERACTIVE));
      assertTrue(badge.getPseudoClassStates().contains(INVALID));
      assertTrue(badge.isFocusTraversable());
    });
  }

  @Test
  void rejectsNullVariant() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge();

      assertThrows(NullPointerException.class, () -> badge.setVariant(null));
      assertEquals(Variant.DEFAULT, badge.getVariant());
    });
  }

  @Test
  void resolvesShadcnDimensionsPaddingAndThemeColorsFromCss() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge("New");
      StackPane root = new StackPane(badge);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      root.applyCss();
      assertEquals(20.0, badge.prefHeight(-1.0));
      assertEquals(8.0, badge.getPadding().getLeft());
      assertEquals(8.0, badge.getPadding().getRight());
      assertEquals(4.0, badge.getGraphicTextGap());
      assertEquals(12.0, badge.getFont().getSize());
      assertEquals(Color.web("#171717"), background(badge));
      assertEquals(Color.web("#fafafa"), badge.getTextFill());

      badge.setGraphic(new Circle(4.0));
      root.applyCss();
      assertEquals(6.0, badge.getPadding().getLeft());
      assertEquals(8.0, badge.getPadding().getRight());

      badge.setVariant(Variant.DESTRUCTIVE);
      root.applyCss();
      assertEquals(Color.web("rgba(231, 0, 11, 0.1)"), background(badge));
      assertEquals(Color.web("#e7000b"), badge.getTextFill());
    });
  }

  @Test
  void mapsDarkDestructiveBackground() {
    FxTestSupport.runAndWait(() -> {
      Badge badge = new Badge("Error", Variant.DESTRUCTIVE);
      StackPane root = new StackPane(badge);
      new ThemeManager(new Scene(root), AppTheme.DARK);

      root.applyCss();

      assertEquals(Color.web("rgba(255, 100, 103, 0.2)"), background(badge));
      assertEquals(Color.web("#ff6467"), badge.getTextFill());
    });
  }

  private static String styleClass(Variant variant) {
    return "badge-variant-" + variant.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static Color background(Badge badge) {
    return (Color) badge.getBackground().getFills().getFirst().getFill();
  }
}
