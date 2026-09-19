package com.acteque.terminal.chart.settings;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.ChartTypePresentation;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.drawer.Drawer;
import com.acteque.terminal.ui.drawer.DrawerDirection;
import com.acteque.terminal.ui.drawer.DrawerMode;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.togglegroup.ToggleGroupItem;
import java.util.concurrent.atomic.AtomicReference;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.Test;

class ChartSettingsTest {

  @Test
  void presentsEveryChartTypeInTheLeftSettingsDrawer() {
    FxTestSupport.runAndWait(() -> {
      ChartSettings settings = new ChartSettings();
      Drawer drawer = settings.getView();
      StackPane root = new StackPane(drawer);
      new AppThemeManager(new Scene(root, 800, 600), AppTheme.LIGHT);

      settings.showChartTypes();
      root.applyCss();
      root.layout();

      assertEquals(DrawerDirection.LEFT, drawer.getDirection());
      assertEquals(DrawerMode.NON_MODAL, drawer.getMode());
      assertTrue(drawer.isDismissOnOutsidePress());
      assertTrue(drawer.isOpen());
      assertEquals(384.0, drawer.getContent().getWidth());

      ToggleGroup options = options(drawer);
      assertEquals(ChartType.values().length, options.getChildren().size());
      assertTrue(options.getWidth() <= drawer.getContent().getWidth());
      for (int index = 0; index < ChartType.values().length; index++) {
        ChartType type = ChartType.values()[index];
        ToggleGroupItem item = assertInstanceOf(ToggleGroupItem.class, options.getChildren().get(index));
        HBox row = assertInstanceOf(HBox.class, item.getGraphic());
        VBox text = assertInstanceOf(VBox.class, row.getChildren().get(1));

        assertSame(
          ChartTypePresentation.icon(type),
          assertInstanceOf(LucideIcon.class, row.getChildren().get(0)).getGlyph()
        );
        assertEquals(
          ChartTypePresentation.displayName(type),
          assertInstanceOf(Label.class, text.getChildren().get(0)).getText()
        );
        assertEquals(
          ChartTypePresentation.description(type),
          assertInstanceOf(Label.class, text.getChildren().get(1)).getText()
        );
        assertTrue(item.isFocusTraversable());
        assertTrue(item.getHeight() > 32.0);
      }
      assertTrue(item(options, ChartType.LINE).isSelected());
    });
  }

  @Test
  void selectingATypeClosesTheDrawerAndRetainsSelectionOnReopen() {
    FxTestSupport.runAndWait(() -> {
      ChartSettings settings = new ChartSettings();
      AtomicReference<ChartType> selected = new AtomicReference<>();
      settings.onChartTypeSelected(selected::set);
      settings.setChartType(ChartType.CANDLESTICK);
      settings.showChartTypes();
      ToggleGroup options = options(settings.getView());

      assertTrue(item(options, ChartType.CANDLESTICK).isSelected());
      item(options, ChartType.STEP_LINE).fire();

      assertEquals(ChartType.STEP_LINE, selected.get());
      assertFalse(settings.getView().isOpen());
      settings.showChartTypes();
      assertTrue(item(options, ChartType.STEP_LINE).isSelected());
      assertFalse(item(options, ChartType.CANDLESTICK).isSelected());

      item(options, ChartType.STEP_LINE).fire();
      assertTrue(item(options, ChartType.STEP_LINE).isSelected());
      assertFalse(settings.getView().isOpen());
    });
  }

  @Test
  void presentsChartTypesWithoutHeaderOrScrollContainer() {
    FxTestSupport.runAndWait(() -> {
      ChartSettings settings = new ChartSettings();
      Drawer drawer = settings.getView();
      StackPane root = new StackPane(drawer);
      new AppThemeManager(new Scene(root, 800, 360), AppTheme.LIGHT);

      settings.showChartTypes();
      root.applyCss();
      root.layout();

      ToggleGroup options = options(drawer);

      assertSame(options, drawer.getContent().getBody().getChildren().getFirst());
      assertEquals(1, drawer.getContent().getBody().getChildren().size());
      assertTrue(drawer.lookupAll(".core-drawer-header").isEmpty());
      assertTrue(drawer.lookupAll(".core-drawer-close").isEmpty());
      assertTrue(drawer.lookupAll(".scroll-pane").isEmpty());
    });
  }

  @Test
  void fitsTheDrawerHeightToItsContent() {
    FxTestSupport.runAndWait(() -> {
      ChartSettings settings = new ChartSettings();
      Drawer drawer = settings.getView();
      StackPane root = new StackPane(drawer);
      new AppThemeManager(new Scene(root, 800, 760), AppTheme.LIGHT);

      settings.showChartTypes();
      root.applyCss();
      root.layout();

      assertEquals(Region.USE_PREF_SIZE, drawer.getMaxHeight());
      assertSame(Pos.TOP_LEFT, StackPane.getAlignment(drawer));
      assertEquals(0.0, drawer.getLayoutY());
      assertTrue(drawer.getHeight() > 0.0);
      assertTrue(drawer.getHeight() < root.getHeight());
      assertTrue(options(drawer).getBoundsInParent().getMaxY() <= drawer.getContent().getHeight());
    });
  }

  private static ToggleGroup options(Drawer drawer) {
    return assertInstanceOf(ToggleGroup.class, drawer.getContent().getBody().getChildren().getFirst());
  }

  private static ToggleGroupItem item(ToggleGroup options, ChartType type) {
    return assertInstanceOf(ToggleGroupItem.class, options.getChildren().get(type.ordinal()));
  }
}
