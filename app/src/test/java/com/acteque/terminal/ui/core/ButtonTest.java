package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

class ButtonTest {

  @Test
  void defaultsToThePrimaryDefaultSize() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("Save");

      assertEquals(Variant.DEFAULT, button.getVariant());
      assertEquals(Size.DEFAULT, button.getSize());
      assertTrue(button.getStyleClass().contains("core-button"));
      assertTrue(button.getStyleClass().contains("button-variant-default"));
      assertTrue(button.getStyleClass().contains("button-size-default"));
    });
  }

  @Test
  void preservesTheJavaFxGraphicAndActionBehavior() {
    FxTestSupport.runAndWait(() -> {
      Circle graphic = new Circle(4.0);
      Button button = new Button("Delete", graphic, Variant.DESTRUCTIVE, Size.SM);
      AtomicBoolean fired = new AtomicBoolean();
      button.setOnAction(ignored -> fired.set(true));

      button.fire();

      assertSame(graphic, button.getGraphic());
      assertEquals("Delete", button.getText());
      assertEquals(Variant.DESTRUCTIVE, button.getVariant());
      assertEquals(Size.SM, button.getSize());
      assertTrue(fired.get());
    });
  }

  @Test
  void keepsExactlyOneVariantAndSizeStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button();

      for (Variant variant : Variant.values()) {
        button.setVariant(variant);
        assertEquals(variant, button.getVariant());
        assertEquals(
          1,
          button
            .getStyleClass()
            .stream()
            .filter(style -> style.startsWith("button-variant-"))
            .count()
        );
        assertTrue(button.getStyleClass().contains(styleClass(variant)));
      }

      for (Size size : Size.values()) {
        button.setSize(size);
        assertEquals(size, button.getSize());
        assertEquals(
          1,
          button
            .getStyleClass()
            .stream()
            .filter(style -> style.startsWith("button-size-"))
            .count()
        );
        assertTrue(button.getStyleClass().contains(styleClass(size)));
      }
    });
  }

  @Test
  void supportsBindingVariantAndSize() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button();
      SimpleObjectProperty<Variant> variant = new SimpleObjectProperty<>(Variant.OUTLINE);
      SimpleObjectProperty<Size> size = new SimpleObjectProperty<>(Size.XS);
      button.variantProperty().bind(variant);
      button.sizeProperty().bind(size);

      variant.set(Variant.GHOST);
      size.set(Size.ICON_LG);

      assertEquals(Variant.GHOST, button.getVariant());
      assertEquals(Size.ICON_LG, button.getSize());
      assertTrue(button.getStyleClass().contains("button-variant-ghost"));
      assertTrue(button.getStyleClass().contains("button-size-icon-lg"));
      assertFalse(button.getStyleClass().contains("button-variant-outline"));
      assertFalse(button.getStyleClass().contains("button-size-xs"));
    });
  }

  @Test
  void rejectsNullVariantAndSize() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button();

      assertThrows(NullPointerException.class, () -> button.setVariant(null));
      assertThrows(NullPointerException.class, () -> button.setSize(null));
      assertEquals(Variant.DEFAULT, button.getVariant());
      assertEquals(Size.DEFAULT, button.getSize());
    });
  }

  @Test
  void resolvesVariantColorsFromTheActiveTheme() {
    FxTestSupport.runAndWait(() -> {
      assertEquals(Color.web("#171717"), resolvedBackground(AppTheme.LIGHT));
      assertEquals(Color.web("#e5e5e5"), resolvedBackground(AppTheme.DARK));
    });
  }

  @Test
  void doesNotTransitionTheHoverBackground() throws IOException {
    String stylesheet;
    try (var input = Objects.requireNonNull(Button.class.getResourceAsStream("button.css"))) {
      stylesheet = new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }

    Pattern backgroundTransition = Pattern.compile("transition-property\\s*:[^;]*-fx-background-color", Pattern.DOTALL);
    assertFalse(backgroundTransition.matcher(stylesheet).find());
  }

  @Test
  void appliesTextAndIconDimensionsFromCss() {
    FxTestSupport.runAndWait(() -> {
      Button button = new Button("B");
      StackPane root = new StackPane(button);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      assertDimensions(root, button, Size.XS, 24.0, null);
      assertDimensions(root, button, Size.SM, 28.0, null);
      assertDimensions(root, button, Size.DEFAULT, 32.0, null);
      assertDimensions(root, button, Size.LG, 36.0, null);
      assertDimensions(root, button, Size.ICON_XS, 24.0, 24.0);
      assertDimensions(root, button, Size.ICON_SM, 28.0, 28.0);
      assertDimensions(root, button, Size.ICON, 32.0, 32.0);
      assertDimensions(root, button, Size.ICON_LG, 36.0, 36.0);
    });
  }

  private static Color resolvedBackground(AppTheme theme) {
    Button button = new Button("Save");
    StackPane root = new StackPane(button);
    new ThemeManager(new Scene(root), theme);
    root.applyCss();
    return (Color) button.getBackground().getFills().getFirst().getFill();
  }

  private static void assertDimensions(StackPane root, Button button, Size size, double height, Double width) {
    button.setSize(size);
    root.applyCss();
    assertEquals(height, button.prefHeight(-1.0));
    if (width != null) {
      assertEquals(width, button.prefWidth(-1.0));
    }
  }

  private static String styleClass(Enum<?> value) {
    String prefix = value instanceof Variant ? "button-variant-" : "button-size-";
    return prefix + value.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }
}
