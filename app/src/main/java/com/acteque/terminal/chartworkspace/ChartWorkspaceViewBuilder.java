package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.drawer.Drawer;
import com.acteque.terminal.ui.resizable.ResizableHandle;
import com.acteque.terminal.ui.resizable.ResizablePanel;
import com.acteque.terminal.ui.resizable.ResizablePanelGroup;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.util.Builder;

/** Builds the recursively split JavaFX view for the chart workspace. */
final class ChartWorkspaceViewBuilder implements Builder<StackPane> {

  private static final PseudoClass ACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("workspace-active");
  private static final List<NavigationShortcut> NAVIGATION_SHORTCUTS = List.of(
    new NavigationShortcut(control(KeyCode.L), ChartWorkspaceNavigationDirection.RIGHT),
    new NavigationShortcut(control(KeyCode.J), ChartWorkspaceNavigationDirection.BELOW),
    new NavigationShortcut(control(KeyCode.K), ChartWorkspaceNavigationDirection.ABOVE),
    new NavigationShortcut(control(KeyCode.H), ChartWorkspaceNavigationDirection.LEFT)
  );
  private static final List<SplitShortcut> SPLIT_SHORTCUTS = List.of(
    new SplitShortcut(shortcut(KeyCode.L), ChartSplitDirection.RIGHT),
    new SplitShortcut(shortcut(KeyCode.J), ChartSplitDirection.BOTTOM),
    new SplitShortcut(shortcut(KeyCode.K), ChartSplitDirection.TOP),
    new SplitShortcut(shortcut(KeyCode.H), ChartSplitDirection.LEFT)
  );
  private static final KeyCombination CLOSE_SHORTCUT = new KeyCodeCombination(KeyCode.W, KeyCombination.META_DOWN);

  private final ChartWorkspaceModel model;
  private final Consumer<Chart> chartActivatedHandler;
  private final Consumer<ChartWorkspaceNavigationDirection> navigationRequestedHandler;
  private final Consumer<ChartSplitDirection> splitRequestedHandler;
  private final Runnable closeRequestedHandler;
  private final boolean macOs;
  private final Map<Chart, ChartContainer> chartContainers = new IdentityHashMap<>();
  private final StackPane root = new StackPane();
  private final StackPane chartLayer = new StackPane();
  private final StackPane menuOverlay;
  private final Drawer inspectorDrawer;

  /**
   * Creates a workspace view using the current operating system for shortcut behavior.
   *
   * @param model the observable workspace state
   * @param menu the shared workspace menu
   * @param inspectorDrawer the shared chart inspector
   * @param intervalSelectionDialog the shared interval-selection dialog
   * @param instrumentSearchDialog the shared instrument-search dialog
   * @param chartActivatedHandler the chart-activation callback
   * @param navigationRequestedHandler the directional-navigation callback
   * @param splitRequestedHandler the directional-split callback
   * @param closeRequestedHandler the active-chart close callback
   */
  ChartWorkspaceViewBuilder(
    ChartWorkspaceModel model,
    Node menu,
    Drawer inspectorDrawer,
    Dialog intervalSelectionDialog,
    Dialog instrumentSearchDialog,
    Consumer<Chart> chartActivatedHandler,
    Consumer<ChartWorkspaceNavigationDirection> navigationRequestedHandler,
    Consumer<ChartSplitDirection> splitRequestedHandler,
    Runnable closeRequestedHandler
  ) {
    this(
      model,
      menu,
      inspectorDrawer,
      intervalSelectionDialog,
      instrumentSearchDialog,
      isMacOs(),
      chartActivatedHandler,
      navigationRequestedHandler,
      splitRequestedHandler,
      closeRequestedHandler
    );
  }

  /**
   * Creates a workspace view with injectable operating-system shortcut behavior.
   *
   * @param model the observable workspace state
   * @param menu the shared workspace menu
   * @param inspectorDrawer the shared chart inspector
   * @param intervalSelectionDialog the shared interval-selection dialog
   * @param instrumentSearchDialog the shared instrument-search dialog
   * @param macOs true to enable macOS-only navigation and close shortcuts
   * @param chartActivatedHandler the chart-activation callback
   * @param navigationRequestedHandler the directional-navigation callback
   * @param splitRequestedHandler the directional-split callback
   * @param closeRequestedHandler the active-chart close callback
   */
  ChartWorkspaceViewBuilder(
    ChartWorkspaceModel model,
    Node menu,
    Drawer inspectorDrawer,
    Dialog intervalSelectionDialog,
    Dialog instrumentSearchDialog,
    boolean macOs,
    Consumer<Chart> chartActivatedHandler,
    Consumer<ChartWorkspaceNavigationDirection> navigationRequestedHandler,
    Consumer<ChartSplitDirection> splitRequestedHandler,
    Runnable closeRequestedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    Node validatedMenu = Objects.requireNonNull(menu, "menu cannot be null");
    this.inspectorDrawer = Objects.requireNonNull(inspectorDrawer, "inspectorDrawer cannot be null");
    Dialog validatedIntervalSelectionDialog = Objects.requireNonNull(
      intervalSelectionDialog,
      "intervalSelectionDialog cannot be null"
    );
    Dialog validatedInstrumentSearchDialog = Objects.requireNonNull(
      instrumentSearchDialog,
      "instrumentSearchDialog cannot be null"
    );
    this.macOs = macOs;
    this.chartActivatedHandler = Objects.requireNonNull(chartActivatedHandler, "chartActivatedHandler cannot be null");
    this.navigationRequestedHandler = Objects.requireNonNull(
      navigationRequestedHandler,
      "navigationRequestedHandler cannot be null"
    );
    this.splitRequestedHandler = Objects.requireNonNull(splitRequestedHandler, "splitRequestedHandler cannot be null");
    this.closeRequestedHandler = Objects.requireNonNull(closeRequestedHandler, "closeRequestedHandler cannot be null");

    menuOverlay = new StackPane(validatedMenu);
    configureMenuOverlay();
    configureRoot(validatedIntervalSelectionDialog, validatedInstrumentSearchDialog);
    connectModel();
    rebuildCharts();
  }

  /**
   * Returns the assembled workspace root.
   *
   * @return the workspace root
   */
  @Override
  public StackPane build() {
    return root;
  }

  /** Configures menu overlay sizing, alignment, and inspector positioning. */
  private void configureMenuOverlay() {
    menuOverlay.getStyleClass().add("chart-workspace-menu-overlay");
    menuOverlay.setMaxHeight(StackPane.USE_PREF_SIZE);
    menuOverlay.setPickOnBounds(false);
    StackPane.setAlignment(menuOverlay, Pos.TOP_CENTER);
    menuOverlay.boundsInParentProperty().addListener((ignored, previous, current) -> positionInspectorBelowMenu());
  }

  /**
   * Configures keyboard handling and composes persistent workspace layers.
   *
   * @param intervalSelectionDialog the validated interval-selection dialog
   * @param instrumentSearchDialog the validated instrument-search dialog
   */
  private void configureRoot(Dialog intervalSelectionDialog, Dialog instrumentSearchDialog) {
    root.getStyleClass().add("chart-workspace");
    root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
    root
      .getChildren()
      .setAll(chartLayer, menuOverlay, inspectorDrawer, intervalSelectionDialog, instrumentSearchDialog);
  }

  /** Connects observable workspace state to recursive view rebuilding and active styling. */
  private void connectModel() {
    model.rootProperty().addListener((ignored, previous, current) -> rebuildCharts());
    model.activeChartProperty().addListener((ignored, previous, current) -> displayActiveChart());
    model.multipleChartsProperty().addListener((ignored, previous, current) -> displayActiveChart());
  }

  /** Rebuilds the recursive chart layer while safely detaching reused chart nodes. */
  private void rebuildCharts() {
    List<Node> previous = List.copyOf(chartLayer.getChildren());
    chartLayer.getChildren().clear();
    previous.forEach(ChartWorkspaceViewBuilder::detach);
    chartContainers.clear();
    if (model.getRoot() != null) {
      chartLayer.getChildren().add(createItemView(model.getRoot()));
    }
    displayActiveChart();
  }

  /** Positions the inspector beneath the centered workspace menu. */
  private void positionInspectorBelowMenu() {
    double menuBottom = Math.max(0.0, menuOverlay.localToParent(menuOverlay.getLayoutBounds()).getMaxY());
    StackPane.setMargin(inspectorDrawer, new Insets(menuBottom, 0.0, 0.0, 0.0));
  }

  /**
   * Recursively creates the view corresponding to a workspace-tree item.
   *
   * @param item the workspace-tree item
   * @return the item view
   */
  private Node createItemView(ChartWorkspaceItem item) {
    if (item instanceof ChartWorkspaceLeaf leaf) {
      Chart chart = leaf.chart();
      ChartContainer container = new ChartContainer(chart.getView(), () -> chartActivatedHandler.accept(chart));
      chartContainers.put(chart, container);
      return new ChartSlot(container);
    }

    ChartWorkspaceSplit split = (ChartWorkspaceSplit) item;
    ResizableHandle handle = new ResizableHandle(true);
    ResizablePanelGroup group = new ResizablePanelGroup(
      split.orientation(),
      new ResizablePanel(createItemView(split.first())),
      handle,
      new ResizablePanel(createItemView(split.second()))
    );
    group.getStyleClass().add("chart-workspace-split");
    group.setMinSize(0.0, 0.0);
    group.setDividerPositions(split.dividerPosition());
    handle
      .positionProperty()
      .addListener((ignored, previous, current) -> split.setDividerPosition(current.doubleValue()));
    return group;
  }

  /** Updates chart-container active styling from the current workspace state. */
  private void displayActiveChart() {
    Chart activeChart = model.getActiveChart();
    boolean multipleCharts = model.hasMultipleCharts();
    chartContainers.forEach((chart, container) ->
      container.pseudoClassStateChanged(ACTIVE_PSEUDO_CLASS, multipleCharts && chart == activeChart)
    );
  }

  /**
   * Routes supported workspace shortcuts unless the active chart owns an open modal.
   *
   * @param event the key event to inspect and optionally consume
   */
  private void handleShortcut(KeyEvent event) {
    if (model.getActiveChart().modalOpenProperty().get()) {
      return;
    }
    ChartWorkspaceNavigationDirection navigationDirection = navigationDirection(event, macOs);
    if (navigationDirection != null) {
      navigationRequestedHandler.accept(navigationDirection);
      event.consume();
      return;
    }
    if (isCloseShortcut(event, macOs)) {
      closeRequestedHandler.run();
      event.consume();
      return;
    }
    ChartSplitDirection direction = splitDirection(event);
    if (direction != null) {
      splitRequestedHandler.accept(direction);
      event.consume();
    }
  }

  /**
   * Maps a macOS control-key event to a workspace navigation direction.
   *
   * @param event the key event to inspect
   * @param macOs true when macOS navigation shortcuts are enabled
   * @return the requested navigation direction, or null
   */
  static ChartWorkspaceNavigationDirection navigationDirection(KeyEvent event, boolean macOs) {
    Objects.requireNonNull(event, "event cannot be null");
    if (!macOs) {
      return null;
    }
    return NAVIGATION_SHORTCUTS.stream()
      .filter(shortcut -> shortcut.keyCombination().match(event))
      .map(NavigationShortcut::direction)
      .findFirst()
      .orElse(null);
  }

  /**
   * Reports whether an event requests closing the active chart on macOS.
   *
   * @param event the key event to inspect
   * @param macOs true when the macOS close shortcut is enabled
   * @return true when the event matches the close shortcut
   */
  static boolean isCloseShortcut(KeyEvent event, boolean macOs) {
    Objects.requireNonNull(event, "event cannot be null");
    return macOs && CLOSE_SHORTCUT.match(event);
  }

  /**
   * Maps a platform shortcut-key event to a chart split direction.
   *
   * @param event the key event to inspect
   * @return the requested split direction, or null
   */
  static ChartSplitDirection splitDirection(KeyEvent event) {
    Objects.requireNonNull(event, "event cannot be null");
    return SPLIT_SHORTCUTS.stream()
      .filter(shortcut -> shortcut.keyCombination().match(event))
      .map(SplitShortcut::direction)
      .findFirst()
      .orElse(null);
  }

  /**
   * Creates a platform shortcut-key combination.
   *
   * @param keyCode the shortcut's primary key
   * @return the platform shortcut combination
   */
  private static KeyCombination shortcut(KeyCode keyCode) {
    return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
  }

  /**
   * Creates a control-key combination.
   *
   * @param keyCode the shortcut's primary key
   * @return the control-key combination
   */
  private static KeyCombination control(KeyCode keyCode) {
    return new KeyCodeCombination(keyCode, KeyCombination.CONTROL_DOWN);
  }

  /**
   * Reports whether the current operating system is macOS.
   *
   * @return true on macOS
   */
  private static boolean isMacOs() {
    return System.getProperty("os.name", "").startsWith("Mac");
  }

  /**
   * Recursively detaches chart nodes before rebuilding their parent tree.
   *
   * @param node the node to detach
   */
  private static void detach(Node node) {
    if (node instanceof ChartSlot slot) {
      List<Node> children = List.copyOf(slot.getChildren());
      slot.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
      return;
    }
    if (node instanceof ChartContainer container) {
      container.getChildren().clear();
      return;
    }
    if (node instanceof ResizablePanelGroup group) {
      List<Node> children = List.copyOf(group.getChildren());
      group.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
      return;
    }
    if (node instanceof ResizablePanel panel) {
      List<Node> children = List.copyOf(panel.getChildren());
      panel.getChildren().clear();
      children.forEach(ChartWorkspaceViewBuilder::detach);
    }
  }

  /** Provides workspace spacing around one chart container. */
  private static final class ChartSlot extends StackPane {

    /**
     * Creates a slot containing a chart container.
     *
     * @param container the contained chart view
     */
    private ChartSlot(ChartContainer container) {
      getStyleClass().add("chart-workspace-chart-slot");
      setMinSize(0.0, 0.0);
      getChildren().add(container);
    }
  }

  /**
   * Associates a key combination with a chart split direction.
   *
   * @param keyCombination the matching key combination
   * @param direction the requested split direction
   */
  private record SplitShortcut(KeyCombination keyCombination, ChartSplitDirection direction) {}

  /**
   * Associates a key combination with a workspace navigation direction.
   *
   * @param keyCombination the matching key combination
   * @param direction the requested navigation direction
   */
  private record NavigationShortcut(KeyCombination keyCombination, ChartWorkspaceNavigationDirection direction) {}

  /** Displays and clips a chart while forwarding activation mouse presses. */
  private static final class ChartContainer extends StackPane {

    private final Rectangle clip = new Rectangle();

    /**
     * Creates a clipped, activatable chart container.
     *
     * @param chart the chart root
     * @param chartActivatedHandler the chart-activation callback
     */
    private ChartContainer(StackPane chart, Runnable chartActivatedHandler) {
      getStyleClass().add("chart-workspace-chart");
      setMinSize(0.0, 0.0);
      chart.setMinSize(0.0, 0.0);
      clip.widthProperty().bind(chart.widthProperty());
      clip.heightProperty().bind(chart.heightProperty());
      chart.setClip(clip);
      chart.backgroundProperty().addListener((ignored, previous, current) -> updateClipRadius(chart));
      getChildren().add(chart);
      updateClipRadius(chart);
      addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
        if (event.getButton() == MouseButton.PRIMARY) {
          chartActivatedHandler.run();
        }
      });
    }

    /**
     * Mirrors the chart background radius into its clipping rectangle.
     *
     * @param chart the clipped chart root
     */
    private void updateClipRadius(StackPane chart) {
      if (chart.getBackground() == null || chart.getBackground().getFills().isEmpty()) {
        return;
      }
      double radius = chart.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius();
      clip.setArcWidth(radius * 2.0);
      clip.setArcHeight(radius * 2.0);
    }
  }
}
