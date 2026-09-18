package com.acteque.terminal.chart.menu;

import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.buttongroup.ButtonGroup;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart menu. */
final class ChartMenuViewBuilder implements Builder<Region>, ReloadTarget {

  private static final double TOP_MARGIN = 12.0;

  private final ChartMenuModel model;
  private final Consumer<Item> actionRequestedHandler;
  private final HBox root = new HBox();

  ChartMenuViewBuilder(ChartMenuModel model, Consumer<Item> actionRequestedHandler) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.actionRequestedHandler = Objects.requireNonNull(
      actionRequestedHandler,
      "actionRequestedHandler cannot be null"
    );

    root.getStyleClass().add("chart-menu");
    root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    StackPane.setAlignment(root, Pos.TOP_CENTER);
    StackPane.setMargin(root, new Insets(TOP_MARGIN, 0.0, 0.0, 0.0));

    model.itemsProperty().addListener((ListChangeListener<Item>) ignored -> rebuildItems());
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
    root.getChildren().setAll(new ButtonGroup(buttons));
  }

  private Button createItem(Item item) {
    Button button = new Button(item.label(), Variant.GHOST, Size.ICON);
    button.setAccessibleText(item.description());
    button.setFocusTraversable(true);
    button.setOnAction(ignored -> actionRequestedHandler.accept(item));
    return button;
  }
}
