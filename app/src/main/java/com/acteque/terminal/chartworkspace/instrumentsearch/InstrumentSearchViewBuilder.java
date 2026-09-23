package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Input;
import com.acteque.terminal.ui.ListView;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.dialog.DialogContent;
import com.acteque.terminal.ui.dialog.DialogTitle;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for instrument search. */
final class InstrumentSearchViewBuilder implements Builder<Dialog>, ReloadTarget {

  private static final double MAX_VIEWPORT_WIDTH_RATIO = 0.70;
  private static final double MAX_VIEWPORT_HEIGHT_RATIO = 0.70;
  private static final PseudoClass GLIDING_PSEUDO_CLASS = PseudoClass.getPseudoClass("gliding");

  private final InstrumentSearchModel model;
  private final Consumer<Instrument> instrumentSelectedHandler;
  private final Dialog root = new Dialog();
  private final Input symbolField = new Input();
  private final ListView<Instrument> instruments = new ListView<>();

  /**
   * Creates and connects the instrument-search JavaFX composition.
   *
   * @param model the observable instrument-search state
   * @param queryChangedHandler the search-query callback
   * @param catalogRequestedHandler the catalog-load callback
   * @param instrumentSelectedHandler the instrument-selection callback
   * @param closeRequestHandler the dialog-close callback
   */
  InstrumentSearchViewBuilder(
    InstrumentSearchModel model,
    Consumer<String> queryChangedHandler,
    Runnable catalogRequestedHandler,
    Consumer<Instrument> instrumentSelectedHandler,
    Runnable closeRequestHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.instrumentSelectedHandler = Objects.requireNonNull(
      instrumentSelectedHandler,
      "instrumentSelectedHandler cannot be null"
    );
    Consumer<String> validatedQueryChangedHandler = Objects.requireNonNull(
      queryChangedHandler,
      "queryChangedHandler cannot be null"
    );
    Runnable validatedCatalogRequestedHandler = Objects.requireNonNull(
      catalogRequestedHandler,
      "catalogRequestedHandler cannot be null"
    );
    Runnable validatedCloseRequestHandler = Objects.requireNonNull(
      closeRequestHandler,
      "closeRequestHandler cannot be null"
    );

    configureDialog();
    configureSymbolField();
    configureInstrumentList();
    connectComponents(validatedQueryChangedHandler, validatedCatalogRequestedHandler, validatedCloseRequestHandler);

    refreshView();
    displayOpenState(model.isOpen(), validatedCatalogRequestedHandler);
    updatePlaceholder();
    ReloadHooks.register(this);
  }

  /**
   * Returns the assembled instrument-search dialog.
   *
   * @return the instrument-search dialog
   */
  @Override
  public Dialog build() {
    return root;
  }

  /** Rebuilds reloadable dialog content while retaining model listeners and controls. */
  @Override
  public void refreshView() {
    DialogTitle title = new DialogTitle("Instrument search");
    title.getStyleClass().add("instrument-search-title");

    symbolField.setPromptText("Symbol");
    symbolField.setAccessibleText("Stock symbol");

    DialogContent card = new DialogContent(title, symbolField, instruments);
    card.getStyleClass().add("instrument-search-card");
    card.maxWidthProperty().bind(root.widthProperty().multiply(MAX_VIEWPORT_WIDTH_RATIO));
    card.maxHeightProperty().bind(root.heightProperty().multiply(MAX_VIEWPORT_HEIGHT_RATIO));
    DialogContent.setVgrow(instruments, Priority.ALWAYS);
    root.setContent(card);
  }

  /** Configures the dialog's stable structural styling. */
  private void configureDialog() {
    root.getStyleClass().add("instrument-search-dialog");
  }

  /** Initializes the search field from the current model state. */
  private void configureSymbolField() {
    symbolField.setText(model.getCurrentSymbol());
  }

  /** Configures the searchable instrument list and its gliding presentation state. */
  private void configureInstrumentList() {
    instruments.getStyleClass().add("instrument-list");
    instruments.setItems(model.matchingInstrumentsProperty());
    instruments.setCellFactory(ignored -> new InstrumentCell());
    instruments
      .glidingProperty()
      .addListener((ignored, wasGliding, isGliding) ->
        instruments.pseudoClassStateChanged(GLIDING_PSEUDO_CLASS, isGliding)
      );
  }

  /**
   * Connects model observations and forwards control intents to the interactor.
   *
   * @param queryChangedHandler the validated search-query callback
   * @param catalogRequestedHandler the validated catalog-load callback
   * @param closeRequestHandler the validated dialog-close callback
   */
  private void connectComponents(
    Consumer<String> queryChangedHandler,
    Runnable catalogRequestedHandler,
    Runnable closeRequestHandler
  ) {
    symbolField.textProperty().addListener((ignored, oldValue, newValue) -> queryChangedHandler.accept(newValue));
    model.currentSymbolProperty().addListener((ignored, oldValue, newValue) -> {
      if (!Objects.equals(symbolField.getText(), newValue)) {
        symbolField.setText(newValue);
      }
    });
    model.loadStateProperty().addListener(ignored -> updatePlaceholder());

    model.openProperty().addListener((ignored, wasOpen, isOpen) -> displayOpenState(isOpen, catalogRequestedHandler));
    root.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && model.isOpen()) {
        closeRequestHandler.run();
      }
    });
  }

  /**
   * Mirrors model visibility into the dialog and prepares the search field when opened.
   *
   * @param open true to open the dialog
   * @param catalogRequestedHandler the catalog-load callback
   */
  private void displayOpenState(boolean open, Runnable catalogRequestedHandler) {
    root.setOpen(open);
    if (open) {
      symbolField.selectAll();
      Platform.runLater(symbolField::requestFocus);
      catalogRequestedHandler.run();
    }
  }

  /** Displays placeholder text appropriate to the current catalog loading state. */
  private void updatePlaceholder() {
    instruments.setPlaceholder(
      new Label(
        switch (model.getLoadState()) {
          case NOT_LOADED, LOADING -> "Loading instruments…";
          case LOADED -> "No matching instruments";
          case FAILED -> "Unable to load instruments";
        }
      )
    );
  }

  /** Renders an instrument result and forwards valid primary-button selections. */
  private final class InstrumentCell extends ListCell<Instrument> {

    private final Label symbol = new Label();
    private final Label description = new Label();
    private final Label exchange = new Label();
    private final HBox row = new HBox(symbol, description, exchange);

    /** Creates and styles a reusable instrument-result cell. */
    private InstrumentCell() {
      getStyleClass().add("instrument-cell");
      row.getStyleClass().add("instrument-row");
      symbol.getStyleClass().add("instrument-ticker");
      exchange.getStyleClass().add("instrument-exchange");
      description.getStyleClass().add("instrument-description");
      description.setMaxWidth(Double.MAX_VALUE);
      HBox.setHgrow(description, Priority.ALWAYS);

      setOnMouseClicked(event -> {
        if (event.getButton() == MouseButton.PRIMARY && !isEmpty() && getItem() != null && !instruments.isGliding()) {
          instrumentSelectedHandler.accept(getItem());
          event.consume();
        }
      });
    }

    /**
     * Updates the cell's labels and graphic for its current instrument.
     *
     * @param instrument the instrument to display
     * @param empty true when the cell has no item
     */
    @Override
    protected void updateItem(Instrument instrument, boolean empty) {
      super.updateItem(instrument, empty);
      setText(null);
      if (empty || instrument == null) {
        setGraphic(null);
        return;
      }

      symbol.setText(instrument.symbol());
      description.setText(instrument.name().orElse(""));
      exchange.setText(instrument.exchange().orElse(""));
      setGraphic(row);
    }
  }
}
