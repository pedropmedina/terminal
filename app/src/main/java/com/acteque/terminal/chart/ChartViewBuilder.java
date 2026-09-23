package com.acteque.terminal.chart;

import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.List;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart. */
final class ChartViewBuilder implements Builder<StackPane>, ReloadTarget {

  private static final CornerRadii IDENTIFIER_RADII = new CornerRadii(3.0);
  private static final List<KeyCombination> INSTRUMENT_SEARCH_SHORTCUTS = List.of(
    shortcut(KeyCode.F),
    shortcut(KeyCode.SLASH),
    shortcut(KeyCode.P)
  );
  private static final KeyCombination INTERVAL_SELECTION_SHORTCUT = shortcut(KeyCode.I);

  private final ChartModel model;
  private final Canvas canvas;
  private final Region statusLine;
  private final Region identifier = new Region();
  private final Runnable instrumentSearchRequestedHandler;
  private final Runnable intervalSelectionRequestedHandler;
  private final StackPane root = new StackPane();

  /**
   * Creates and connects the chart's JavaFX composition.
   *
   * @param model the observable chart state
   * @param canvas the rendered chart canvas
   * @param statusLine the passive OHLCV status line
   * @param instrumentSearchRequestedHandler the instrument-search intent callback
   * @param intervalSelectionRequestedHandler the interval-selection intent callback
   */
  ChartViewBuilder(
    ChartModel model,
    Canvas canvas,
    Region statusLine,
    Runnable instrumentSearchRequestedHandler,
    Runnable intervalSelectionRequestedHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.canvas = Objects.requireNonNull(canvas, "canvas cannot be null");
    this.statusLine = Objects.requireNonNull(statusLine, "statusLine cannot be null");
    this.instrumentSearchRequestedHandler = Objects.requireNonNull(
      instrumentSearchRequestedHandler,
      "instrumentSearchRequestedHandler cannot be null"
    );
    this.intervalSelectionRequestedHandler = Objects.requireNonNull(
      intervalSelectionRequestedHandler,
      "intervalSelectionRequestedHandler cannot be null"
    );

    configureIdentifier();
    configureRoot();

    refreshView();
    ReloadHooks.register(this);
  }

  /** Configures model bindings for the workspace identifier. */
  private void configureIdentifier() {
    identifier.getStyleClass().add("chart-identifier");
    identifier
      .backgroundProperty()
      .bind(
        Bindings.createObjectBinding(
          () ->
            new Background(new BackgroundFill(model.identifierColorProperty().get(), IDENTIFIER_RADII, Insets.EMPTY)),
          model.identifierColorProperty()
        )
      );
    identifier.visibleProperty().bind(model.identifierVisibleProperty());
    identifier.managedProperty().bind(model.identifierVisibleProperty());
    identifier.setMouseTransparent(true);
  }

  /** Connects root sizing and keyboard-intent forwarding. */
  private void configureRoot() {
    root.getStyleClass().add("chart");
    root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
    canvas.widthProperty().bind(root.widthProperty());
    canvas.heightProperty().bind(root.heightProperty());
  }

  /** @return the assembled chart root */
  @Override
  public StackPane build() {
    return root;
  }

  /** Rebuilds reloadable child composition while retaining model bindings. */
  @Override
  public void refreshView() {
    statusLine.setMaxHeight(Region.USE_PREF_SIZE);
    root.getChildren().setAll(canvas, createStatusOverlay(), createLoadError());
  }

  /** @return the bottom-aligned overlay containing identifier and status line */
  private StackPane createStatusOverlay() {
    HBox statusContent = new HBox(identifier, statusLine);
    statusContent.getStyleClass().add("chart-status-content");
    statusContent.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    statusContent.setPickOnBounds(false);

    StackPane statusOverlay = new StackPane(statusContent);
    statusOverlay.getStyleClass().add("chart-status-overlay");
    statusOverlay.setPickOnBounds(false);
    return statusOverlay;
  }

  /** @return the top-aligned label bound to asynchronous load failures */
  private Label createLoadError() {
    Label loadError = new Label();
    loadError.getStyleClass().add("chart-load-error");
    loadError.textProperty().bind(model.loadErrorProperty());
    loadError.visibleProperty().bind(model.loadErrorProperty().isNotNull());
    loadError.managedProperty().bind(loadError.visibleProperty());
    loadError.setMouseTransparent(true);
    StackPane.setAlignment(loadError, Pos.TOP_CENTER);
    return loadError;
  }

  /**
   * Forwards a supported keyboard shortcut when no chart modal is already open.
   *
   * @param event the key event to inspect and consume
   */
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

  /**
   * @param event the key event to test
   * @return true when the event requests instrument search
   */
  static boolean isInstrumentSearchShortcut(KeyEvent event) {
    return INSTRUMENT_SEARCH_SHORTCUTS.stream().anyMatch(shortcut -> shortcut.match(event));
  }

  /**
   * @param event the key event to test
   * @return true when the event requests interval selection
   */
  static boolean isIntervalSelectionShortcut(KeyEvent event) {
    return INTERVAL_SELECTION_SHORTCUT.match(event);
  }

  /**
   * @param keyCode the shortcut's primary key
   * @return a platform shortcut-key combination
   */
  private static KeyCombination shortcut(KeyCode keyCode) {
    return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
  }
}
