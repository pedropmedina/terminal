package com.acteque.terminal.chartworkspace.menu;

import javafx.scene.Node;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Houses the workspace menu's independently styled buttons. */
final class ChartWorkspaceMenuItems extends StackPane {

  private final HBox buttons = new HBox();

  /** Creates the styled menu container and its horizontal item row. */
  ChartWorkspaceMenuItems() {
    getStyleClass().add("chart-workspace-menu");
    buttons.getStyleClass().add("chart-workspace-menu-items");
    getChildren().setAll(buttons);
  }

  /**
   * Replaces the controls displayed in the menu row.
   *
   * @param values the menu controls in display order
   */
  void setItems(Node... values) {
    buttons.getChildren().setAll(values);
  }

  /**
   * Returns the horizontal item row.
   *
   * @return the menu's button container
   */
  HBox getButtons() {
    return buttons;
  }
}
