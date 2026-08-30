package com.acteque.terminal.chart;

import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.UiStyles;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;

/** Presentational controls shown over the top of the price chart. */
final class ChartMenu extends HBox implements RefreshableView {

  private static final double TOP_MARGIN = 12.0;
  private static final double ITEM_SIZE = 32.0;

  ChartMenu() {
    refreshView();
    ChartReloadHooks.register(this);
  }

  @Override
  public void refreshView() {
    setSpacing(2.0);
    setAlignment(Pos.CENTER);
    setPadding(new Insets(4.0));
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setBackground(UiStyles.SURFACE_BACKGROUND);
    StackPane.setAlignment(this, Pos.TOP_CENTER);
    StackPane.setMargin(this, new Insets(TOP_MARGIN, 0.0, 0.0, 0.0));
    getChildren().setAll(
      createItem("S", "Symbol or instrument"),
      createItem("I", "Interval"),
      createItem("C", "Chart type")
    );
  }

  private static StackPane createItem(String label, String description) {
    Text text = new Text(label);
    text.setFill(Color.rgb(45, 48, 55));
    text.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13.0));

    StackPane item = new StackPane(text);
    item.setMinSize(ITEM_SIZE, ITEM_SIZE);
    item.setPrefSize(ITEM_SIZE, ITEM_SIZE);
    item.setMaxSize(ITEM_SIZE, ITEM_SIZE);
    item.setAccessibleText(description);
    item.setBackground(Background.EMPTY);
    item
      .hoverProperty()
      .addListener((ignored, wasHovered, isHovered) ->
        item.setBackground(isHovered ? createHoveredItemBackground() : Background.EMPTY)
      );
    return item;
  }

  private static Background createHoveredItemBackground() {
    return new Background(new BackgroundFill(Color.rgb(221, 224, 230), new CornerRadii(5.0), Insets.EMPTY));
  }
}
