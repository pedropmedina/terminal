package com.acteque.terminal.ui.core.togglegroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Toggle.Size;
import com.acteque.terminal.ui.core.Toggle.Variant;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

class ToggleGroupTest {

  @Test
  void defaultsToShadcnPropertiesAndPropagatesThemToItems() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroupItem first = new ToggleGroupItem("Bold");
      ToggleGroupItem second = new ToggleGroupItem("Italic");
      ToggleGroup group = new ToggleGroup(first, second);

      assertEquals(Orientation.HORIZONTAL, group.getOrientation());
      assertEquals(Variant.DEFAULT, group.getVariant());
      assertEquals(Size.DEFAULT, group.getSize());
      assertEquals(8.0, group.getSpacing());
      assertFalse(group.isMultiple());
      assertEquals(AccessibleRole.PARENT, group.getAccessibleRole());
      assertTrue(group.getStyleClass().contains("core-toggle-group"));
      assertTrue(group.getStyleClass().contains("toggle-group-variant-default"));
      assertTrue(group.getStyleClass().contains("toggle-group-size-default"));
      assertTrue(first.getStyleClass().contains("toggle-group-first"));
      assertTrue(second.getStyleClass().contains("toggle-group-last"));

      group.setVariant(Variant.OUTLINE);
      group.setSize(Size.LG);

      assertEquals(Variant.OUTLINE, first.getVariant());
      assertEquals(Variant.OUTLINE, second.getVariant());
      assertEquals(Size.LG, first.getSize());
      assertEquals(Size.LG, second.getSize());
      assertTrue(group.getStyleClass().contains("toggle-group-variant-outline"));
      assertTrue(group.getStyleClass().contains("toggle-group-size-lg"));
    });
  }

  @Test
  void providesExclusiveAndMultipleSelectionModes() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroupItem first = new ToggleGroupItem("Bold");
      ToggleGroupItem second = new ToggleGroupItem("Italic");
      ToggleGroup group = new ToggleGroup(first, second);

      first.fire();
      second.fire();

      assertFalse(first.isSelected());
      assertTrue(second.isSelected());

      group.setMultiple(true);
      first.fire();
      second.fire();

      assertTrue(first.isSelected());
      assertTrue(second.isSelected());
      assertNull(first.getToggleGroup());
      assertNull(second.getToggleGroup());
    });
  }

  @Test
  void laysOutItemsUsingMappedSpacingAndOrientation() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroupItem first = new ToggleGroupItem("One");
      ToggleGroupItem second = new ToggleGroupItem("Longer label");
      ToggleGroup group = new ToggleGroup(first, second);
      StackPane root = themedRoot(group);
      root.applyCss();
      root.layout();

      assertEquals(8.0, second.getLayoutX() - first.getLayoutX() - first.getWidth(), 0.5);

      group.setOrientation(Orientation.VERTICAL);
      root.layout();

      assertEquals(8.0, second.getLayoutY() - first.getLayoutY() - first.getHeight(), 0.5);
      assertEquals(first.getWidth(), second.getWidth(), 0.5);
    });
  }

  @Test
  void supportsBoundSpacingWithoutBeingOverwrittenByCss() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroup group = new ToggleGroup(new ToggleGroupItem("One"), new ToggleGroupItem("Two"));
      SimpleDoubleProperty spacing = new SimpleDoubleProperty(4.0);
      group.spacingProperty().bind(spacing);
      StackPane root = themedRoot(group);
      root.applyCss();

      assertEquals(4.0, group.getSpacing());

      spacing.set(0.0);

      assertTrue(group.getStyleClass().contains("toggle-group-spacing-zero"));
    });
  }

  @Test
  void joinsZeroSpacedOutlineItemsWithShadcnRadiiAndBorders() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroupItem first = new ToggleGroupItem("One");
      ToggleGroupItem second = new ToggleGroupItem("Two");
      ToggleGroup group = new ToggleGroup(first, second);
      group.setVariant(Variant.OUTLINE);
      group.setSize(Size.SM);
      group.setSpacing(0.0);
      StackPane root = themedRoot(group);
      root.applyCss();

      assertTrue(group.getStyleClass().contains("toggle-group-spacing-zero"));
      assertEquals(new Insets(0.0, 8.0, 0.0, 8.0), first.getPadding());
      assertEquals(6.0, first.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(0.0, first.getBackground().getFills().getFirst().getRadii().getTopRightHorizontalRadius());
      assertEquals(0.0, second.getBorder().getStrokes().getFirst().getWidths().getLeft());
    });
  }

  @Test
  void refreshesClassesAndSelectionOwnershipWhenItemsChange() {
    FxTestSupport.runAndWait(() -> {
      ToggleGroupItem first = new ToggleGroupItem("One");
      ToggleGroupItem second = new ToggleGroupItem("Two");
      ToggleGroup group = new ToggleGroup(first, second);

      group.getChildren().remove(first);

      assertFalse(first.getStyleClass().contains("toggle-group-item"));
      assertNull(first.getToggleGroup());
      assertTrue(second.getStyleClass().contains("toggle-group-only"));
      assertThrows(IllegalArgumentException.class, () -> group.setSpacing(-1.0));
      assertThrows(NullPointerException.class, () -> group.setOrientation(null));
    });
  }

  private static StackPane themedRoot(ToggleGroup group) {
    StackPane root = new StackPane(group);
    new ThemeManager(new Scene(root, 320, 200), AppTheme.LIGHT);
    return root;
  }
}
