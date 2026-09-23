package com.acteque.terminal.chartworkspace.menu;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Houses the workspace menu's independently styled buttons. */
final class ChartWorkspaceMenuItems extends StackPane {

  private final HBox buttons = new HBox();

  ChartWorkspaceMenuItems() {
    getStyleClass().add("chart-workspace-menu");
    buttons.getStyleClass().add("chart-workspace-menu-items");
    getChildren().setAll(buttons);
  }

  void setItems(Node... values) {
    buttons.getChildren().setAll(values);
  }

  HBox getButtons() {
    return buttons;
  }
}
