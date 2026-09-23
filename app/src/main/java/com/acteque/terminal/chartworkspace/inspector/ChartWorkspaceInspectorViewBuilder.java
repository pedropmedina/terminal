package com.acteque.terminal.chartworkspace.inspector;

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
final class ChartWorkspaceInspectorViewBuilder implements Builder<Drawer> {

  private final ChartWorkspaceInspectorModel model;
  private final Drawer drawer = new Drawer();
  private final ToggleGroup chartTypes = new ToggleGroup(Orientation.VERTICAL);
  private final Map<ChartType, ToggleGroupItem> chartTypeItems = new EnumMap<>(ChartType.class);

  /**
   * Creates and connects the inspector's JavaFX composition.
   *
   * @param model the observable inspector state
   * @param chartTypeSelectedHandler the chart-type selection callback
   * @param openChangedHandler the drawer-state callback
   */
  ChartWorkspaceInspectorViewBuilder(
    ChartWorkspaceInspectorModel model,
    Consumer<ChartType> chartTypeSelectedHandler,
    Consumer<Boolean> openChangedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    Consumer<ChartType> validatedChartTypeSelectedHandler = Objects.requireNonNull(
      chartTypeSelectedHandler,
      "chartTypeSelectedHandler cannot be null"
    );
    Consumer<Boolean> validatedOpenChangedHandler = Objects.requireNonNull(
      openChangedHandler,
      "openChangedHandler cannot be null"
    );

    configureDrawer();
    composeChartTypes(validatedChartTypeSelectedHandler);
    connectComponents(validatedOpenChangedHandler);

    displayChartType(model.getChartType());
    displayOpenState(model.isOpen());
  }

  /**
   * Returns the assembled inspector drawer.
   *
   * @return the inspector drawer
   */
  @Override
  public Drawer build() {
    return drawer;
  }

  /** Configures the inspector drawer's structural behavior and workspace alignment. */
  private void configureDrawer() {
    drawer.getStyleClass().add("chart-workspace-inspector-drawer");
    drawer.setDirection(DrawerDirection.LEFT);
    drawer.setMode(DrawerMode.NON_MODAL);
    drawer.setMaxHeight(Region.USE_PREF_SIZE);
    StackPane.setAlignment(drawer, Pos.TOP_LEFT);
  }

  /**
   * Composes the available chart-type choices into the drawer.
   *
   * @param chartTypeSelectedHandler the chart-type selection callback
   */
  private void composeChartTypes(Consumer<ChartType> chartTypeSelectedHandler) {
    chartTypes.getStyleClass().add("chart-workspace-inspector-options");
    chartTypes.setMinWidth(0.0);
    chartTypes.setMaxWidth(Double.MAX_VALUE);

    for (ChartType type : ChartType.values()) {
      ToggleGroupItem item = createChartTypeItem(type, chartTypeSelectedHandler);
      chartTypeItems.put(type, item);
      chartTypes.getChildren().add(item);
    }

    drawer.setContent(new DrawerContent(chartTypes));
  }

  /**
   * Connects observable state to the drawer and forwards drawer state changes to the interactor.
   *
   * @param openChangedHandler the drawer-state callback
   */
  private void connectComponents(Consumer<Boolean> openChangedHandler) {
    model.chartTypeProperty().addListener((ignored, previous, current) -> displayChartType(current));
    model.openProperty().addListener((ignored, wasOpen, isOpen) -> displayOpenState(isOpen));
    drawer.openProperty().addListener((ignored, wasOpen, isOpen) -> openChangedHandler.accept(isOpen));
  }

  /**
   * Displays the current chart-type selection.
   *
   * @param type the chart type to display as selected
   */
  private void displayChartType(ChartType type) {
    Objects.requireNonNull(type, "type cannot be null");
    chartTypeItems.forEach((candidate, item) -> item.setSelected(candidate == type));
  }

  /**
   * Mirrors the model's open state into the drawer and focuses its selected item when opened.
   *
   * @param open true to open the drawer
   */
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

  /**
   * Creates a chart-type option that forwards its user selection to the interactor.
   *
   * @param type the chart type represented by the option
   * @param onSelect the chart-type selection callback
   * @return the configured chart-type option
   */
  private ToggleGroupItem createChartTypeItem(ChartType type, Consumer<ChartType> onSelect) {
    Label name = new Label(ChartTypePresentation.displayName(type));
    name.getStyleClass().add("chart-workspace-inspector-option-name");

    Label description = new Label(ChartTypePresentation.description(type));
    description.getStyleClass().add("chart-workspace-inspector-option-description");
    description.setWrapText(true);

    VBox text = new VBox(name, description);
    text.getStyleClass().add("chart-workspace-inspector-option-text");
    HBox.setHgrow(text, Priority.ALWAYS);

    HBox row = new HBox(new LucideIcon(ChartTypePresentation.icon(type)), text);
    row.getStyleClass().add("chart-workspace-inspector-option-content");

    ToggleGroupItem item = new ToggleGroupItem("", row);
    item.getStyleClass().add("chart-workspace-inspector-option");
    item.setAccessibleText(ChartTypePresentation.displayName(type) + ". " + ChartTypePresentation.description(type));
    item.setOnAction(event -> {
      onSelect.accept(type);
      displayChartType(model.getChartType());
    });
    return item;
  }
}
