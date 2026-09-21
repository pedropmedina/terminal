package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.ChartTypePresentation;
import com.acteque.terminal.chartworkspace.menu.ChartWorkspaceMenuModel.Item;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.popover.Popover;
import com.acteque.terminal.ui.popover.PopoverContent;
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the workspace menu. */
final class ChartWorkspaceMenuViewBuilder implements Builder<Region>, ReloadTarget {

  private static final PseudoClass DRAWER_OPEN = PseudoClass.getPseudoClass("drawer-open");

  private final ChartWorkspaceMenuModel model;
  private final Consumer<Item> actionRequestedHandler;
  private final Consumer<ChartSplitDirection> splitRequestedHandler;
  private final ChartWorkspaceMenuItems root = new ChartWorkspaceMenuItems();
  private final Popover splitPopover;
  private Button chartTypeButton;
  private boolean chartTypeSelectionOpen;

  ChartWorkspaceMenuViewBuilder(
    ChartWorkspaceMenuModel model,
    Consumer<Item> actionRequestedHandler,
    Consumer<ChartSplitDirection> splitRequestedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.actionRequestedHandler = Objects.requireNonNull(
      actionRequestedHandler,
      "actionRequestedHandler cannot be null"
    );
    this.splitRequestedHandler = Objects.requireNonNull(splitRequestedHandler, "splitRequestedHandler cannot be null");
    splitPopover = createSplitPopover();

    root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    root
      .getIdentifier()
      .backgroundProperty()
      .bind(
        Bindings.createObjectBinding(
          () ->
            new Background(new BackgroundFill(model.identifierColorProperty().get(), CornerRadii.EMPTY, Insets.EMPTY)),
          model.identifierColorProperty()
        )
      );
    root.getIdentifier().visibleProperty().bind(model.identifierVisibleProperty());

    model.itemsProperty().addListener((ListChangeListener<Item>) ignored -> rebuildItems());
    model.chartTypeProperty().addListener((ignored, previous, current) -> updateChartTypeButton());
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

  void setChartTypeSelectionOpen(boolean value) {
    chartTypeSelectionOpen = value;
    if (chartTypeButton != null) {
      chartTypeButton.pseudoClassStateChanged(DRAWER_OPEN, value);
    }
  }

  void closeTransientUi() {
    splitPopover.setOpen(false);
  }

  private void rebuildItems() {
    splitPopover.setOpen(false);
    Button[] buttons = model
      .getItems()
      .stream()
      .map(this::createItem)
      .toArray(Button[]::new);
    root.setButtons(buttons);
  }

  private Button createItem(Item item) {
    if (item == Item.SPLIT) {
      PopoverTrigger trigger = new PopoverTrigger(
        "",
        new LucideIcon(LucideIcons.SQUARE_SPLIT_HORIZONTAL),
        Variant.GHOST,
        Size.ICON,
        splitPopover
      );
      trigger.getStyleClass().add("chart-workspace-menu-split");
      trigger.setAccessibleText(item.description());
      trigger.setFocusTraversable(true);
      return trigger;
    }

    Button button = new Button(
      "",
      Variant.GHOST,
      item == Item.CHART_TYPE || item == Item.CLOSE ? Size.ICON : Size.DEFAULT
    );
    button.setAccessibleText(item.description());
    button.setFocusTraversable(true);
    button.setOnAction(ignored -> actionRequestedHandler.accept(item));
    switch (item) {
      case INSTRUMENT -> {
        button.textProperty().bind(model.instrumentSymbolProperty());
        button
          .accessibleTextProperty()
          .bind(
            Bindings.createStringBinding(
              () -> "Select symbol or instrument, currently " + model.getInstrumentSymbol(),
              model.instrumentSymbolProperty()
            )
          );
      }
      case INTERVAL -> {
        button
          .textProperty()
          .bind(Bindings.createStringBinding(() -> model.getInterval().name(), model.intervalProperty()));
        button
          .accessibleTextProperty()
          .bind(
            Bindings.createStringBinding(
              () -> "Select interval, currently " + ChartIntervalText.displayName(model.getInterval()),
              model.intervalProperty()
            )
          );
      }
      case CHART_TYPE -> {
        chartTypeButton = button;
        button.getStyleClass().add("chart-workspace-menu-chart-type");
        button.pseudoClassStateChanged(DRAWER_OPEN, chartTypeSelectionOpen);
        updateChartTypeButton();
      }
      case CLOSE -> {
        button.getStyleClass().add("chart-workspace-menu-close");
        button.setGraphic(new LucideIcon(LucideIcons.X));
      }
      case SPLIT -> throw new IllegalStateException("Split item must use its popover trigger");
    }
    return button;
  }

  private Popover createSplitPopover() {
    GridPane actions = new GridPane();
    actions.getStyleClass().add("chart-workspace-split-actions");
    addSplitAction(actions, "Top", ChartSplitDirection.TOP, 1, 0);
    addSplitAction(actions, "Left", ChartSplitDirection.LEFT, 0, 1);
    addSplitAction(actions, "Right", ChartSplitDirection.RIGHT, 2, 1);
    addSplitAction(actions, "Bottom", ChartSplitDirection.BOTTOM, 1, 2);

    PopoverContent content = new PopoverContent(actions);
    content.getStyleClass().add("chart-workspace-split-popover");
    return new Popover(content);
  }

  private void addSplitAction(GridPane actions, String label, ChartSplitDirection direction, int column, int row) {
    Button button = new Button(label, Variant.GHOST, Size.DEFAULT);
    button.getStyleClass().add("chart-workspace-split-action");
    button.setAccessibleText("Create chart to the " + label.toLowerCase());
    button.setFocusTraversable(true);
    button.setMaxWidth(Double.MAX_VALUE);
    button.setOnAction(ignored -> {
      splitPopover.setOpen(false);
      splitRequestedHandler.accept(direction);
    });
    actions.add(button, column, row);
  }

  private void updateChartTypeButton() {
    if (chartTypeButton == null) {
      return;
    }
    ChartType chartType = model.getChartType();
    chartTypeButton.setGraphic(new LucideIcon(ChartTypePresentation.icon(chartType)));
    chartTypeButton.setAccessibleText("Chart type: " + ChartTypePresentation.displayName(chartType));
  }
}
