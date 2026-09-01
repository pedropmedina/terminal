package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Toggle.Size;
import com.acteque.terminal.ui.core.Toggle.Variant;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

class ToggleTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  @Test
  void defaultsToTheDefaultVariantAndSize() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle("Bold");

      assertEquals(Variant.DEFAULT, toggle.getVariant());
      assertEquals(Size.DEFAULT, toggle.getSize());
      assertTrue(toggle.getStyleClass().contains("toggle-button"));
      assertTrue(toggle.getStyleClass().contains("core-toggle"));
      assertTrue(toggle.getStyleClass().contains("toggle-variant-default"));
      assertTrue(toggle.getStyleClass().contains("toggle-size-default"));
      assertFalse(toggle.isSelected());
      assertFalse(toggle.isInvalid());
    });
  }

  @Test
  void preservesJavaFxGraphicActionAndToggleBehavior() {
    FxTestSupport.runAndWait(() -> {
      Circle graphic = new Circle(4.0);
      Toggle toggle = new Toggle("Italic", graphic, Variant.OUTLINE, Size.SM);
      AtomicBoolean fired = new AtomicBoolean();
      toggle.setOnAction(ignored -> fired.set(true));

      toggle.fire();

      assertSame(graphic, toggle.getGraphic());
      assertEquals("Italic", toggle.getText());
      assertTrue(toggle.isSelected());
      assertTrue(fired.get());
    });
  }

  @Test
  void keepsExactlyOneVariantAndSizeStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle();

      for (Variant variant : Variant.values()) {
        toggle.setVariant(variant);
        assertEquals(1, countClassesWithPrefix(toggle, "toggle-variant-"));
        assertTrue(toggle.getStyleClass().contains(styleClass(variant)));
      }

      for (Size size : Size.values()) {
        toggle.setSize(size);
        assertEquals(1, countClassesWithPrefix(toggle, "toggle-size-"));
        assertTrue(toggle.getStyleClass().contains(styleClass(size)));
      }
    });
  }

  @Test
  void supportsBindingVariantSizeAndInvalidState() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle();
      SimpleObjectProperty<Variant> variant = new SimpleObjectProperty<>(Variant.DEFAULT);
      SimpleObjectProperty<Size> size = new SimpleObjectProperty<>(Size.DEFAULT);
      SimpleBooleanProperty invalid = new SimpleBooleanProperty();
      toggle.variantProperty().bind(variant);
      toggle.sizeProperty().bind(size);
      toggle.invalidProperty().bind(invalid);

      variant.set(Variant.OUTLINE);
      size.set(Size.LG);
      invalid.set(true);

      assertEquals(Variant.OUTLINE, toggle.getVariant());
      assertEquals(Size.LG, toggle.getSize());
      assertTrue(toggle.getStyleClass().contains("toggle-variant-outline"));
      assertTrue(toggle.getStyleClass().contains("toggle-size-lg"));
      assertTrue(toggle.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void rejectsNullVariantAndSize() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle();

      assertThrows(NullPointerException.class, () -> toggle.setVariant(null));
      assertThrows(NullPointerException.class, () -> toggle.setSize(null));
      assertEquals(Variant.DEFAULT, toggle.getVariant());
      assertEquals(Size.DEFAULT, toggle.getSize());
    });
  }

  @Test
  void resolvesShadcnDimensionsAndVariantColorsFromCss() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle("B");
      StackPane root = themedRoot(toggle, AppTheme.LIGHT);

      assertDimensions(root, toggle, Size.SM, 28.0, 28.0);
      assertDimensions(root, toggle, Size.DEFAULT, 32.0, 32.0);
      assertDimensions(root, toggle, Size.LG, 36.0, 36.0);

      toggle.setVariant(Variant.OUTLINE);
      root.applyCss();
      assertEquals(Color.web("#e5e5e5"), toggle.getBorder().getStrokes().getFirst().getTopStroke());

      toggle.setSelected(true);
      root.applyCss();
      assertEquals(Color.web("#f5f5f5"), background(toggle));
    });
  }

  @Test
  void mapsDarkSelectedAndInvalidColors() {
    FxTestSupport.runAndWait(() -> {
      Toggle toggle = new Toggle();
      StackPane root = themedRoot(toggle, AppTheme.DARK);
      toggle.setSelected(true);
      toggle.setInvalid(true);
      root.applyCss();

      assertEquals(Color.web("#262626"), background(toggle));
      assertEquals(Color.web("rgba(255, 100, 103, 0.5)"), toggle.getBorder().getStrokes().getFirst().getTopStroke());
    });
  }

  private static long countClassesWithPrefix(Toggle toggle, String prefix) {
    return toggle
      .getStyleClass()
      .stream()
      .filter(style -> style.startsWith(prefix))
      .count();
  }

  private static String styleClass(Enum<?> value) {
    String prefix = value instanceof Variant ? "toggle-variant-" : "toggle-size-";
    return prefix + value.name().toLowerCase(Locale.ROOT).replace('_', '-');
  }

  private static void assertDimensions(StackPane root, Toggle toggle, Size size, double height, double minWidth) {
    toggle.setSize(size);
    root.applyCss();
    assertEquals(height, toggle.prefHeight(-1.0));
    assertEquals(minWidth, toggle.minWidth(-1.0));
  }

  private static Color background(Toggle toggle) {
    return (Color) toggle.getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(Toggle toggle, AppTheme theme) {
    StackPane root = new StackPane(toggle);
    new ThemeManager(new Scene(root), theme);
    return root;
  }
}
