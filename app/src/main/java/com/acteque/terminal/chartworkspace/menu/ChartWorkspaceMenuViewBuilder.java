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
import com.acteque.terminal.ui.Separator;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.kbd.Kbd;
import com.acteque.terminal.ui.kbd.KbdGroup;
import com.acteque.terminal.ui.popover.Popover;
import com.acteque.terminal.ui.popover.PopoverContent;
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.binding.Bindings;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.image.Image;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundImage;
import javafx.scene.layout.BackgroundPosition;
import javafx.scene.layout.BackgroundRepeat;
import javafx.scene.layout.BackgroundSize;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
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

  /**
   * Creates and connects the workspace-menu JavaFX composition.
   *
   * @param model the observable workspace-menu state
   * @param actionRequestedHandler the menu-item request callback
   * @param splitRequestedHandler the directional-split callback
   */
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
    configureRoot();
    connectModel();
    refreshView();
    ReloadHooks.register(this);
  }

  /** Configures stable sizing for the workspace-menu root. */
  private void configureRoot() {
    root.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
  }

  /** Connects observable menu state to the corresponding view updates. */
  private void connectModel() {
    model.itemsProperty().addListener((ListChangeListener<Item>) ignored -> rebuildItems());
    model.chartTypeProperty().addListener((ignored, previous, current) -> updateChartTypeButton());
  }

  /**
   * Returns the assembled workspace menu.
   *
   * @return the workspace-menu root
   */
  @Override
  public Region build() {
    return root;
  }

  /** Rebuilds reloadable menu controls from the current model state. */
  @Override
  public void refreshView() {
    rebuildItems();
  }

  /**
   * Updates the chart-type button's open-state presentation.
   *
   * @param value true when the chart-type selector is open
   */
  void setChartTypeSelectionOpen(boolean value) {
    chartTypeSelectionOpen = value;
    if (chartTypeButton != null) {
      chartTypeButton.pseudoClassStateChanged(DRAWER_OPEN, value);
    }
  }

  /** Closes menu-owned transient controls. */
  void closeTransientUi() {
    splitPopover.setOpen(false);
  }

  /** Rebuilds the ordered menu controls and closes the split popover. */
  private void rebuildItems() {
    splitPopover.setOpen(false);
    chartTypeButton = null;

    List<Node> items = new ArrayList<>();
    for (Item item : model.getItems()) {
      if (item == Item.SPLIT) {
        items.add(createSplitSeparator());
      }
      items.add(createItem(item));
    }
    root.setItems(items.toArray(Node[]::new));
  }

  /**
   * Creates the separator displayed before the split action.
   *
   * @return the configured vertical separator
   */
  private static Separator createSplitSeparator() {
    Separator separator = new Separator(Orientation.VERTICAL);
    separator.getStyleClass().add("chart-workspace-menu-separator");
    return separator;
  }

  /**
   * Creates a control for one menu item and binds its dynamic presentation.
   *
   * @param item the represented menu item
   * @return the configured menu control
   */
  private Button createItem(Item item) {
    if (item == Item.SPLIT) {
      PopoverTrigger trigger = new PopoverTrigger(
        null,
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
      null,
      Variant.GHOST,
      item == Item.CHART_TYPE || item == Item.CLOSE ? Size.ICON : Size.DEFAULT
    );
    button.setAccessibleText(item.description());
    button.setFocusTraversable(true);
    button.setOnAction(ignored -> actionRequestedHandler.accept(item));
    switch (item) {
      case INSTRUMENT -> {
        button.getStyleClass().add("chart-workspace-menu-instrument");
        button.setSize(Size.ICON);
        button.setTextOverrun(OverrunStyle.ELLIPSIS);
        button
          .textProperty()
          .bind(
            Bindings.createStringBinding(
              () -> model.getInstrumentLogoImage() == null ? model.getInstrumentSymbol() : "",
              model.instrumentLogoImageProperty(),
              model.instrumentSymbolProperty()
            )
          );
        button
          .graphicProperty()
          .bind(
            Bindings.createObjectBinding(
              () -> createInstrumentLogo(model.getInstrumentLogoImage()),
              model.instrumentLogoImageProperty()
            )
          );
        button
          .styleProperty()
          .bind(
            Bindings.createStringBinding(
              () -> "-chart-workspace-menu-instrument-ring: " + cssColor(model.getIdentifierColor()) + ";",
              model.identifierColorProperty()
            )
          );
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

  /**
   * Creates a circular region displaying a loaded instrument logo.
   *
   * @param image the loaded logo image, or null
   * @return the clipped logo region, or null when no image is loaded
   */
  private static Node createInstrumentLogo(Image image) {
    if (image == null) {
      return null;
    }
    Region logo = new Region();
    logo.getStyleClass().add("chart-workspace-menu-instrument-logo");
    logo.setBackground(
      new Background(
        new BackgroundImage(
          image,
          BackgroundRepeat.NO_REPEAT,
          BackgroundRepeat.NO_REPEAT,
          BackgroundPosition.CENTER,
          new BackgroundSize(BackgroundSize.AUTO, BackgroundSize.AUTO, false, false, false, true)
        )
      )
    );
    Circle clip = new Circle();
    clip.centerXProperty().bind(logo.widthProperty().divide(2.0));
    clip.centerYProperty().bind(logo.heightProperty().divide(2.0));
    clip.radiusProperty().bind(Bindings.min(logo.widthProperty(), logo.heightProperty()).divide(2.0));
    logo.setClip(clip);
    return logo;
  }

  /**
   * Converts a JavaFX color to a locale-independent CSS color expression.
   *
   * @param color the color to convert
   * @return the CSS rgba expression
   */
  private static String cssColor(Color color) {
    return String.format(
      Locale.ROOT,
      "rgba(%.10f%%, %.10f%%, %.10f%%, %.10f)",
      color.getRed() * 100.0,
      color.getGreen() * 100.0,
      color.getBlue() * 100.0,
      color.getOpacity()
    );
  }

  /**
   * Creates the directional split-action popover.
   *
   * @return the configured split popover
   */
  private Popover createSplitPopover() {
    VBox actions = new VBox();
    actions.getStyleClass().add("chart-workspace-split-actions");
    addSplitAction(actions, "Split right", "L", ChartSplitDirection.RIGHT);
    addSplitAction(actions, "Split down", "J", ChartSplitDirection.BOTTOM);
    addSplitAction(actions, "Split up", "K", ChartSplitDirection.TOP);
    addSplitAction(actions, "Split left", "H", ChartSplitDirection.LEFT);

    PopoverContent content = new PopoverContent(actions);
    content.getStyleClass().add("chart-workspace-split-popover");
    return new Popover(content);
  }

  /**
   * Adds one directional split action to the split popover.
   *
   * @param actions the split-action container
   * @param label the action label and accessible text
   * @param key the displayed shortcut key
   * @param direction the requested chart split direction
   */
  private void addSplitAction(VBox actions, String label, String key, ChartSplitDirection direction) {
    Label actionLabel = new Label(label);
    actionLabel.getStyleClass().add("chart-workspace-split-action-label");
    Region spacer = new Region();
    HBox.setHgrow(spacer, Priority.ALWAYS);
    HBox actionContent = new HBox(actionLabel, spacer, new KbdGroup(new Kbd("⌘"), new Kbd(key)));
    actionContent.getStyleClass().add("chart-workspace-split-action-content");

    Button button = new Button(null, actionContent, Variant.GHOST, Size.DEFAULT);
    button.getStyleClass().add("chart-workspace-split-action");
    button.setAccessibleText(label);
    button.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
    button.setFocusTraversable(true);
    button.setMaxWidth(Double.MAX_VALUE);
    actionContent
      .prefWidthProperty()
      .bind(
        Bindings.createDoubleBinding(
          () -> Math.max(0.0, button.getWidth() - button.getInsets().getLeft() - button.getInsets().getRight()),
          button.widthProperty(),
          button.insetsProperty()
        )
      );
    button.setOnAction(ignored -> {
      splitPopover.setOpen(false);
      splitRequestedHandler.accept(direction);
    });
    actions.getChildren().add(button);
  }

  /** Updates the chart-type control to represent the active chart type. */
  private void updateChartTypeButton() {
    if (chartTypeButton == null) {
      return;
    }
    ChartType chartType = model.getChartType();
    chartTypeButton.setGraphic(new LucideIcon(ChartTypePresentation.icon(chartType)));
    chartTypeButton.setAccessibleText("Chart type: " + ChartTypePresentation.displayName(chartType));
  }
}
