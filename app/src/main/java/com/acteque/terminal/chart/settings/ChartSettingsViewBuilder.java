package com.acteque.terminal.chart.settings;

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

/** Builds the chart-type settings drawer. */
final class ChartSettingsViewBuilder implements Builder<Drawer> {

  private final Drawer drawer = new Drawer();
  private final ToggleGroup chartTypes = new ToggleGroup(Orientation.VERTICAL);
  private final Map<ChartType, ToggleGroupItem> chartTypeItems = new EnumMap<>(ChartType.class);

  ChartSettingsViewBuilder(Consumer<ChartType> chartTypeSelectedHandler) {
    Objects.requireNonNull(chartTypeSelectedHandler, "chartTypeSelectedHandler cannot be null");

    drawer.getStyleClass().add("chart-settings-drawer");
    drawer.setDirection(DrawerDirection.LEFT);
    drawer.setMode(DrawerMode.NON_MODAL);
    drawer.setMaxHeight(Region.USE_PREF_SIZE);
    StackPane.setAlignment(drawer, Pos.TOP_LEFT);

    chartTypes.getStyleClass().add("chart-settings-options");
    chartTypes.setMinWidth(0.0);
    chartTypes.setMaxWidth(Double.MAX_VALUE);
    for (ChartType type : ChartType.values()) {
      ToggleGroupItem item = createChartTypeItem(type, chartTypeSelectedHandler);
      chartTypeItems.put(type, item);
      chartTypes.getChildren().add(item);
    }
    drawer.setContent(new DrawerContent(chartTypes));
    setChartType(ChartType.LINE);
  }

  @Override
  public Drawer build() {
    return drawer;
  }

  void showChartTypes() {
    drawer.show();
    Platform.runLater(() -> {
      if (drawer.isOpen()) {
        chartTypeItems.values().stream().filter(ToggleGroupItem::isSelected).findFirst().ifPresent(Node::requestFocus);
      }
    });
  }

  void close() {
    drawer.close();
  }

  void setChartType(ChartType type) {
    Objects.requireNonNull(type, "type cannot be null");
    chartTypeItems.forEach((candidate, item) -> item.setSelected(candidate == type));
  }

  private static ToggleGroupItem createChartTypeItem(ChartType type, Consumer<ChartType> onSelect) {
    Label name = new Label(ChartTypePresentation.displayName(type));
    name.getStyleClass().add("chart-settings-option-name");
    Label description = new Label(ChartTypePresentation.description(type));
    description.getStyleClass().add("chart-settings-option-description");
    description.setWrapText(true);

    VBox text = new VBox(name, description);
    text.getStyleClass().add("chart-settings-option-text");
    HBox.setHgrow(text, Priority.ALWAYS);

    HBox row = new HBox(new LucideIcon(ChartTypePresentation.icon(type)), text);
    row.getStyleClass().add("chart-settings-option-content");
    ToggleGroupItem item = new ToggleGroupItem("", row);
    item.getStyleClass().add("chart-settings-option");
    item.setAccessibleText(ChartTypePresentation.displayName(type) + ". " + ChartTypePresentation.description(type));
    item.setOnAction(event -> onSelect.accept(type));
    return item;
  }
}
