package com.acteque.terminal.chart;

import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Presentational controls shown over the top of the price chart. */
final class ChartMenu extends HBox implements RefreshableView {

  private static final double TOP_MARGIN = 12.0;

  ChartMenu() {
    getStyleClass().add("chart-menu");
    refreshView();
    ChartReloadHooks.register(this);
  }

  @Override
  public void refreshView() {
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    StackPane.setAlignment(this, Pos.TOP_CENTER);
    StackPane.setMargin(this, new Insets(TOP_MARGIN, 0.0, 0.0, 0.0));
    getChildren().setAll(
      createItem("S", "Symbol or instrument"),
      createItem("I", "Interval"),
      createItem("C", "Chart type")
    );
  }

  private static Button createItem(String label, String description) {
    Button item = new Button(label);
    item.getStyleClass().add("icon-button");
    item.setAccessibleText(description);
    item.setFocusTraversable(true);
    return item;
  }
}
