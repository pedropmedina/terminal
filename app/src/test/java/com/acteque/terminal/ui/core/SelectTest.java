package com.acteque.terminal.ui.core;

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
import com.acteque.terminal.ui.core.Select.Size;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
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
      assertEquals(2, select.getBorder().getStrokes().size());
      assertEquals(Color.web("#e5e5e5"), select.getBorder().getStrokes().getFirst().getTopStroke());
      assertEquals(Color.TRANSPARENT, select.getBorder().getStrokes().get(1).getTopStroke());
      assertEquals(8.0, select.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(11.0, select.getBorder().getStrokes().get(1).getRadii().getTopLeftHorizontalRadius());
      assertEquals(new Insets(-3.0), select.getBorder().getStrokes().get(1).getInsets());

      select.setSize(Size.SM);
      root.applyCss();

      assertEquals(28.0, select.prefHeight(-1.0));
      assertEquals(6.0, select.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(9.0, select.getBorder().getStrokes().get(1).getRadii().getTopLeftHorizontalRadius());

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
      assertEquals(Color.TRANSPARENT, select.getBorder().getStrokes().get(1).getTopStroke());

      select.setDisable(true);
      root.applyCss();
      assertEquals(0.5, select.getOpacity());

      LucideIcon downIndicator = assertInstanceOf(LucideIcon.class, select.lookup(".select-indicator-down"));
      LucideIcon upIndicator = assertInstanceOf(LucideIcon.class, select.lookup(".select-indicator-up"));
      assertSame(LucideIcons.CHEVRON_DOWN, downIndicator.getGlyph());
      assertSame(LucideIcons.CHEVRON_UP, upIndicator.getGlyph());
      assertEquals(16.0, downIndicator.prefWidth(-1.0));
      assertEquals(16.0, downIndicator.prefHeight(-1.0));
      assertTrue(downIndicator.isVisible());
      assertFalse(upIndicator.isVisible());
    });
  }

  @Test
  void stylesAndAlignsTheNativePopupWithRightAlignedLucideCheck() {
    FxTestSupport.runAndWait(() -> {
      Select<Object> select = new Select<>(FXCollections.observableArrayList("One", new Separator(), "Two"));
      select.setPrefWidth(180.0);
      select.getSelectionModel().select(2);
      StackPane root = themedRoot(select, AppTheme.LIGHT);
      Stage stage = new Stage();
      stage.setScene(root.getScene());
      Platform.setImplicitExit(false);

      try {
        stage.show();
        root.applyCss();
        root.layout();
        select.show();

        assertFalse(select.lookup(".select-indicator-down").isVisible());
        assertTrue(select.lookup(".select-indicator-up").isVisible());
        ContextMenu popup = Window.getWindows()
          .stream()
          .filter(ContextMenu.class::isInstance)
          .map(ContextMenu.class::cast)
          .filter(window -> window.isShowing() && window.getOwnerNode() == select)
          .findFirst()
          .orElseThrow();
        Node popupRoot = popup.getScene().getRoot();
        popupRoot.applyCss();
        assertInstanceOf(Parent.class, popupRoot).layout();
        assertTrue(popupRoot.getStyleClass().contains("core-select-popup-root"));
        Region popupSceneRoot = assertInstanceOf(Region.class, popupRoot);
        assertEquals(Color.TRANSPARENT, popupSceneRoot.getBackground().getFills().getFirst().getFill());
        assertEquals(8.0, popupSceneRoot.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());

        Bounds triggerBounds = select.localToScreen(select.getBoundsInLocal());
        Region popupSurface = assertInstanceOf(Region.class, popup.getStyleableNode());
        Bounds popupSurfaceBounds = popupSurface.localToScreen(popupSurface.getLayoutBounds());
        assertEquals(select.getWidth(), popupSurfaceBounds.getWidth());
        assertNull(popupSurface.getBackground());
        assertNull(popupSurface.getEffect());

        Region contextMenu = popupRoot
          .lookupAll(".context-menu")
          .stream()
          .map(Region.class::cast)
          .filter(node -> node.getParent() != popupRoot)
          .findFirst()
          .orElseThrow();
        assertEquals(select.getWidth(), contextMenu.getWidth());
        assertEquals(new Insets(4.0), contextMenu.getPadding());
        assertEquals(2, contextMenu.getBackground().getFills().size());
        assertEquals(Color.web("#ffffff"), contextMenu.getBackground().getFills().get(1).getFill());
        assertNull(contextMenu.getEffect());
        Rectangle popupClip = assertInstanceOf(Rectangle.class, contextMenu.getClip());
        assertEquals(16.0, popupClip.getArcWidth());
        assertEquals(16.0, popupClip.getArcHeight());
        assertFalse(popupRoot.lookupAll(".menu-item").isEmpty());
        assertEquals(new Insets(4.0, 0.0, 4.0, 0.0), ((Region) popupRoot.lookup(".separator")).getPadding());

        LucideIcon checkedIndicator = assertInstanceOf(LucideIcon.class, popupRoot.lookup(".select-item-check"));
        assertSame(LucideIcons.CHECK, checkedIndicator.getGlyph());
        assertEquals(16.0, checkedIndicator.prefWidth(-1.0));
        assertTrue(
          popupRoot
            .lookupAll(".radio")
            .stream()
            .allMatch(radio -> !radio.getParent().isVisible() && radio.getParent().getOpacity() == 0.0)
        );
        Node selectedItem = checkedIndicator.getParent().getParent();
        Node selectedLabel = selectedItem.lookup(".label");
        Bounds selectedItemBounds = selectedItem.localToScreen(selectedItem.getBoundsInLocal());
        assertEquals(triggerBounds.getCenterY(), selectedItemBounds.getCenterY(), 1.0);
        Bounds checkBounds = checkedIndicator.localToScene(checkedIndicator.getBoundsInLocal());
        Bounds labelBounds = selectedLabel.localToScene(selectedLabel.getBoundsInLocal());
        assertTrue(
          checkBounds.getMinX() > labelBounds.getMaxX(),
          () -> "check=" + checkBounds + ", label=" + labelBounds
        );

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

  @Test
  void retainsTriggerWidthAfterPopupLayoutPulse() throws InterruptedException {
    AtomicReference<Select<String>> selectReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();

    FxTestSupport.runAndWait(() -> {
      Select<String> select = new Select<>(FXCollections.observableArrayList("One", "Two"));
      select.setPrefWidth(220.0);
      StackPane root = themedRoot(select, AppTheme.LIGHT);
      Stage stage = new Stage();
      stage.setScene(root.getScene());
      Platform.setImplicitExit(false);
      stage.show();
      root.applyCss();
      root.layout();
      select.show();
      selectReference.set(select);
      stageReference.set(stage);
    });

    waitForNextPulse();

    try {
      FxTestSupport.runAndWait(() -> {
        Select<String> select = selectReference.get();
        ContextMenu popup = Window.getWindows()
          .stream()
          .filter(ContextMenu.class::isInstance)
          .map(ContextMenu.class::cast)
          .filter(window -> window.isShowing() && window.getOwnerNode() == select)
          .findFirst()
          .orElseThrow();
        Region popupContent = assertInstanceOf(Region.class, popup.getSkin().getNode());
        assertEquals(select.getWidth(), popupContent.getWidth());

        select.resize(260.0, select.getHeight());

        assertEquals(260.0, popupContent.getWidth());
      });
    } finally {
      FxTestSupport.runAndWait(() -> {
        selectReference.get().hide();
        stageReference.get().hide();
      });
    }
  }

  private static void waitForNextPulse() throws InterruptedException {
    CountDownLatch pulse = new CountDownLatch(1);
    Platform.runLater(() ->
      new AnimationTimer() {
        @Override
        public void handle(long now) {
          stop();
          pulse.countDown();
        }
      }.start()
    );
    assertTrue(pulse.await(10, TimeUnit.SECONDS), "Timed out waiting for a JavaFX pulse");
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
