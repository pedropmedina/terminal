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
import com.acteque.terminal.ui.UiStyles;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.BorderWidths;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** A transient symbol picker. Selection is intentionally not applied to market data yet. */
public final class InstrumentSearchDialog extends StackPane implements RefreshableView {

  private static final double MODAL_WIDTH = 640.0;
  private static final double MAX_VIEWPORT_WIDTH_RATIO = 0.70;
  private static final double MAX_VIEWPORT_HEIGHT_RATIO = 0.70;
  private static final Color ROW_BORDER_COLOR = Color.rgb(220, 223, 229);
  private static final Background OVERLAY_BACKGROUND = new Background(
    new BackgroundFill(Color.rgb(45, 48, 55, 0.24), CornerRadii.EMPTY, Insets.EMPTY)
  );

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
    setAlignment(Pos.CENTER);
    setBackground(OVERLAY_BACKGROUND);
    setOnMouseClicked(event -> overlayClickHandler.run());

    Label title = new Label("Instrument search");
    title.setFont(Font.font("System", FontWeight.SEMI_BOLD, 18.0));

    symbolField.setPromptText("Symbol");
    symbolField.setAccessibleText("Stock symbol");
    symbolField.setBackground(UiStyles.SURFACE_BACKGROUND);
    symbolField.setBorder(
      new Border(
        new BorderStroke(ROW_BORDER_COLOR, BorderStrokeStyle.SOLID, new CornerRadii(8.0), BorderWidths.DEFAULT)
      )
    );

    getChildren().clear();
    VBox card = new VBox(16.0, title, symbolField, instruments);
    card.setPrefWidth(MODAL_WIDTH);
    card.maxWidthProperty().bind(widthProperty().multiply(MAX_VIEWPORT_WIDTH_RATIO));
    card.maxHeightProperty().bind(heightProperty().multiply(MAX_VIEWPORT_HEIGHT_RATIO));
    card.setPadding(new Insets(24.0));
    card.setBackground(UiStyles.SURFACE_BACKGROUND);
    card.setBorder(Border.EMPTY);
    card.setOnMouseClicked(event -> event.consume());
    VBox.setVgrow(instruments, Priority.ALWAYS);
    StackPane.setAlignment(card, Pos.CENTER);

    getChildren().setAll(card);
  }

  private void configureInstrumentList() {
    instruments.setItems(filteredInstruments);
    instruments.setCellFactory(ignored -> new InstrumentCell(instruments.glidingProperty()));
    instruments.setBackground(Background.EMPTY);
    instruments.setBorder(Border.EMPTY);
    instruments.setPrefHeight(360.0);
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

    private static final double TICKER_WIDTH = 120.0;
    private static final double EXCHANGE_WIDTH = 100.0;
    private static final Border ROW_DIVIDER = new Border(
      new BorderStroke(
        ROW_BORDER_COLOR,
        BorderStrokeStyle.SOLID,
        CornerRadii.EMPTY,
        new BorderWidths(0.0, 0.0, 1.0, 0.0)
      )
    );
    private static final Background HOVER_BACKGROUND = new Background(
      new BackgroundFill(Color.rgb(229, 231, 235), CornerRadii.EMPTY, Insets.EMPTY)
    );

    private final Label ticker = new Label();
    private final Label description = new Label();
    private final Label exchange = new Label();
    private final HBox row = new HBox(16.0, ticker, description, exchange);
    private final ObservableBooleanValue gliding;

    private InstrumentCell(ObservableBooleanValue gliding) {
      this.gliding = Objects.requireNonNull(gliding, "gliding");
      setBackground(Background.EMPTY);
      hoverProperty().addListener((ignored, wasHovered, isHovered) -> refreshBackground());
      selectedProperty().addListener((ignored, wasSelected, isSelected) -> refreshBackground());
      gliding.addListener((ignored, wasGliding, isGliding) -> refreshBackground());
      ticker.setMinWidth(TICKER_WIDTH);
      ticker.setFont(Font.font("System", FontWeight.SEMI_BOLD, 13.0));
      ticker.setTextFill(Color.BLACK);
      description.setMaxWidth(Double.MAX_VALUE);
      description.setTextFill(Color.BLACK);
      exchange.setMinWidth(EXCHANGE_WIDTH);
      exchange.setAlignment(Pos.CENTER_RIGHT);
      exchange.setTextFill(Color.BLACK);
      HBox.setHgrow(description, Priority.ALWAYS);
      row.setAlignment(Pos.CENTER_LEFT);
      row.setPadding(new Insets(8.0, 4.0, 8.0, 4.0));
    }

    @Override
    protected void updateItem(TiingoSupportedTicker instrument, boolean empty) {
      super.updateItem(instrument, empty);
      setText(null);
      if (empty || instrument == null) {
        refreshBackground();
        setBorder(Border.EMPTY);
        setGraphic(null);
        return;
      }

      refreshBackground();
      setBorder(ROW_DIVIDER);
      ticker.setText(instrument.ticker());
      description.setText("");
      exchange.setText(instrument.exchange());
      setGraphic(row);
    }

    private void refreshBackground() {
      setBackground(!gliding.get() && !isEmpty() && isHover() ? HOVER_BACKGROUND : Background.EMPTY);
    }
  }

  public void onOverlayClick(Runnable callback) {
    overlayClickHandler = Objects.requireNonNull(callback, "callback");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback");
  }
}
