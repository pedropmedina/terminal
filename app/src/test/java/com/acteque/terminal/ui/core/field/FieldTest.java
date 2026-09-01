package com.acteque.terminal.ui.core.field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class FieldTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");

  @Test
  void composesTheFieldComponentFamily() {
    FxTestSupport.runAndWait(() -> {
      FieldLabel label = new FieldLabel("Symbol");
      TextField input = new TextField();
      label.setLabelFor(input);
      FieldContent content = new FieldContent(label, new FieldDescription("Ticker symbol"));
      Field field = new Field(content, input);
      FieldGroup group = new FieldGroup(field, new FieldSeparator("or"));
      FieldSet set = new FieldSet(new FieldLegend("Instrument"), group);

      assertSame(input, label.getLabelFor());
      assertEquals(List.of(content, input), field.getChildren());
      assertEquals(List.of(field, group.getChildren().get(1)), group.getChildren());
      assertEquals(AccessibleRole.PARENT, field.getAccessibleRole());
      assertTrue(set.getStyleClass().contains("core-field-set"));
      assertTrue(content.getStyleClass().contains("core-field-content"));
    });
  }

  @Test
  void switchesOrientationAndPublishesInvalidState() {
    FxTestSupport.runAndWait(() -> {
      Field field = new Field();

      field.setOrientation(FieldOrientation.HORIZONTAL);
      field.setInvalid(true);

      assertEquals(FieldOrientation.HORIZONTAL, field.getOrientation());
      assertTrue(field.getStyleClass().contains("field-orientation-horizontal"));
      assertFalse(field.getStyleClass().contains("field-orientation-vertical"));
      assertTrue(field.getPseudoClassStates().contains(INVALID));
      assertThrows(NullPointerException.class, () -> field.setOrientation(null));
    });
  }

  @Test
  void laysOutVerticalHorizontalAndResponsiveFields() {
    FxTestSupport.runAndWait(() -> {
      FieldTitle title = new FieldTitle("Name");
      TextField input = new TextField();
      Field field = new Field(title, input);
      StackPane root = themedRoot(field, 600.0, 180.0);

      root.applyCss();
      root.layout();
      assertTrue(input.getLayoutY() >= title.getLayoutY() + title.getHeight() + 7.5);

      field.setOrientation(FieldOrientation.HORIZONTAL);
      root.layout();
      assertTrue(input.getLayoutX() >= title.getLayoutX() + title.getWidth() + 7.5);

      field.setOrientation(FieldOrientation.RESPONSIVE);
      root.resize(400.0, 180.0);
      root.layout();
      assertTrue(input.getLayoutY() >= title.getLayoutY() + title.getHeight() + 7.5);

      root.resize(600.0, 180.0);
      root.layout();
      assertTrue(input.getLayoutX() >= title.getLayoutX() + title.getWidth() + 7.5);
    });
  }

  @Test
  void mapsTypographySpacingAndDestructiveStateFromCss() {
    FxTestSupport.runAndWait(() -> {
      FieldLegend legend = new FieldLegend("Profile");
      FieldDescription description = new FieldDescription("Shown publicly");
      FieldError error = new FieldError("Required");
      Field field = new Field(description, error);
      FieldSet set = new FieldSet(legend, field);
      StackPane root = themedRoot(set, 320.0, 200.0);

      root.applyCss();
      field.setInvalid(true);
      root.applyCss();

      assertEquals(16.0, set.getSpacing());
      assertEquals(16.0, legend.getFont().getSize());
      assertEquals(new Insets(0.0, 0.0, 6.0, 0.0), legend.getPadding());
      assertEquals(14.0, description.getFont().getSize());
      assertEquals(Color.web("#737373"), description.getTextFill());
      assertEquals(Color.web("#e7000b"), ((javafx.scene.control.Label) error.getChildren().getFirst()).getTextFill());
    });
  }

  @Test
  void updatesLegendVariantAndSeparatorContent() {
    FxTestSupport.runAndWait(() -> {
      FieldLegend legend = new FieldLegend("Contact", FieldLegendVariant.LABEL);
      javafx.scene.control.Label content = new javafx.scene.control.Label("or");
      FieldSeparator separator = new FieldSeparator(content);
      StackPane root = themedRoot(new FieldSet(legend, separator), 320.0, 100.0);

      root.applyCss();

      assertEquals(14.0, legend.getFont().getSize());
      assertSame(content, separator.getContent());
      assertEquals(2, separator.getChildren().size());
      assertEquals(20.0, separator.getPrefHeight());

      separator.setContent(null);
      assertEquals(1, separator.getChildren().size());
    });
  }

  @Test
  void deduplicatesErrorsAndLetsExplicitContentTakePrecedence() {
    FxTestSupport.runAndWait(() -> {
      FieldError error = new FieldError(List.of("Required", "Required", "Too short"));

      assertEquals(2, error.getChildren().size());
      assertEquals("\u2022 Required", ((javafx.scene.control.Label) error.getChildren().getFirst()).getText());
      assertEquals("Required. Too short", error.getAccessibleText());

      Node explicit = new javafx.scene.control.Label("Custom error");
      error.setContent(explicit);
      assertEquals(List.of(explicit), error.getChildren());

      error.setContent(null);
      error.setErrors(List.of());
      assertFalse(error.isManaged());
      assertFalse(error.isVisible());
    });
  }

  private static StackPane themedRoot(Node content, double width, double height) {
    StackPane root = new StackPane(content);
    new ThemeManager(new Scene(root, width, height), AppTheme.LIGHT);
    root.resize(width, height);
    return root;
  }
}
