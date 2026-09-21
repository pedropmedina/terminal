package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.ui.Button;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.scene.layout.Background;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;

/** Houses the workspace menu's independently styled buttons and active-chart accent. */
final class ChartWorkspaceMenuItems extends StackPane {

  private final HBox buttons = new HBox();
  private final Region identifier = new Region();

  ChartWorkspaceMenuItems() {
    getStyleClass().add("chart-workspace-menu");
    buttons.getStyleClass().add("chart-workspace-menu-items");
    identifier.getStyleClass().add("chart-workspace-menu-identifier");
    identifier.setManaged(false);
    identifier.setMouseTransparent(true);
    StackPane.setAlignment(identifier, Pos.TOP_LEFT);

    Rectangle clip = new Rectangle();
    clip.widthProperty().bind(widthProperty());
    clip.heightProperty().bind(heightProperty());
    clip
      .arcWidthProperty()
      .bind(
        Bindings.createDoubleBinding(() -> backgroundRadii().getTopLeftHorizontalRadius() * 2.0, backgroundProperty())
      );
    clip
      .arcHeightProperty()
      .bind(
        Bindings.createDoubleBinding(() -> backgroundRadii().getTopLeftVerticalRadius() * 2.0, backgroundProperty())
      );
    setClip(clip);

    getChildren().setAll(buttons, identifier);
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    identifier.resizeRelocate(0.0, 0.0, identifier.prefWidth(-1.0), getHeight());
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

  private CornerRadii backgroundRadii() {
    Background background = getBackground();
    return background == null || background.getFills().isEmpty()
      ? CornerRadii.EMPTY
      : background.getFills().getFirst().getRadii();
  }
}
