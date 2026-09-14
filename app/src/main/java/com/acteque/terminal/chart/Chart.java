package com.acteque.terminal.chart;

import com.acteque.terminal.chart.canvas.ChartCanvasController;
import com.acteque.terminal.chart.intervalselection.ChartIntervalSelectionController;
import com.acteque.terminal.chart.statusline.ChartStatusLineController;
import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.search.InstrumentSearchDialog;
import com.acteque.terminal.ui.core.dialog.Dialog;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.Canvas;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** Composes the price chart canvas with its controls and overlays. */
public final class Chart extends StackPane {

  private static final List<KeyCombination> INSTRUMENT_SEARCH_SHORTCUTS = List.of(
    shortcut(KeyCode.F),
    shortcut(KeyCode.SLASH),
    shortcut(KeyCode.P)
  );
  private static final KeyCombination INTERVAL_SELECTION_SHORTCUT = shortcut(KeyCode.I);

  private final BooleanProperty instrumentSearchOpen = new SimpleBooleanProperty(false);
  private final BooleanProperty intervalSelectionOpen = new SimpleBooleanProperty(false);
  private final InstrumentSearchDialog instrumentSearchDialog;
  private final ChartIntervalSelectionController intervalSelection;
  private final Dialog intervalSelectionDialog;
  private final ChartCanvasController canvasController;
  private final Canvas canvas;
  private final ChartStatusLineController statusLine;

  public Chart(
    List<PricePoint> pricePoints,
    String stockSymbol,
    ChartInterval interval,
    TiingoTickerCatalogApi tickerCatalog
  ) {
    Objects.requireNonNull(stockSymbol, "stockSymbol");
    Objects.requireNonNull(interval, "interval");
    Objects.requireNonNull(tickerCatalog, "tickerCatalog");
    getStyleClass().add("chart");

    instrumentSearchDialog = new InstrumentSearchDialog(stockSymbol, instrumentSearchOpen, tickerCatalog);
    instrumentSearchDialog.onRequestClose(() -> instrumentSearchOpen.set(false));

    intervalSelection = new ChartIntervalSelectionController(interval, intervalSelectionOpen);
    intervalSelection.onRequestClose(() -> intervalSelectionOpen.set(false));
    intervalSelectionDialog = intervalSelection.getView();

    BooleanBinding modalOpen = instrumentSearchOpen.or(intervalSelectionOpen);
    statusLine = new ChartStatusLineController(
      stockSymbol,
      interval,
      modalOpen,
      this::openInstrumentSearch,
      this::openIntervalSelection
    );
    intervalSelection.onIntervalSelected(statusLine::setInterval);

    ChartMenu menu = new ChartMenu();

    canvasController = new ChartCanvasController(pricePoints, interval, statusLine);
    canvas = canvasController.getView();

    VBox statusContent = new VBox(statusLine.getView());
    statusContent.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    statusContent.setPickOnBounds(false);
    statusContent.disableProperty().bind(modalOpen);
    StackPane statusOverlay = new StackPane(statusContent);
    statusOverlay.getStyleClass().add("chart-status-overlay");
    statusOverlay.setPickOnBounds(false);

    getChildren().setAll(canvas, menu, statusOverlay, instrumentSearchDialog, intervalSelectionDialog);
    addEventFilter(KeyEvent.KEY_PRESSED, this::handleShortcut);

    canvas.widthProperty().bind(widthProperty());
    canvas.heightProperty().bind(heightProperty());
  }

  public void setOnEarlierHistoryRequested(Runnable callback) {
    canvasController.setOnEarlierHistoryRequested(callback);
  }

  public void setOnInstrumentSelected(Consumer<String> callback) {
    instrumentSearchDialog.onInstrumentSelected(callback);
  }

  public void setOnIntervalSelected(Consumer<ChartInterval> callback) {
    Objects.requireNonNull(callback, "callback");
    intervalSelection.onIntervalSelected(interval -> {
      statusLine.setInterval(interval);
      callback.accept(interval);
    });
  }

  public void setInstrument(String symbol, String displayName, List<PricePoint> pricePoints) {
    Objects.requireNonNull(symbol, "symbol");
    statusLine.setInstrumentName(displayName);
    instrumentSearchDialog.setCurrentSymbol(symbol);
    canvasController.setInstrumentPricePoints(pricePoints);
  }

  public void setInstrumentLogo(InstrumentLogo logo, Image image) {
    statusLine.setInstrumentLogo(logo, image);
  }

  public void setPricePoints(List<PricePoint> pricePoints) {
    canvasController.setPricePoints(pricePoints);
  }

  public void drawChart() {
    canvasController.drawChart();
  }

  private void handleShortcut(KeyEvent event) {
    if (instrumentSearchOpen.get() || intervalSelectionOpen.get()) {
      return;
    }
    if (isInstrumentSearchShortcut(event)) {
      openInstrumentSearch();
      event.consume();
    } else if (isIntervalSelectionShortcut(event)) {
      openIntervalSelection();
      event.consume();
    }
  }

  private void openInstrumentSearch() {
    instrumentSearchOpen.set(true);
  }

  private void openIntervalSelection() {
    intervalSelectionOpen.set(true);
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

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    instrumentSearchDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
    intervalSelectionDialog.resizeRelocate(0.0, 0.0, getWidth(), getHeight());
  }
}
