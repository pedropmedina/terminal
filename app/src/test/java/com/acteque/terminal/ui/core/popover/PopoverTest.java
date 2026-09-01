package com.acteque.terminal.ui.core.popover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class PopoverTest {

  @Test
  void composesThePopoverComponentFamily() {
    FxTestSupport.runAndWait(() -> {
      PopoverTitle title = new PopoverTitle("Dimensions");
      PopoverDescription description = new PopoverDescription("Set the dimensions for the layer.");
      PopoverHeader header = new PopoverHeader(title, description);
      PopoverContent content = new PopoverContent(header);
      Popover popover = new Popover(content);
      PopoverTrigger trigger = new PopoverTrigger("Open", popover);

      assertSame(content, popover.getContent());
      assertSame(content, popover.getPopup().getContent().getFirst());
      assertSame(popover, trigger.getPopover());
      assertTrue(content.getStyleClass().contains("core-popover-content"));
      assertTrue(header.getStyleClass().contains("core-popover-header"));
      assertTrue(title.getStyleClass().contains("core-popover-title"));
      assertTrue(description.getStyleClass().contains("core-popover-description"));
    });
  }

  @Test
  void defaultsMatchTheProvidedPositionerProps() {
    FxTestSupport.runAndWait(() -> {
      PopoverContent content = new PopoverContent();

      assertEquals(PopoverContent.Align.CENTER, content.getAlign());
      assertEquals(PopoverContent.Side.BOTTOM, content.getSide());
      assertEquals(0.0, content.getAlignOffset());
      assertEquals(4.0, content.getSideOffset());
    });
  }

  @Test
  void triggerTogglesLogicalOpenStateBeforeItsWindowIsShown() {
    FxTestSupport.runAndWait(() -> {
      Popover popover = new Popover(new PopoverContent());
      PopoverTrigger trigger = new PopoverTrigger("Open", popover);
      new Scene(new StackPane(trigger));

      trigger.fire();
      assertTrue(popover.isOpen());

      trigger.fire();
      assertFalse(popover.isOpen());
    });
  }

  @Test
  void mapsTheProvidedSizingSpacingAndColorsToJavaFxCss() {
    FxTestSupport.runAndWait(() -> {
      PopoverTitle title = new PopoverTitle("Title");
      PopoverDescription description = new PopoverDescription("Description");
      PopoverHeader header = new PopoverHeader(title, description);
      PopoverContent content = new PopoverContent(header);
      StackPane root = new StackPane(content);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      root.applyCss();

      assertEquals(288.0, content.getPrefWidth());
      assertEquals(10.0, content.getSpacing());
      assertEquals(new Insets(10.0), content.getPadding());
      assertEquals(8.0, content.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.web("#ffffff"), content.getBackground().getFills().getFirst().getFill());
      assertEquals(2.0, header.getSpacing());
      assertEquals(14.0, title.getFont().getSize());
      assertEquals(Color.web("#737373"), description.getTextFill());
    });
  }
}
