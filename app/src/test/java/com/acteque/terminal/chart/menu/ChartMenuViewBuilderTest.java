package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.List;
import java.util.Map;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

class ChartMenuViewBuilderTest {

  @Test
  void buildsAccessibleButtonsFromTheModel() {
    FxTestSupport.runAndWait(() -> {
      ChartMenuModel model = new ChartMenuModel();
      ChartMenuInteractor interactor = new ChartMenuInteractor(model);
      interactor.initialize("ACME", ChartInterval.DAILY);
      Region menu = new ChartMenuViewBuilder(model, interactor::request).build();
      List<String> descriptions = List.of(
        "Select symbol or instrument, currently ACME",
        "Select interval, currently Daily",
        "Chart type: Line"
      );
      List<String> labels = List.of("ACME", "1D", "");
      ChartMenuItems items = assertInstanceOf(ChartMenuItems.class, menu);

      assertEquals(3, items.getChildren().size());
      for (int index = 0; index < items.getChildren().size(); index++) {
        Button button = assertInstanceOf(Button.class, items.getChildren().get(index));
        assertEquals(Variant.GHOST, button.getVariant());
        assertEquals(index == 2 ? Size.ICON : Size.DEFAULT, button.getSize());
        assertEquals(labels.get(index), button.getText());
        assertEquals(descriptions.get(index), button.getAccessibleText());
      }
      Button chartTypeButton = assertInstanceOf(Button.class, items.getChildren().get(2));
      assertEquals("", chartTypeButton.getText());
      assertSame(LucideIcons.CHART_LINE, assertInstanceOf(LucideIcon.class, chartTypeButton.getGraphic()).getGlyph());
    });
  }

  @Test
  void updatesChartTypeIconAndAccessibleNameWithoutReplacingTheButton() {
    FxTestSupport.runAndWait(() -> {
      ChartMenuModel model = new ChartMenuModel();
      ChartMenuInteractor interactor = new ChartMenuInteractor(model);
      interactor.initialize("ACME", ChartInterval.DAILY);
      Region menu = new ChartMenuViewBuilder(model, interactor::request).build();
      ChartMenuItems items = assertInstanceOf(ChartMenuItems.class, menu);
      Button button = assertInstanceOf(Button.class, items.getChildren().get(2));
      Map<ChartType, LucideIcons> icons = Map.of(
        ChartType.LINE,
        LucideIcons.CHART_LINE,
        ChartType.LINE_WITH_MARKERS,
        LucideIcons.CHART_NETWORK,
        ChartType.STEP_LINE,
        LucideIcons.CHART_LINE,
        ChartType.AREA,
        LucideIcons.CHART_AREA,
        ChartType.BAR,
        LucideIcons.CHART_BAR,
        ChartType.CANDLESTICK,
        LucideIcons.CHART_CANDLESTICK
      );
      Map<ChartType, String> names = Map.of(
        ChartType.LINE,
        "Line",
        ChartType.LINE_WITH_MARKERS,
        "Line with markers",
        ChartType.STEP_LINE,
        "Step line",
        ChartType.AREA,
        "Area",
        ChartType.BAR,
        "Bar",
        ChartType.CANDLESTICK,
        "Candlestick"
      );

      for (ChartType chartType : ChartType.values()) {
        interactor.setChartType(chartType);
        assertSame(button, items.getChildren().get(2));
        assertSame(icons.get(chartType), assertInstanceOf(LucideIcon.class, button.getGraphic()).getGlyph());
        assertEquals("Chart type: " + names.get(chartType), button.getAccessibleText());
      }
    });
  }

  @Test
  void updatesSymbolAndIntervalLabelsWithoutReplacingButtons() {
    FxTestSupport.runAndWait(() -> {
      ChartMenuModel model = new ChartMenuModel();
      ChartMenuInteractor interactor = new ChartMenuInteractor(model);
      interactor.initialize("ACME", ChartInterval.DAILY);
      ChartMenuItems items = assertInstanceOf(
        ChartMenuItems.class,
        new ChartMenuViewBuilder(model, interactor::request).build()
      );
      Button symbolButton = assertInstanceOf(Button.class, items.getChildren().get(0));
      Button intervalButton = assertInstanceOf(Button.class, items.getChildren().get(1));

      interactor.setInstrumentSymbol("BTC-USD");
      interactor.setInterval(ChartInterval.FIVE_MINUTES);
      assertSame(symbolButton, items.getChildren().get(0));
      assertSame(intervalButton, items.getChildren().get(1));
      assertEquals("BTC-USD", symbolButton.getText());
      assertEquals("Select symbol or instrument, currently BTC-USD", symbolButton.getAccessibleText());
      assertEquals("5M", intervalButton.getText());
      assertEquals("Select interval, currently 5 minutes", intervalButton.getAccessibleText());

      interactor.setInterval(ChartInterval.of(7, ChartInterval.Classification.HOURS));
      assertEquals("7H", intervalButton.getText());
      assertEquals("Select interval, currently 7 hours", intervalButton.getAccessibleText());
    });
  }

  @Test
  void keepsButtonHeightAndSpacingWhileTextButtonsGrowToFit() {
    FxTestSupport.runAndWait(() -> {
      ChartMenuModel model = new ChartMenuModel();
      ChartMenuInteractor interactor = new ChartMenuInteractor(model);
      interactor.initialize("BTC-USD", ChartInterval.DAILY);
      ChartMenuItems items = assertInstanceOf(
        ChartMenuItems.class,
        new ChartMenuViewBuilder(model, interactor::request).build()
      );
      StackPane root = new StackPane(items);
      Scene scene = new Scene(root, 200.0, 100.0);
      new AppThemeManager(scene, AppTheme.LIGHT);
      root.applyCss();
      root.layout();

      assertEquals(8.0, items.getSpacing());
      for (var node : items.getChildren()) {
        Button button = assertInstanceOf(Button.class, node);
        assertEquals(32.0, button.prefHeight(-1));
      }
      assertTrue(items.getChildren().get(0).prefWidth(-1) > 32.0);
      assertEquals(32.0, items.getChildren().get(2).prefWidth(-1));
    });
  }
}
