package com.acteque.terminal.chart;

import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.drawer.Drawer;
import java.util.List;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart. */
final class ChartViewBuilder implements Builder<StackPane>, ReloadTarget {

  private static final List<KeyCombination> INSTRUMENT_SEARCH_SHORTCUTS = List.of(
    shortcut(KeyCode.F),
    shortcut(KeyCode.SLASH),
    shortcut(KeyCode.P)
  );
  private static final KeyCombination INTERVAL_SELECTION_SHORTCUT = shortcut(KeyCode.I);

  private final ChartModel model;
  private final Canvas canvas;
  private final Region menu;
  private final Drawer inspectorDrawer;
  private final Region statusLine;
  private final Dialog instrumentSearchDialog;
  private final Dialog intervalSelectionDialog;
  private final Runnable instrumentSearchRequestedHandler;
  private final Runnable intervalSelectionRequestedHandler;
  private final ChartPane root;

  ChartViewBuilder(
    ChartModel model,
    Canvas canvas,
    Region menu,
    Drawer inspectorDrawer,
    Region statusLine,
    Dialog instrumentSearchDialog,
    Dialog intervalSelectionDialog,
    Runnable instrumentSearchRequestedHandler,
    Runnable intervalSelectionRequestedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.canvas = Objects.requireNonNull(canvas, "canvas cannot be null");
    this.menu = Objects.requireNonNull(menu, "menu cannot be null");
    this.inspectorDrawer = Objects.requireNonNull(inspectorDrawer, "inspectorDrawer cannot be null");
    this.statusLine = Objects.requireNonNull(statusLine, "statusLine cannot be null");
    this.instrumentSearchDialog = Objects.requireNonNull(
      instrumentSearchDialog,
      "instrumentSearchDialog cannot be null"
    );
    this.intervalSelectionDialog = Objects.requireNonNull(
      intervalSelectionDialog,
      "intervalSelectionDialog cannot be null"
    );
    this.instrumentSearchRequestedHandler = Objects.requireNonNull(
      instrumentSearchRequestedHandler,
      "instrumentSearchRequestedHandler cannot be null"
    );
    this.intervalSelectionRequestedHandler = Objects.requireNonNull(
      intervalSelectionRequestedHandler,
      "intervalSelectionRequestedHandler cannot be null"
    );
    root = new ChartPane(instrumentSearchDialog, intervalSelectionDialog);

    root.getStyleClass().add("chart");
    root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
    canvas.widthProperty().bind(root.widthProperty());
    canvas.heightProperty().bind(root.heightProperty());
    menu.boundsInParentProperty().addListener((ignored, previous, current) -> positionSettingsDrawerBelowMenu());

    refreshView();
    positionSettingsDrawerBelowMenu();
    ReloadHooks.register(this);
  }

  @Override
  public StackPane build() {
    return root;
  }

  @Override
  public void refreshView() {
    VBox statusContent = new VBox(statusLine);
    statusContent.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    statusContent.setPickOnBounds(false);
    statusContent.disableProperty().bind(model.modalOpenProperty());

    StackPane statusOverlay = new StackPane(statusContent);
    statusOverlay.getStyleClass().add("chart-status-overlay");
    statusOverlay.setPickOnBounds(false);

    root
      .getChildren()
      .setAll(canvas, menu, statusOverlay, inspectorDrawer, instrumentSearchDialog, intervalSelectionDialog);
  }

  private void handleShortcut(KeyEvent event) {
    if (model.isModalOpen()) {
      return;
    }
    if (isInstrumentSearchShortcut(event)) {
      instrumentSearchRequestedHandler.run();
      event.consume();
    } else if (isIntervalSelectionShortcut(event)) {
      intervalSelectionRequestedHandler.run();
      event.consume();
    }
  }

  private void positionSettingsDrawerBelowMenu() {
    double menuBottom = Math.max(0.0, menu.getBoundsInParent().getMaxY());
    StackPane.setMargin(inspectorDrawer, new Insets(menuBottom, 0.0, 0.0, 0.0));
  }

  static boolean isInstrumentSearchShortcut(KeyEvent event) {
    return INSTRUMENT_SEARCH_SHORTCUTS.stream().anyMatch(shortcut -> shortcut.match(event));
  }

  static boolean isIntervalSelectionShortcut(KeyEvent event) {
    return INTERVAL_SELECTION_SHORTCUT.match(event);
  }

  private static KeyCombination shortcut(KeyCode keyCode) {
    return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
  }

  private static final class ChartPane extends StackPane {

    private final Dialog instrumentSearchDialog;
    private final Dialog intervalSelectionDialog;

    private ChartPane(Dialog instrumentSearchDialog, Dialog intervalSelectionDialog) {
      this.instrumentSearchDialog = instrumentSearchDialog;
      this.intervalSelectionDialog = intervalSelectionDialog;
    }

    @Override
    protected void layoutChildren() {
      super.layoutChildren();
      instrumentSearchDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
      intervalSelectionDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
    }
  }
}
