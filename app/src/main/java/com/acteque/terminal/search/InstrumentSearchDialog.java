package com.acteque.terminal.search;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoSupportedTicker;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.KineticListView;
import com.acteque.terminal.ui.RefreshableView;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** A transient symbol picker. Selection is intentionally not applied to market data yet. */
public final class InstrumentSearchDialog extends StackPane implements RefreshableView {

  private static final double MAX_VIEWPORT_WIDTH_RATIO = 0.70;
  private static final double MAX_VIEWPORT_HEIGHT_RATIO = 0.70;
  private static final PseudoClass GLIDING_PSEUDO_CLASS = PseudoClass.getPseudoClass("gliding");
  private final TextField symbolField = new TextField();
  private final KineticListView<TiingoSupportedTicker> instruments = new KineticListView<>();
  private final ObservableList<TiingoSupportedTicker> catalogInstruments = FXCollections.observableArrayList();
  private final FilteredList<TiingoSupportedTicker> filteredInstruments = new FilteredList<>(catalogInstruments);
  private final TiingoTickerCatalogApi tickerCatalog;
  private Runnable overlayClickHandler = () -> {};
  private Runnable closeRequestHandler = () -> {};
  private boolean catalogLoadStarted;

  public InstrumentSearchDialog(
    String currentSymbol,
    ObservableBooleanValue open,
    TiingoTickerCatalogApi tickerCatalog
  ) {
    symbolField.setText(Objects.requireNonNull(currentSymbol, "currentSymbol"));
    Objects.requireNonNull(open, "open");
    this.tickerCatalog = Objects.requireNonNull(tickerCatalog, "tickerCatalog");

    getStyleClass().add("instrument-search-dialog");
    symbolField.getStyleClass().add("instrument-search-field");
    configureInstrumentList();

    visibleProperty().bind(open);
    managedProperty().bind(open);
    open.addListener((ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        symbolField.selectAll();
        symbolField.requestFocus();
        loadCatalog();
      }
    });
    addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        closeRequestHandler.run();
        event.consume();
      }
    });

    refreshView();
    ChartReloadHooks.register(this);
  }

  @Override
  public void refreshView() {
    setPickOnBounds(true);
    setOnMouseClicked(event -> overlayClickHandler.run());

    Label title = new Label("Instrument search");
    title.getStyleClass().add("instrument-search-title");

    symbolField.setPromptText("Symbol");
    symbolField.setAccessibleText("Stock symbol");

    getChildren().clear();
    VBox card = new VBox(title, symbolField, instruments);
    card.getStyleClass().add("instrument-search-card");
    card.maxWidthProperty().bind(widthProperty().multiply(MAX_VIEWPORT_WIDTH_RATIO));
    card.maxHeightProperty().bind(heightProperty().multiply(MAX_VIEWPORT_HEIGHT_RATIO));
    card.setOnMouseClicked(event -> event.consume());
    VBox.setVgrow(instruments, Priority.ALWAYS);

    getChildren().setAll(card);
  }

  private void configureInstrumentList() {
    instruments.getStyleClass().add("instrument-list");
    instruments.setItems(filteredInstruments);
    instruments.setCellFactory(ignored -> new InstrumentCell());
    instruments
      .glidingProperty()
      .addListener((ignored, wasGliding, isGliding) ->
        instruments.pseudoClassStateChanged(GLIDING_PSEUDO_CLASS, isGliding)
      );
    instruments.setPlaceholder(new Label("Loading instruments…"));

    symbolField.textProperty().addListener((ignored, oldValue, newValue) -> filterInstruments(newValue));
  }

  private void loadCatalog() {
    if (catalogLoadStarted) {
      return;
    }
    catalogLoadStarted = true;

    CompletableFuture.supplyAsync(tickerCatalog::getSupportedTickers).whenComplete((tickers, failure) ->
      Platform.runLater(() -> {
        if (failure != null) {
          instruments.setPlaceholder(new Label("Unable to load instruments"));
          return;
        }
        setInstruments(tickers);
      })
    );
  }

  private void setInstruments(List<TiingoSupportedTicker> tickers) {
    catalogInstruments.setAll(tickers);
    instruments.setPlaceholder(new Label("No matching instruments"));
    filterInstruments(symbolField.getText());
  }

  private void filterInstruments(String query) {
    String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    filteredInstruments.setPredicate(
      ticker ->
        normalizedQuery.isEmpty() ||
        ticker.ticker().toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
        ticker.exchange().toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
  }

  private static final class InstrumentCell extends ListCell<TiingoSupportedTicker> {

    private final Label ticker = new Label();
    private final Label description = new Label();
    private final Label exchange = new Label();
    private final HBox row = new HBox(ticker, description, exchange);

    private InstrumentCell() {
      getStyleClass().add("instrument-cell");
      ticker.getStyleClass().add("instrument-ticker");
      description.getStyleClass().add("instrument-description");
      exchange.getStyleClass().add("instrument-exchange");
      row.getStyleClass().add("instrument-row");
      description.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(description, Priority.ALWAYS);
    }

    @Override
    protected void updateItem(TiingoSupportedTicker instrument, boolean empty) {
      super.updateItem(instrument, empty);
      setText(null);
      if (empty || instrument == null) {
        setGraphic(null);
        return;
      }

      ticker.setText(instrument.ticker());
      description.setText("");
      exchange.setText(instrument.exchange());
      setGraphic(row);
    }
  }

  public void onOverlayClick(Runnable callback) {
    overlayClickHandler = Objects.requireNonNull(callback, "callback");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback");
  }
}
