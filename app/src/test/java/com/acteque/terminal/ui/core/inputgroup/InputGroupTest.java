package com.acteque.terminal.ui.core.inputgroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.Textarea;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class InputGroupTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass CONTAINS_DISABLED = PseudoClass.getPseudoClass("contains-disabled");

  @Test
  void specializesTheExistingCoreInputAndTextareaControls() {
    FxTestSupport.runAndWait(() -> {
      InputGroupInput input = new InputGroupInput("AAPL");
      InputGroupTextarea textarea = new InputGroupTextarea("Market notes");

      assertTrue(input instanceof Input);
      assertTrue(textarea instanceof Textarea);
      assertEquals("AAPL", input.getText());
      assertEquals("Market notes", textarea.getText());
      assertTrue(textarea.isWrapText());
      assertEquals(2, textarea.getPrefRowCount());
      assertTrue(input.getStyleClass().contains("input-group-control"));
      assertTrue(input.getStyleClass().contains("input-group-input"));
      assertTrue(textarea.getStyleClass().contains("input-group-control"));
      assertTrue(textarea.getStyleClass().contains("input-group-textarea"));

      textarea.setInvalid(true);
      assertTrue(textarea.isInvalid());
      assertTrue(textarea.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void configuresAddonButtonAndTextPrimitives() {
    FxTestSupport.runAndWait(() -> {
      InputGroupButton button = new InputGroupButton("Clear");
      InputGroupButton iconButton = new InputGroupButton(new LucideIcon(LucideIcons.PLUS));
      InputGroupText text = new InputGroupText("USD");
      InputGroupAddon addon = new InputGroupAddon(InputGroupAlignment.INLINE_END, text, button, iconButton);
      StackPane root = themedRoot(new InputGroup(addon, new InputGroupInput()), AppTheme.LIGHT);
      root.applyCss();

      assertEquals(Button.Variant.GHOST, button.getVariant());
      assertEquals(Button.Size.XS, button.getSize());
      assertEquals(14.0, button.getFont().getSize());
      assertEquals(24.0, button.prefHeight(-1));
      assertEquals(new Insets(0.0, 6.0, 0.0, 6.0), button.getPadding());
      assertEquals(5.0, button.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Button.Size.ICON_XS, iconButton.getSize());
      assertEquals(Color.web("#737373"), iconButton.getTextFill());
      assertEquals(24.0, iconButton.prefWidth(-1));
      assertEquals(24.0, iconButton.prefHeight(-1));
      assertEquals(Insets.EMPTY, iconButton.getPadding());
      assertEquals(5.0, iconButton.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertTrue(button.getStyleClass().contains("core-input-group-button"));
      assertTrue(text.isMouseTransparent());
      assertSame(InputGroupAlignment.INLINE_END, addon.getAlignmentPosition());
      assertTrue(addon.getStyleClass().contains("input-group-addon-inline-end"));
    });
  }

  @Test
  void derivesStructureInvalidAndDisabledStateFromItsChildren() {
    FxTestSupport.runAndWait(() -> {
      InputGroupInput input = new InputGroupInput();
      InputGroupButton button = new InputGroupButton("Clear");
      InputGroupAddon addon = new InputGroupAddon(button);
      InputGroup group = new InputGroup(addon, input);

      assertSame(input, group.getControl());
      assertTrue(group.getStyleClass().contains("with-input"));
      assertTrue(group.getStyleClass().contains("has-inline-start"));
      assertFalse(group.getPseudoClassStates().contains(INVALID));
      assertFalse(group.getPseudoClassStates().contains(CONTAINS_DISABLED));

      input.setInvalid(true);
      assertTrue(group.getPseudoClassStates().contains(INVALID));

      button.setDisable(true);
      assertTrue(group.getPseudoClassStates().contains(CONTAINS_DISABLED));

      addon.setAlignmentPosition(InputGroupAlignment.BLOCK_END);
      assertFalse(group.getStyleClass().contains("has-inline-start"));
      assertTrue(group.getStyleClass().contains("has-block-end"));

      group.getChildren().remove(input);
      assertNull(group.getControl());
      assertFalse(group.getStyleClass().contains("with-input"));
      assertFalse(group.getPseudoClassStates().contains(INVALID));

      input.setInvalid(false);
      input.setInvalid(true);
      assertFalse(group.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void laysOutInlineAddonsAroundAFlexibleInputAtTheShadcnHeight() {
    FxTestSupport.runAndWait(() -> {
      InputGroupAddon start = new InputGroupAddon(new Label("$"));
      InputGroupInput input = new InputGroupInput();
      InputGroupAddon end = new InputGroupAddon(InputGroupAlignment.INLINE_END, new Label("USD"));
      InputGroup group = new InputGroup(start, input, end);
      StackPane root = themedRoot(group, AppTheme.LIGHT);

      root.applyCss();
      double height = group.prefHeight(300.0);
      group.resize(300.0, height);
      group.layout();

      assertEquals(32.0, height);
      assertEquals(height, group.maxHeight(300.0));
      assertEquals(1.0, start.getLayoutX());
      assertEquals(start.getLayoutX() + start.getWidth(), input.getLayoutX(), 0.01);
      assertEquals(end.getLayoutX(), input.getLayoutX() + input.getWidth(), 0.01);
      assertEquals(299.0, end.getLayoutX() + end.getWidth(), 0.01);
      assertTrue(input.getWidth() > start.getWidth());
      assertEquals(new Insets(4.0, 6.0, 4.0, 6.0), input.getPadding());
      assertEquals(Color.TRANSPARENT, background(input));
      assertEquals(Color.TRANSPARENT, input.getBorder().getStrokes().getFirst().getTopStroke());
      assertEquals(0.0, input.getBorder().getStrokes().getFirst().getWidths().getTop());
    });
  }

  @Test
  void stacksBlockAddonsAndLetsTextareaDetermineTheGroupHeight() {
    FxTestSupport.runAndWait(() -> {
      InputGroupAddon start = new InputGroupAddon(InputGroupAlignment.BLOCK_START, new Label("Notes"));
      InputGroupTextarea textarea = new InputGroupTextarea();
      InputGroupAddon end = new InputGroupAddon(InputGroupAlignment.BLOCK_END, new Label("Optional"));
      InputGroup group = new InputGroup(start, textarea, end);
      StackPane root = themedRoot(group, AppTheme.LIGHT);

      root.applyCss();
      double height = group.prefHeight(320.0);
      group.resize(320.0, height);
      group.layout();

      assertTrue(height > textarea.minHeight(318.0));
      assertEquals(Double.MAX_VALUE, group.maxHeight(320.0));
      assertEquals(1.0, start.getLayoutY());
      assertEquals(start.getLayoutY() + start.getHeight(), textarea.getLayoutY(), 0.01);
      assertEquals(end.getLayoutY(), textarea.getLayoutY() + textarea.getHeight(), 0.05);
      assertEquals(318.0, start.getWidth());
      assertEquals(318.0, end.getWidth());
      assertTrue(group.getStyleClass().contains("with-textarea"));
      assertEquals(Color.TRANSPARENT, textarea.getBorder().getStrokes().getFirst().getTopStroke());
      assertEquals(0.0, textarea.getBorder().getStrokes().getFirst().getWidths().getTop());
    });
  }

  @Test
  void rendersFocusInvalidAndDisabledStylesOnTheGroupBoundary() {
    FxTestSupport.runAndWait(() -> {
      InputGroupInput input = new InputGroupInput();
      InputGroup group = new InputGroup(input);
      StackPane root = themedRoot(group, AppTheme.LIGHT);

      group.pseudoClassStateChanged(PseudoClass.getPseudoClass("control-focus-visible"), true);
      root.applyCss();
      assertEquals(2, group.getBorder().getStrokes().size());
      assertEquals(Color.web("#a1a1a1"), group.getBorder().getStrokes().get(0).getTopStroke());
      assertEquals(Color.web("rgba(161, 161, 161, 0.5)"), group.getBorder().getStrokes().get(1).getTopStroke());

      input.setInvalid(true);
      root.applyCss();
      assertEquals(Color.web("#e7000b"), group.getBorder().getStrokes().get(0).getTopStroke());
      assertEquals(Color.web("rgba(231, 0, 11, 0.2)"), group.getBorder().getStrokes().get(1).getTopStroke());

      input.setDisable(true);
      root.applyCss();
      assertEquals(Color.web("rgba(229, 229, 229, 0.5)"), background(group));
      assertEquals(0.5, group.getOpacity());
      assertEquals(1.0, input.getOpacity());
    });
  }

  @Test
  void resolvesDarkTextareaGroupBackgroundAndInvalidColors() {
    FxTestSupport.runAndWait(() -> {
      InputGroupTextarea textarea = new InputGroupTextarea();
      InputGroup group = new InputGroup(textarea);
      StackPane root = themedRoot(group, AppTheme.DARK);

      root.applyCss();
      assertEquals(Color.web("rgba(255, 255, 255, 0.045)"), background(group));

      textarea.setInvalid(true);
      root.applyCss();
      assertEquals(Color.web("rgba(255, 100, 103, 0.5)"), group.getBorder().getStrokes().get(0).getTopStroke());
      assertEquals(Color.web("rgba(255, 100, 103, 0.4)"), group.getBorder().getStrokes().get(1).getTopStroke());
    });
  }

  private static Color background(Node node) {
    return (Color) ((javafx.scene.layout.Region) node).getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(InputGroup group, AppTheme theme) {
    StackPane root = new StackPane(group);
    new ThemeManager(new Scene(root), theme);
    return root;
  }
}
