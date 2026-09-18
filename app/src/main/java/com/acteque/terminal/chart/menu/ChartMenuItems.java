package com.acteque.terminal.chart.menu;

import com.acteque.terminal.ui.Button;
import javafx.scene.layout.HBox;

/** Houses the chart menu's independently styled buttons. */
final class ChartMenuItems extends HBox {

  ChartMenuItems() {
    getStyleClass().add("chart-menu");
  }

  void setButtons(Button... buttons) {
    getChildren().setAll(buttons);
  }
}
