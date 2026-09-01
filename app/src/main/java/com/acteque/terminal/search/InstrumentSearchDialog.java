package com.acteque.terminal.search;

import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoSupportedTicker;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.KineticListView;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.dialog.DialogContent;
import com.acteque.terminal.ui.core.dialog.DialogTitle;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

/** A transient symbol picker. */
public final class InstrumentSearchDialog extends Dialog implements RefreshableView {

  private static final double MAX_VIEWPORT_WIDTH_RATIO = 0.70;
  private static final double MAX_VIEWPORT_HEIGHT_RATIO = 0.70;
  private static final PseudoClass GLIDING_PSEUDO_CLASS = PseudoClass.getPseudoClass("gliding");
  private final Input symbolField = new Input();
  private final KineticListView<TiingoSupportedTicker> instruments = new KineticListView<>();
  private final ObservableList<TiingoSupportedTicker> catalogInstruments = FXCollections.observableArrayList();
  private final FilteredList<TiingoSupportedTicker> filteredInstruments = new FilteredList<>(catalogInstruments);
  private final TiingoTickerCatalogApi tickerCatalog;
  private Runnable closeRequestHandler = () -> {};
  private Consumer<String> instrumentSelectedHandler = ignored -> {};
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
    configureInstrumentList();

    open.addListener((ignored, wasOpen, isOpen) -> {
      setOpen(isOpen);
      if (isOpen) {
        symbolField.selectAll();
        symbolField.requestFocus();
        loadCatalog();
      }
    });
    openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && open.get()) {
        closeRequestHandler.run();
      }
    });
    setOpen(open.get());

    refreshView();
    ChartReloadHooks.register(this);
  }

  @Override
  public void refreshView() {
    DialogTitle title = new DialogTitle("Instrument search");
    title.getStyleClass().add("instrument-search-title");

    symbolField.setPromptText("Symbol");
    symbolField.setAccessibleText("Stock symbol");

    DialogContent card = new DialogContent(title, symbolField, instruments);
    card.getStyleClass().add("instrument-search-card");
    card.maxWidthProperty().bind(widthProperty().multiply(MAX_VIEWPORT_WIDTH_RATIO));
    card.maxHeightProperty().bind(heightProperty().multiply(MAX_VIEWPORT_HEIGHT_RATIO));
    DialogContent.setVgrow(instruments, Priority.ALWAYS);

    setContent(card);
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

  public void setCurrentSymbol(String symbol) {
    symbolField.setText(Objects.requireNonNull(symbol, "symbol"));
  }

  public void onInstrumentSelected(Consumer<String> callback) {
    instrumentSelectedHandler = Objects.requireNonNull(callback, "callback");
  }

  private final class InstrumentCell extends ListCell<TiingoSupportedTicker> {

    private final Label ticker = new Label();
    private final Label description = new Label();
    private final Label exchange = new Label();
    private final HBox row = new HBox(ticker, description, exchange);

    private InstrumentCell() {
      getStyleClass().add("instrument-cell");
      row.getStyleClass().add("instrument-row");
      ticker.getStyleClass().add("instrument-ticker");
      exchange.getStyleClass().add("instrument-exchange");
      description.getStyleClass().add("instrument-description");

      description.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(description, Priority.ALWAYS);

      setOnMouseClicked(event -> {
        if (event.getButton() == MouseButton.PRIMARY && !isEmpty() && getItem() != null && !instruments.isGliding()) {
          String selectedSymbol = getItem().ticker();
          close();
          instrumentSelectedHandler.accept(selectedSymbol);
          event.consume();
        }
      });
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

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback");
  }
}
