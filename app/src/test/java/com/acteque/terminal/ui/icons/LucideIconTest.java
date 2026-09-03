package com.acteque.terminal.ui.icons;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import org.junit.jupiter.api.Test;

class LucideIconTest {

  @Test
  void createsOneFreshPathForEachIcon() {
    FxTestSupport.runAndWait(() -> {
      LucideIcon first = new LucideIcon(LucideIcons.SEARCH);
      LucideIcon second = new LucideIcon(LucideIcons.SEARCH);
      StackPane root = new StackPane(first, second);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);
      root.applyCss();

      SVGPath firstPath = path(first);
      SVGPath secondPath = path(second);
      assertNotSame(firstPath, secondPath);
      assertEquals(LucideIcons.SEARCH.pathData(), firstPath.getContent());
      assertSame(LucideIcons.SEARCH, first.getGlyph());
      assertTrue(first.isMouseTransparent());
      assertTrue(firstPath.isMouseTransparent());
    });
  }

  @Test
  void sizesSquareAndNonSquareGlyphsByTheirLargestDimension() {
    FxTestSupport.runAndWait(() -> {
      LucideIcon square = new LucideIcon(LucideIcons.X, 20.0);
      LucideIcon wide = new LucideIcon(new TestGlyph(32.0, 16.0), 20.0);

      assertEquals(20.0, square.prefWidth(-1.0));
      assertEquals(20.0, square.prefHeight(-1.0));
      assertEquals(20.0, wide.prefWidth(-1.0));
      assertEquals(10.0, wide.prefHeight(-1.0));

      square.setIconSize(24.0);
      assertEquals(24.0, square.prefWidth(-1.0));
      assertEquals(24.0, square.prefHeight(-1.0));
    });
  }

  @Test
  void rejectsInvalidGeometryAndSizes() {
    FxTestSupport.runAndWait(() -> {
      assertThrows(NullPointerException.class, () -> new LucideIcon(null));
      assertThrows(IllegalArgumentException.class, () -> new LucideIcon(LucideIcons.PLUS, 0.0));
      assertThrows(IllegalArgumentException.class, () -> new LucideIcon(new TestGlyph(0.0, 24.0)));
      assertThrows(IllegalArgumentException.class, () -> new LucideIcon(new TestGlyph(24.0, 24.0, " ")));

      LucideIcon icon = new LucideIcon(LucideIcons.PLUS);
      assertThrows(IllegalArgumentException.class, () -> icon.setIconSize(Double.NaN));
      assertEquals(LucideIcon.DEFAULT_SIZE, icon.getIconSize());
    });
  }

  @Test
  void resolvesStrokePresentationFromButtonCss() {
    FxTestSupport.runAndWait(() -> {
      LucideIcon icon = new LucideIcon(LucideIcons.PLUS);
      Button button = new Button(null, icon, Button.Variant.DEFAULT, Button.Size.ICON);
      StackPane root = new StackPane(button);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);
      root.applyCss();

      SVGPath path = path(icon);
      assertEquals(Color.TRANSPARENT, path.getFill());
      assertEquals(Color.web("#fafafa"), path.getStroke());
      assertEquals(2.0, path.getStrokeWidth());
    });
  }

  @Test
  void generatedCatalogContainsValidGeometry() {
    for (LucideIcons icon : LucideIcons.values()) {
      assertFalse(icon.sourceName().isBlank());
      assertTrue(icon.width() > 0.0);
      assertTrue(icon.height() > 0.0);
      assertFalse(icon.pathData().isBlank());
    }
    assertEquals("M0 0M18 6 6 18 M0 0m6 6 12 12", LucideIcons.X.pathData());
  }

  private static SVGPath path(LucideIcon icon) {
    return (SVGPath) icon.lookup(".lucide-icon-path");
  }

  private record TestGlyph(double width, double height, String pathData) implements LucideGlyph {
    private TestGlyph(double width, double height) {
      this(width, height, "M0 0H1");
    }

    @Override
    public String sourceName() {
      return "test";
    }
  }
}
