package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart menu. */
final class ChartMenuViewBuilder implements Builder<Region>, ReloadTarget {

  private static final double TOP_MARGIN = 12.0;

  private final ChartMenuModel model;
  private final Consumer<Item> actionRequestedHandler;
  private final ChartMenuItems root = new ChartMenuItems();
  private Button chartTypeButton;

  ChartMenuViewBuilder(ChartMenuModel model, Consumer<Item> actionRequestedHandler) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.actionRequestedHandler = Objects.requireNonNull(
      actionRequestedHandler,
      "actionRequestedHandler cannot be null"
    );

    root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    StackPane.setAlignment(root, Pos.TOP_CENTER);
    StackPane.setMargin(root, new Insets(TOP_MARGIN, 0.0, 0.0, 0.0));

    model.itemsProperty().addListener((ListChangeListener<Item>) ignored -> rebuildItems());
    model.chartTypeProperty().addListener((ignored, previous, current) -> updateChartTypeButton());
    refreshView();
    ReloadHooks.register(this);
  }

  @Override
  public Region build() {
    return root;
  }

  @Override
  public void refreshView() {
    rebuildItems();
  }

  private void rebuildItems() {
    Button[] buttons = model
      .getItems()
      .stream()
      .map(this::createItem)
      .toArray(Button[]::new);
    root.setButtons(buttons);
  }

  private Button createItem(Item item) {
    Button button = new Button("", Variant.GHOST, item == Item.CHART_TYPE ? Size.ICON : Size.DEFAULT);
    button.setAccessibleText(item.description());
    button.setFocusTraversable(true);
    button.setOnAction(ignored -> actionRequestedHandler.accept(item));
    switch (item) {
      case INSTRUMENT -> {
        button.textProperty().bind(model.instrumentSymbolProperty());
        button
          .accessibleTextProperty()
          .bind(
            Bindings.createStringBinding(
              () -> "Select symbol or instrument, currently " + model.getInstrumentSymbol(),
              model.instrumentSymbolProperty()
            )
          );
      }
      case INTERVAL -> {
        button
          .textProperty()
          .bind(Bindings.createStringBinding(() -> model.getInterval().name(), model.intervalProperty()));
        button
          .accessibleTextProperty()
          .bind(
            Bindings.createStringBinding(
              () -> "Select interval, currently " + ChartIntervalText.displayName(model.getInterval()),
              model.intervalProperty()
            )
          );
      }
      case CHART_TYPE -> {
        chartTypeButton = button;
        updateChartTypeButton();
      }
    }
    return button;
  }

  private void updateChartTypeButton() {
    if (chartTypeButton == null) {
      return;
    }
    ChartType chartType = model.getChartType();
    chartTypeButton.setGraphic(new LucideIcon(iconFor(chartType)));
    chartTypeButton.setAccessibleText("Chart type: " + displayName(chartType));
  }

  private static LucideIcons iconFor(ChartType chartType) {
    return switch (chartType) {
      case LINE, STEP_LINE -> LucideIcons.CHART_LINE;
      case LINE_WITH_MARKERS -> LucideIcons.CHART_NETWORK;
      case AREA -> LucideIcons.CHART_AREA;
      case CANDLESTICK -> LucideIcons.CHART_CANDLESTICK;
    };
  }

  private static String displayName(ChartType chartType) {
    return switch (chartType) {
      case LINE -> "Line";
      case LINE_WITH_MARKERS -> "Line with markers";
      case STEP_LINE -> "Step line";
      case AREA -> "Area";
      case CANDLESTICK -> "Candlestick";
    };
  }
}
