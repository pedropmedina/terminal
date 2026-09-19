package com.acteque.terminal.chart.inspector;

import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.ChartTypePresentation;
import com.acteque.terminal.ui.drawer.Drawer;
import com.acteque.terminal.ui.drawer.DrawerContent;
import com.acteque.terminal.ui.drawer.DrawerDirection;
import com.acteque.terminal.ui.drawer.DrawerMode;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.togglegroup.ToggleGroupItem;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Builder;

/** Builds the contextual chart inspector drawer. */
final class ChartInspectorViewBuilder implements Builder<Drawer> {

  private final ChartInspectorModel model;
  private final Drawer drawer = new Drawer();
  private final ToggleGroup chartTypes = new ToggleGroup(Orientation.VERTICAL);
  private final Map<ChartType, ToggleGroupItem> chartTypeItems = new EnumMap<>(ChartType.class);

  ChartInspectorViewBuilder(
    ChartInspectorModel model,
    Consumer<ChartType> chartTypeSelectedHandler,
    Consumer<Boolean> openChangedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    Objects.requireNonNull(chartTypeSelectedHandler, "chartTypeSelectedHandler cannot be null");
    Objects.requireNonNull(openChangedHandler, "openChangedHandler cannot be null");

    drawer.getStyleClass().add("chart-inspector-drawer");
    drawer.setDirection(DrawerDirection.LEFT);
    drawer.setMode(DrawerMode.NON_MODAL);
    drawer.setMaxHeight(Region.USE_PREF_SIZE);
    StackPane.setAlignment(drawer, Pos.TOP_LEFT);

    chartTypes.getStyleClass().add("chart-inspector-options");
    chartTypes.setMinWidth(0.0);
    chartTypes.setMaxWidth(Double.MAX_VALUE);
    for (ChartType type : ChartType.values()) {
      ToggleGroupItem item = createChartTypeItem(type, chartTypeSelectedHandler);
      chartTypeItems.put(type, item);
      chartTypes.getChildren().add(item);
    }
    drawer.setContent(new DrawerContent(chartTypes));

    model.chartTypeProperty().addListener((ignored, previous, current) -> displayChartType(current));
    model.openProperty().addListener((ignored, wasOpen, isOpen) -> displayOpenState(isOpen));
    drawer.openProperty().addListener((ignored, wasOpen, isOpen) -> openChangedHandler.accept(isOpen));
    displayChartType(model.getChartType());
    displayOpenState(model.isOpen());
  }

  @Override
  public Drawer build() {
    return drawer;
  }

  private void displayChartType(ChartType type) {
    Objects.requireNonNull(type, "type cannot be null");
    chartTypeItems.forEach((candidate, item) -> item.setSelected(candidate == type));
  }

  private void displayOpenState(boolean open) {
    drawer.setOpen(open);
    if (open) {
      Platform.runLater(() -> {
        if (drawer.isOpen()) {
          chartTypeItems
            .values()
            .stream()
            .filter(ToggleGroupItem::isSelected)
            .findFirst()
            .ifPresent(Node::requestFocus);
        }
      });
    }
  }

  private ToggleGroupItem createChartTypeItem(ChartType type, Consumer<ChartType> onSelect) {
    Label name = new Label(ChartTypePresentation.displayName(type));
    name.getStyleClass().add("chart-inspector-option-name");
    Label description = new Label(ChartTypePresentation.description(type));
    description.getStyleClass().add("chart-inspector-option-description");
    description.setWrapText(true);

    VBox text = new VBox(name, description);
    text.getStyleClass().add("chart-inspector-option-text");
    HBox.setHgrow(text, Priority.ALWAYS);

    HBox row = new HBox(new LucideIcon(ChartTypePresentation.icon(type)), text);
    row.getStyleClass().add("chart-inspector-option-content");
    ToggleGroupItem item = new ToggleGroupItem("", row);
    item.getStyleClass().add("chart-inspector-option");
    item.setAccessibleText(ChartTypePresentation.displayName(type) + ". " + ChartTypePresentation.description(type));
    item.setOnAction(event -> {
      onSelect.accept(type);
      displayChartType(model.getChartType());
    });
    return item;
  }
}
