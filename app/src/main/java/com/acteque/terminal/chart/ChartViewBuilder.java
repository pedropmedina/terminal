package com.acteque.terminal.chart;

import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.List;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.canvas.Canvas;
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
  private final Region identifier;
  private final Runnable instrumentSearchRequestedHandler;
  private final Runnable intervalSelectionRequestedHandler;
  private final StackPane root = new StackPane();

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

    identifier = new Region();
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

    root.getStyleClass().add("chart");
    root.addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);
    canvas.widthProperty().bind(root.widthProperty());
    canvas.heightProperty().bind(root.heightProperty());

    refreshView();
    ReloadHooks.register(this);
  }

  @Override
  public StackPane build() {
    return root;
  }

  @Override
  public void refreshView() {
    HBox statusContent = new HBox(identifier, statusLine);
    statusContent.getStyleClass().add("chart-status-content");
    statusContent.setMaxSize(Region.USE_PREF_SIZE, Region.USE_PREF_SIZE);
    statusContent.setPickOnBounds(false);
    statusContent.disableProperty().bind(model.modalOpenProperty());

    StackPane statusOverlay = new StackPane(statusContent);
    statusOverlay.getStyleClass().add("chart-status-overlay");
    statusOverlay.setPickOnBounds(false);

    root.getChildren().setAll(canvas, statusOverlay);
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

  static boolean isInstrumentSearchShortcut(KeyEvent event) {
    return INSTRUMENT_SEARCH_SHORTCUTS.stream().anyMatch(shortcut -> shortcut.match(event));
  }

  static boolean isIntervalSelectionShortcut(KeyEvent event) {
    return INTERVAL_SELECTION_SHORTCUT.match(event);
  }

  private static KeyCombination shortcut(KeyCode keyCode) {
    return new KeyCodeCombination(keyCode, KeyCombination.SHORTCUT_DOWN);
  }
}
