package com.acteque.terminal.ui.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
<<<<<<< HEAD
=======
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
>>>>>>> 754c827 (h)
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Select.Size;
<<<<<<< HEAD
=======
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
>>>>>>> 754c827 (h)
import java.util.concurrent.atomic.AtomicBoolean;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.PopupWindow;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.StringConverter;
import org.junit.jupiter.api.Test;

class SelectTest {

  private static final PseudoClass INVALID = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass PLACEHOLDER_SHOWN = PseudoClass.getPseudoClass("placeholder-shown");

  @Test
  void preservesChoiceBoxItemsSelectionConverterAndActions() {
    FxTestSupport.runAndWait(() -> {
      Item first = new Item("One");
      Item second = new Item("Two");
      var items = FXCollections.observableArrayList(first, second);
      Select<Item> select = new Select<>(items);
      AtomicBoolean actionFired = new AtomicBoolean();
      select.setConverter(
        new StringConverter<>() {
          @Override
          public String toString(Item item) {
            return item == null ? "" : item.label();
          }

          @Override
          public Item fromString(String text) {
            return items
              .stream()
              .filter(item -> item.label().equals(text))
              .findFirst()
              .orElse(null);
          }
        }
      );
      select.setOnAction(ignored -> actionFired.set(true));

      select.getSelectionModel().select(second);

      assertSame(items, select.getItems());
      assertSame(second, select.getValue());
      assertEquals("Two", select.getConverter().toString(select.getValue()));
      assertTrue(actionFired.get());
      assertTrue(select.getStyleClass().contains("choice-box"));
      assertTrue(select.getStyleClass().contains("core-select"));
    });
  }

  @Test
  void exposesBindableSizeAndInvalidState() {
    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>();
      SimpleObjectProperty<Size> size = new SimpleObjectProperty<>(Size.SM);
      SimpleBooleanProperty invalid = new SimpleBooleanProperty(true);
      select.sizeProperty().bind(size);
      select.invalidProperty().bind(invalid);

      assertEquals(Size.SM, select.getSize());
      assertTrue(select.getStyleClass().contains("select-size-sm"));
      assertFalse(select.getStyleClass().contains("select-size-default"));
      assertTrue(select.getPseudoClassStates().contains(INVALID));

      size.set(Size.DEFAULT);
      invalid.set(false);

      assertEquals(Size.DEFAULT, select.getSize());
      assertTrue(select.getStyleClass().contains("select-size-default"));
      assertFalse(select.getStyleClass().contains("select-size-sm"));
      assertFalse(select.getPseudoClassStates().contains(INVALID));
    });
  }

  @Test
  void rejectsNullItemsAndSize() {
    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>();

      assertThrows(NullPointerException.class, () -> new Select<String>(null));
      assertThrows(NullPointerException.class, () -> select.setSize(null));
      assertEquals(Size.DEFAULT, select.getSize());
    });
  }

  @Test
  void displaysPromptOnlyWhileValueIsEmpty() {
    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>(FXCollections.observableArrayList("One", "Two"));
      select.setPromptText("Choose an option");
      StackPane root = themedRoot(select, AppTheme.LIGHT);

      root.applyCss();
      root.layout();

      Label placeholder = (Label) select.lookup(".select-placeholder");
      assertEquals("Choose an option", placeholder.getText());
      assertTrue(placeholder.isVisible());
      assertTrue(placeholder.isManaged());
      assertTrue(select.getPseudoClassStates().contains(PLACEHOLDER_SHOWN));
      assertEquals("Choose an option", select.queryAccessibleAttribute(AccessibleAttribute.TEXT));

      select.setValue("One");
      root.layout();

      assertFalse(placeholder.isVisible());
      assertFalse(placeholder.isManaged());
      assertFalse(select.getPseudoClassStates().contains(PLACEHOLDER_SHOWN));
      assertEquals("One", select.queryAccessibleAttribute(AccessibleAttribute.TEXT));

      select.setValue(null);
      select.setPromptText("");

      assertFalse(placeholder.isVisible());
      assertFalse(select.getPseudoClassStates().contains(PLACEHOLDER_SHOWN));
    });
  }

  @Test
  void mapsShadcnDimensionsAndThemeStyles() {
    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>();
      StackPane root = themedRoot(select, AppTheme.LIGHT);

      root.applyCss();

      assertEquals(32.0, select.prefHeight(-1.0));
      assertEquals(14.0, ((Label) select.lookup(".label")).getFont().getSize());
      assertEquals(Color.TRANSPARENT, background(select));
      assertEquals(Color.web("#e5e5e5"), select.getBorder().getStrokes().getFirst().getTopStroke());
      assertEquals(8.0, select.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());

      select.setSize(Size.SM);
      root.applyCss();

      assertEquals(28.0, select.prefHeight(-1.0));
      assertEquals(6.0, select.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());

      Select<String> darkSelect = new Select<>();
      StackPane darkRoot = themedRoot(darkSelect, AppTheme.DARK);
      darkRoot.applyCss();

      assertEquals(Color.web("rgba(255, 255, 255, 0.045)"), background(darkSelect));
    });
  }

  @Test
  void mapsFocusInvalidDisabledAndArrowStyles() {
    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>();
      StackPane root = themedRoot(select, AppTheme.LIGHT);
      select.pseudoClassStateChanged(PseudoClass.getPseudoClass("focus-visible"), true);

      root.applyCss();

      assertEquals(2, select.getBorder().getStrokes().size());
      assertEquals(Color.web("#a1a1a1"), select.getBorder().getStrokes().getFirst().getTopStroke());
      assertEquals(new Insets(-3.0), select.getBorder().getStrokes().get(1).getInsets());

      select.setInvalid(true);
      root.applyCss();
      assertEquals(Color.web("#e7000b"), select.getBorder().getStrokes().getFirst().getTopStroke());

      select.setDisable(true);
      root.applyCss();
      assertEquals(0.5, select.getOpacity());

<<<<<<< HEAD
      Node arrow = select.lookup(".open-button .arrow");
      assertEquals(16.0, arrow.prefWidth(-1.0));
      assertEquals(16.0, arrow.prefHeight(-1.0));
=======
      LucideIcon downIndicator = assertInstanceOf(LucideIcon.class, select.lookup(".select-indicator-down"));
      LucideIcon upIndicator = assertInstanceOf(LucideIcon.class, select.lookup(".select-indicator-up"));
      assertSame(LucideIcons.CHEVRON_DOWN, downIndicator.getGlyph());
      assertSame(LucideIcons.CHEVRON_UP, upIndicator.getGlyph());
      assertEquals(16.0, downIndicator.prefWidth(-1.0));
      assertEquals(16.0, downIndicator.prefHeight(-1.0));
      assertTrue(downIndicator.isVisible());
      assertFalse(upIndicator.isVisible());
>>>>>>> 754c827 (h)
    });
  }

  @Test
  void stylesTheNativePopupItemsIndicatorsSeparatorsAndScrollArrows() {
    FxTestSupport.runAndWait(() -> {
      Select<Object> select = new Select<>(FXCollections.observableArrayList("One", new Separator(), "Two"));
      select.getSelectionModel().selectFirst();
      StackPane root = themedRoot(select, AppTheme.LIGHT);
      Stage stage = new Stage();
      stage.setScene(root.getScene());
      Platform.setImplicitExit(false);

      try {
        stage.show();
        root.applyCss();
        root.layout();
        select.show();

<<<<<<< HEAD
=======
        assertFalse(select.lookup(".select-indicator-down").isVisible());
        assertTrue(select.lookup(".select-indicator-up").isVisible());

>>>>>>> 754c827 (h)
        Window popup = Window.getWindows()
          .stream()
          .filter(window -> window instanceof PopupWindow && window.isShowing())
          .findFirst()
          .orElseThrow();
        Node popupRoot = popup.getScene().getRoot();
        popupRoot.applyCss();

        Region contextMenu = popupRoot
          .lookupAll(".context-menu")
          .stream()
          .map(Region.class::cast)
          .filter(node -> node.getParent() != popupRoot)
          .findFirst()
          .orElseThrow();
        assertEquals(new Insets(4.0), contextMenu.getPadding());
        assertEquals(2, contextMenu.getBackground().getFills().size());
        assertEquals(Color.web("#ffffff"), contextMenu.getBackground().getFills().get(1).getFill());
        assertFalse(popupRoot.lookupAll(".menu-item").isEmpty());
        assertEquals(new Insets(4.0, 0.0, 4.0, 0.0), ((Region) popupRoot.lookup(".separator")).getPadding());

        Region checkedIndicator = popupRoot
          .lookupAll(".radio")
          .stream()
          .map(Region.class::cast)
          .filter(node -> node.getBackground() != null && !node.getBackground().getFills().isEmpty())
          .findFirst()
          .orElseThrow();
        assertEquals(12.0, checkedIndicator.prefWidth(-1.0));
        assertFalse(Color.TRANSPARENT.equals(checkedIndicator.getBackground().getFills().getFirst().getFill()));

        Node scrollArrow = popupRoot.lookup(".scroll-arrow");
        Node scrollChevron = popupRoot.lookup(".menu-up-arrow");
        assertEquals(new Insets(4.0), ((Region) scrollArrow).getPadding());
        assertEquals(16.0, scrollChevron.prefWidth(-1.0));
      } finally {
        select.hide();
        stage.hide();
      }
    });
  }

  private static Color background(Select<?> select) {
    return (Color) select.getBackground().getFills().getFirst().getFill();
  }

  private static StackPane themedRoot(Select<?> select, AppTheme theme) {
    StackPane root = new StackPane(select);
    new ThemeManager(new Scene(root, 320, 200), theme);
    return root;
  }

  private record Item(String label) {}
}
