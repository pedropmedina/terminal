package com.acteque.terminal.ui.combobox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import org.junit.jupiter.api.Test;

class ComboboxTest {

  private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
  private static final PseudoClass HIGHLIGHTED = PseudoClass.getPseudoClass("highlighted");
  private static final PseudoClass EMPTY = PseudoClass.getPseudoClass("empty");

  @Test
  void composesAndFiltersFlatAndGroupedCollections() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>(FXCollections.observableArrayList("Apple", "Banana", "Carrot"));
      ComboboxInput<String> input = new ComboboxInput<>(combobox);
      ComboboxCollection<String> collection = new ComboboxCollection<>(combobox);
      ComboboxGroup<String> group = new ComboboxGroup<>(combobox, new ComboboxLabel("Produce"), collection);
      ComboboxList<String> list = new ComboboxList<>(combobox, group);
      ComboboxEmpty empty = new ComboboxEmpty(combobox, "No produce found");
      ComboboxContent<String> content = new ComboboxContent<>(combobox, empty, list);

      assertSame(combobox.getItems(), combobox.itemsProperty().get());
      assertEquals(3, collection.getChildrenUnmodifiable().size());
      assertEquals(ComboboxContent.Side.BOTTOM, content.getSide());
      assertEquals(ComboboxContent.Align.START, content.getAlign());
      assertEquals(6.0, content.getSideOffset());
      assertEquals(AccessibleRole.COMBO_BOX, input.getAccessibleRole());
      assertEquals(AccessibleRole.LIST_VIEW, list.getAccessibleRole());

      input.getEditor().setText("an");

      List<Node> items = List.copyOf(collection.getChildrenUnmodifiable());
      assertFalse(items.get(0).isVisible());
      assertTrue(items.get(1).isVisible());
      assertFalse(items.get(2).isVisible());
      assertTrue(group.isVisible());
      assertFalse(empty.isVisible());

      input.getEditor().setText("missing");

      assertTrue(items.stream().noneMatch(Node::isVisible));
      assertFalse(group.isVisible());
      assertTrue(empty.isVisible());
      assertTrue(content.getPseudoClassStates().contains(EMPTY));
      assertTrue(list.getPseudoClassStates().contains(EMPTY));

      combobox.setFilter(null);

      assertTrue(items.stream().allMatch(Node::isVisible));
      collection.dispose();
    });
  }

  @Test
  void replacesRootItemsAndHonorsCustomConverterDisabledPredicateAndLimit() {
    FxTestSupport.runAndWait(() -> {
      Combobox<Entry> combobox = new Combobox<>(FXCollections.observableArrayList(new Entry("a", "Alpha")));
      combobox.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Entry entry) {
            return entry == null ? "" : entry.label();
          }

          @Override
          public Entry fromString(String text) {
            throw new UnsupportedOperationException();
          }
        }
      );
      combobox.setItemDisabled(entry -> entry.id().equals("b"));
      combobox.setLimit(1);
      ComboboxCollection<Entry> collection = new ComboboxCollection<>(combobox);

      combobox.setItems(FXCollections.observableArrayList(new Entry("b", "Beta"), new Entry("c", "Gamma")));

      assertEquals(2, collection.getChildrenUnmodifiable().size());
      ComboboxItem<?> first = assertInstanceOf(ComboboxItem.class, collection.getChildrenUnmodifiable().get(0));
      ComboboxItem<?> second = assertInstanceOf(ComboboxItem.class, collection.getChildrenUnmodifiable().get(1));
      assertTrue(first.isVisible());
      assertTrue(first.isDisabled());
      assertFalse(second.isVisible());
      assertThrows(IllegalArgumentException.class, () -> combobox.setLimit(-2));
      collection.dispose();
    });
  }

  @Test
  void selectsSingleValuesAndClearsThroughTheInputActions() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>(FXCollections.observableArrayList("Apple", "Banana"));
      ComboboxInput<String> input = new ComboboxInput<>(combobox);
      input.setShowClear(true);
      ComboboxItem<String> apple = new ComboboxItem<>(combobox, "Apple");

      apple.fire();

      assertEquals("Apple", combobox.getValue());
      assertEquals(List.of("Apple"), combobox.getSelectedItems());
      assertEquals("Apple", combobox.getInputValue());
      assertTrue(apple.getPseudoClassStates().contains(SELECTED));
      assertTrue(input.getClear().isVisible());
      assertFalse(input.getTrigger().isVisible());
      assertEquals("Apple", input.queryAccessibleAttribute(AccessibleAttribute.TEXT));

      input.getClear().fire();

      assertNull(combobox.getValue());
      assertTrue(combobox.getSelectedItems().isEmpty());
      assertEquals("", input.getEditor().getText());
      assertFalse(apple.getPseudoClassStates().contains(SELECTED));
      assertTrue(input.getTrigger().isVisible());
      apple.dispose();
    });
  }

  @Test
  void supportsMultipleSelectionValueRenderingAndChipRemoval() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>(FXCollections.observableArrayList("Apple", "Banana"));
      combobox.setValue("Apple");
      combobox.setMultiple(true);
      ComboboxItem<String> banana = new ComboboxItem<>(combobox, "Banana");
      ComboboxValue<String> value = new ComboboxValue<>(combobox);

      banana.fire();

      assertNull(combobox.getValue());
      assertEquals(List.of("Apple", "Banana"), combobox.getSelectedItems());
      assertEquals(2, value.getChildrenUnmodifiable().size());
      ComboboxChip<?> first = assertInstanceOf(ComboboxChip.class, value.getChildrenUnmodifiable().getFirst());
      assertEquals("Apple", first.getItem());

      first.fireEvent(keyPressed(KeyCode.DELETE));

      assertEquals(List.of("Banana"), combobox.getSelectedItems());
      assertEquals(1, value.getChildrenUnmodifiable().size());
      ComboboxChip<?> remaining = assertInstanceOf(ComboboxChip.class, value.getChildrenUnmodifiable().getFirst());
      remaining.setShowRemove(false);
      assertFalse(remaining.lookup(".combobox-chip-remove").isVisible());
      banana.dispose();
    });
  }

  @Test
  void providesKeyboardNavigationSelectionAndBackspaceRemoval() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>(FXCollections.observableArrayList("One", "Two", "Three"));
      combobox.setItemDisabled("Two"::equals);
      ComboboxInput<String> input = new ComboboxInput<>(combobox);
      ComboboxItem<String> one = new ComboboxItem<>(combobox, "One");
      ComboboxItem<String> two = new ComboboxItem<>(combobox, "Two");
      ComboboxItem<String> three = new ComboboxItem<>(combobox, "Three");

      input.getEditor().fireEvent(keyPressed(KeyCode.DOWN));
      assertTrue(one.getPseudoClassStates().contains(HIGHLIGHTED));
      input.getEditor().fireEvent(keyPressed(KeyCode.DOWN));
      assertFalse(two.getPseudoClassStates().contains(HIGHLIGHTED));
      assertTrue(three.getPseudoClassStates().contains(HIGHLIGHTED));
      input.getEditor().fireEvent(keyPressed(KeyCode.ENTER));

      assertEquals("Three", combobox.getValue());

      combobox.setMultiple(true);
      combobox.getSelectedItems().add("One");
      input.getEditor().setText("");
      input.getEditor().fireEvent(keyPressed(KeyCode.BACK_SPACE));

      assertEquals(List.of("Three"), combobox.getSelectedItems());
      one.dispose();
      two.dispose();
      three.dispose();
    });
  }

  @Test
  void mapsShadcnDimensionsColorsAndIcons() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>();
      ComboboxInput<String> input = new ComboboxInput<>(combobox);
      input.setShowClear(true);
      combobox.setValue("Value");
      ComboboxChips<String> chips = new ComboboxChips<>(combobox);
      ComboboxChip<String> chip = new ComboboxChip<>(combobox, "Value");
      chips.getItems().add(chip);
      StackPane root = themedRoot(new StackPane(input, chips), AppTheme.LIGHT);

      root.applyCss();
      root.layout();

      assertEquals(32.0, input.prefHeight(-1.0));
      assertEquals(8.0, input.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(21.0, chip.prefHeight(-1.0));
      assertEquals(Color.web("#f5f5f5"), background(chip));
      assertEquals(new Insets(0.0, 0.0, 0.0, 6.0), chip.getPadding());
      assertTrue(input.getClear().getGraphic().getStyleClass().contains("lucide-icon"));
    });
  }

  @Test
  void anchorsAndSizesThePopupToTheInput() {
    FxTestSupport.runAndWait(() -> {
      Combobox<String> combobox = new Combobox<>(FXCollections.observableArrayList("One", "Two"));
      ComboboxInput<String> input = new ComboboxInput<>(combobox);
      input.setPrefWidth(220.0);
      ComboboxCollection<String> collection = new ComboboxCollection<>(combobox);
      ComboboxContent<String> content = new ComboboxContent<>(
        combobox,
        new ComboboxEmpty(combobox, "Nothing found"),
        new ComboboxList<>(combobox, collection)
      );
      StackPane root = themedRoot(new StackPane(input), AppTheme.LIGHT);
      Stage stage = new Stage();
      stage.setScene(root.getScene());
      Platform.setImplicitExit(false);

      try {
        stage.show();
        root.applyCss();
        root.layout();
        combobox.show();

        assertTrue(combobox.getPopup().isShowing());
        assertTrue(combobox.isOpen());
        assertTrue(content.getPrefWidth() >= input.getWidth());
        assertTrue(content.getStyleClass().contains("theme-light"));
        assertEquals(Boolean.TRUE, input.queryAccessibleAttribute(AccessibleAttribute.EXPANDED));
      } finally {
        combobox.getPopup().hide();
        stage.hide();
        collection.dispose();
      }
    });
  }

  private static KeyEvent keyPressed(KeyCode code) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
  }

  private static Color background(Region region) {
    return (Color) region.getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(StackPane content, AppTheme theme) {
    StackPane root = new StackPane(content);
    new ThemeManager(new Scene(root), theme);
    return root;
  }

  private record Entry(String id, String label) {}
}
