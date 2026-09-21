package com.acteque.terminal.ui.kbd;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.tooltip.TooltipContent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class KbdTest {

  @Test
  void configuresTextGraphicAndNonInteractiveDefaults() {
    FxTestSupport.runAndWait(() -> {
      LucideIcon icon = new LucideIcon(LucideIcons.PLUS);
      Kbd kbd = new Kbd("K", icon);

      assertEquals("K", kbd.getText());
      assertSame(icon, kbd.getGraphic());
      assertTrue(kbd.getStyleClass().contains("core-kbd"));
      assertTrue(kbd.isMouseTransparent());
      assertFalse(kbd.isFocusTraversable());
      assertEquals(kbd.prefWidth(-1.0), kbd.maxWidth(-1.0));
    });
  }

  @Test
  void resolvesShadcnDimensionsTypographyAndLightThemeColors() {
    FxTestSupport.runAndWait(() -> {
      LucideIcon icon = new LucideIcon(LucideIcons.PLUS);
      Kbd kbd = new Kbd("K", icon);
      StackPane root = themedRoot(kbd, AppTheme.LIGHT);

      root.applyCss();

      assertEquals(20.0, kbd.minWidth(-1.0));
      assertEquals(20.0, kbd.prefHeight(-1.0));
      assertEquals(20.0, kbd.maxHeight(-1.0));
      assertEquals(new Insets(0.0, 4.0, 0.0, 4.0), kbd.getPadding());
      assertEquals(4.0, kbd.getGraphicTextGap());
      assertEquals(12.0, kbd.getFont().getSize());
      assertEquals(4.0, kbd.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.web("#f5f5f5"), background(kbd));
      assertEquals(Color.web("#737373"), kbd.getTextFill());
      assertEquals(12.0, icon.prefWidth(-1.0));
      assertEquals(12.0, icon.prefHeight(-1.0));
    });
  }

  @Test
  void resolvesDarkThemeColors() {
    FxTestSupport.runAndWait(() -> {
      Kbd kbd = new Kbd("Ctrl");
      StackPane root = themedRoot(kbd, AppTheme.DARK);

      root.applyCss();

      assertEquals(Color.web("#262626"), background(kbd));
      assertEquals(Color.web("#a1a1a1"), kbd.getTextFill());
    });
  }

  @Test
  void groupsKeysWithContentSizedCenteredSpacing() {
    FxTestSupport.runAndWait(() -> {
      Kbd first = new Kbd("Ctrl");
      Kbd second = new Kbd("B");
      KbdGroup group = new KbdGroup(first, second);
      StackPane root = themedRoot(group, AppTheme.LIGHT);

      root.applyCss();
      root.layout();

      assertEquals(2, group.getChildren().size());
      assertSame(first, group.getChildren().get(0));
      assertSame(second, group.getChildren().get(1));
      assertTrue(group.getStyleClass().contains("core-kbd-group"));
      assertEquals(Pos.CENTER, group.getAlignment());
      assertEquals(4.0, group.getSpacing());
      assertEquals(group.prefWidth(-1.0), group.getWidth(), 0.5);
      assertEquals(group.prefHeight(-1.0), group.getHeight(), 0.5);
    });
  }

  @Test
  void usesTooltipSpecificColorsInLightAndDarkThemes() {
    FxTestSupport.runAndWait(() -> {
      Kbd lightKbd = new Kbd("S");
      StackPane lightRoot = themedRoot(new TooltipContent(lightKbd), AppTheme.LIGHT);
      lightRoot.applyCss();

      assertEquals(Color.web("rgba(255, 255, 255, 0.2)"), background(lightKbd));
      assertEquals(Color.web("#ffffff"), lightKbd.getTextFill());

      Kbd darkKbd = new Kbd("S");
      StackPane darkRoot = themedRoot(new TooltipContent(darkKbd), AppTheme.DARK);
      darkRoot.applyCss();

      assertEquals(Color.web("rgba(10, 10, 10, 0.1)"), background(darkKbd));
      assertEquals(Color.web("#0a0a0a"), darkKbd.getTextFill());
    });
  }

  private static StackPane themedRoot(javafx.scene.Node child, AppTheme theme) {
    StackPane root = new StackPane(child);
    new ThemeManager(new Scene(root, 320, 200), theme);
    return root;
  }

  private static Color background(Kbd kbd) {
    return (Color) kbd.getBackground().getFills().getFirst().getFill();
  }
}
