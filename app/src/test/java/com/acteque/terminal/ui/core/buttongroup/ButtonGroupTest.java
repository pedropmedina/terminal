package com.acteque.terminal.ui.core.buttongroup;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.TraversalDirection;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class ButtonGroupTest {

  @Test
  void defaultsToHorizontalAndClassifiesItsChildren() {
    FxTestSupport.runAndWait(() -> {
      Button first = new Button("Previous");
      Button middle = new Button("Today");
      Button last = new Button("Next");

      ButtonGroup group = new ButtonGroup(first, middle, last);

      assertEquals(Orientation.HORIZONTAL, group.getOrientation());
      assertEquals(AccessibleRole.PARENT, group.getAccessibleRole());
      assertTrue(group.getStyleClass().contains("core-button-group"));
      assertTrue(group.getStyleClass().contains("button-group-horizontal"));
      assertTrue(first.getStyleClass().contains("button-group-first"));
      assertTrue(middle.getStyleClass().contains("button-group-middle"));
      assertTrue(last.getStyleClass().contains("button-group-last"));
    });
  }

  @Test
  void switchesOrientationAndSupportsPropertyBinding() {
    FxTestSupport.runAndWait(() -> {
      ButtonGroup group = new ButtonGroup();

      group.setOrientation(Orientation.VERTICAL);

      assertEquals(Orientation.VERTICAL, group.getOrientation());
      assertTrue(group.getStyleClass().contains("button-group-vertical"));
      assertFalse(group.getStyleClass().contains("button-group-horizontal"));
      assertThrows(NullPointerException.class, () -> group.setOrientation(null));
      assertEquals(Orientation.VERTICAL, group.getOrientation());
    });
  }

  @Test
  void refreshesPositionClassesWhenChildrenChange() {
    FxTestSupport.runAndWait(() -> {
      Button first = new Button("One");
      Button second = new Button("Two");
      ButtonGroup group = new ButtonGroup(first, second);

      group.getChildren().remove(first);

      assertFalse(first.getStyleClass().contains("button-group-item"));
      assertTrue(second.getStyleClass().contains("button-group-only"));
      assertFalse(second.getStyleClass().contains("button-group-last"));
    });
  }

  @Test
  void laysOutChildrenAlongTheSelectedAxis() {
    FxTestSupport.runAndWait(() -> {
      Button first = new Button("One", Button.Variant.OUTLINE, Button.Size.DEFAULT);
      Button second = new Button("Two", Button.Variant.OUTLINE, Button.Size.DEFAULT);
      ButtonGroup group = new ButtonGroup(first, second);
      StackPane root = new StackPane(group);
      new ThemeManager(new Scene(root, 300, 200), AppTheme.LIGHT);
      root.applyCss();
      root.layout();

      assertEquals(first.getLayoutX() + first.getWidth(), second.getLayoutX());
      assertEquals(group.prefWidth(-1.0), group.getWidth(), 0.5);
      assertEquals(group.prefHeight(-1.0), group.getHeight(), 0.5);

      group.setOrientation(Orientation.VERTICAL);
      root.layout();

      assertEquals(first.getLayoutY() + first.getHeight(), second.getLayoutY());
    });
  }

  @Test
  void leavesSpaceBetweenNestedGroups() {
    FxTestSupport.runAndWait(() -> {
      ButtonGroup first = new ButtonGroup(new Button("One"));
      ButtonGroup second = new ButtonGroup(new Button("Two"));
      ButtonGroup group = new ButtonGroup(first, second);
      StackPane root = new StackPane(group);
      new ThemeManager(new Scene(root, 300, 200), AppTheme.LIGHT);
      root.applyCss();
      root.layout();

      assertEquals(8.0, second.getLayoutX() - first.getLayoutX() - first.getWidth(), 0.5);
    });
  }

  @Test
  void rendersTheFocusedItemAboveItsSiblingsWithoutReordering() {
    AtomicReference<Button> firstReference = new AtomicReference<>();
    AtomicReference<Button> secondReference = new AtomicReference<>();
    AtomicReference<ButtonGroup> groupReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Button first = new Button("One");
      Button second = new Button("Two");
      ButtonGroup group = new ButtonGroup(first, second);
      Stage stage = new Stage();
      stage.setScene(new Scene(new StackPane(group)));
      Platform.setImplicitExit(false);
      firstReference.set(first);
      secondReference.set(second);
      groupReference.set(group);
      stageReference.set(stage);
      stage.show();
      stage.requestFocus();
    });
    try {
      FxTestSupport.runAndWait(() -> firstReference.get().requestFocus());
      FxTestSupport.runAndWait(() -> firstReference.get().requestFocusTraversal(TraversalDirection.NEXT));
      FxTestSupport.runAndWait(() -> {
        Button first = firstReference.get();
        Button second = secondReference.get();
        ButtonGroup group = groupReference.get();
        assertTrue(second.isFocused());
        assertTrue(second.isFocusVisible());
        assertTrue(second.getViewOrder() < first.getViewOrder());
        assertEquals(List.of(first, second), group.getChildren());
      });

      FxTestSupport.runAndWait(() -> secondReference.get().requestFocusTraversal(TraversalDirection.PREVIOUS));
      FxTestSupport.runAndWait(() -> {
        Button first = firstReference.get();
        Button second = secondReference.get();
        ButtonGroup group = groupReference.get();
        assertEquals(0.0, second.getViewOrder());
        assertTrue(first.getViewOrder() < second.getViewOrder());
        assertEquals(List.of(first, second), group.getChildren());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
    }
  }

  @Test
  void providesThemeAwareTextAndSeparatorItems() {
    FxTestSupport.runAndWait(() -> {
      ButtonGroupText text = new ButtonGroupText("More");
      ButtonGroupSeparator separator = new ButtonGroupSeparator();
      ButtonGroup group = new ButtonGroup(text, separator);
      StackPane root = new StackPane(group);
      new ThemeManager(new Scene(root), AppTheme.LIGHT);
      root.applyCss();

      assertEquals(Orientation.VERTICAL, separator.getOrientation());
      assertEquals(new Insets(1.0, 0.0, 1.0, 0.0), separator.getPadding());
      assertTrue(text.getChildren().getFirst() instanceof Label);
      assertEquals(Color.web("#f5f5f5"), text.getBackground().getFills().getFirst().getFill());
    });
  }
}
