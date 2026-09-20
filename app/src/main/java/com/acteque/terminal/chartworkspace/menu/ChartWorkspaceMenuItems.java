package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.ui.Button;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Houses the workspace menu's independently styled buttons and active-chart accent. */
final class ChartWorkspaceMenuItems extends StackPane {

  private final HBox buttons = new HBox();
  private final Region identifier = new Region();

  ChartWorkspaceMenuItems() {
    getStyleClass().add("chart-workspace-menu");
    buttons.getStyleClass().add("chart-workspace-menu-items");
    identifier.getStyleClass().add("chart-workspace-menu-identifier");
    identifier.setMouseTransparent(true);
    StackPane.setAlignment(identifier, Pos.CENTER_LEFT);
    getChildren().setAll(buttons, identifier);
  }

  Region getIdentifier() {
    return identifier;
  }

  void setButtons(Button... values) {
    buttons.getChildren().setAll(values);
  }

  HBox getButtons() {
    return buttons;
  }
}
