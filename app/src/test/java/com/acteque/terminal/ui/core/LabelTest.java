package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.junit.jupiter.api.Test;

class LabelTest {

  private static final PseudoClass LABEL_FOR_DISABLED = PseudoClass.getPseudoClass("label-for-disabled");

  @Test
  void providesJavaFxLabelConstructorsAndCoreStyleClass() {
    FxTestSupport.runAndWait(() -> {
      Circle graphic = new Circle(4.0);
      Label label = new Label("Price", graphic);

      assertEquals("Price", label.getText());
      assertSame(graphic, label.getGraphic());
      assertTrue(label.getStyleClass().contains("label"));
      assertTrue(label.getStyleClass().contains("core-label"));
    });
  }

  @Test
  void resolvesFoundationStylesFromCss() {
    FxTestSupport.runAndWait(() -> {
      Label label = new Label("Price", new Circle(4.0));
      StackPane root = new StackPane(label);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      root.applyCss();

      assertEquals(Pos.CENTER_LEFT, label.getAlignment());
      assertEquals(ContentDisplay.LEFT, label.getContentDisplay());
      assertEquals(8.0, label.getGraphicTextGap());
      assertEquals(14.0, label.getFont().getSize());
      assertEquals(0.0, label.getLineSpacing());
      assertEquals(Color.web("#0a0a0a"), label.getTextFill());
    });
  }

  @Test
  void reflectsTheLabelForDisabledState() {
    FxTestSupport.runAndWait(() -> {
      Label label = new Label("Symbol");
      TextField firstField = new TextField();
      TextField secondField = new TextField();
      label.setLabelFor(firstField);

      firstField.setDisable(true);
      assertTrue(label.getPseudoClassStates().contains(LABEL_FOR_DISABLED));

      label.setLabelFor(secondField);
      assertFalse(label.getPseudoClassStates().contains(LABEL_FOR_DISABLED));

      firstField.setDisable(false);
      firstField.setDisable(true);
      assertFalse(label.getPseudoClassStates().contains(LABEL_FOR_DISABLED));

      secondField.setDisable(true);
      assertTrue(label.getPseudoClassStates().contains(LABEL_FOR_DISABLED));
    });
  }

  @Test
  void dimsWhenItsLabelForControlOrAnAncestorIsDisabled() {
    FxTestSupport.runAndWait(() -> {
      Label label = new Label("Symbol");
      TextField field = new TextField();
      label.setLabelFor(field);
      StackPane root = new StackPane(label, field);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);

      field.setDisable(true);
      root.applyCss();
      assertEquals(0.5, label.getOpacity());

      field.setDisable(false);
      root.setDisable(true);
      root.applyCss();
      assertTrue(label.isDisabled());
      assertEquals(0.5, label.getOpacity());
    });
  }
}
