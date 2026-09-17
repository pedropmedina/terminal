package com.acteque.terminal.search;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.KineticListView;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.dialog.DialogContent;
import com.acteque.terminal.ui.core.dialog.DialogTitle;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.css.PseudoClass;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for instrument search. */
final class InstrumentSearchViewBuilder implements Builder<Dialog>, RefreshableView {

  private static final double MAX_VIEWPORT_WIDTH_RATIO = 0.70;
  private static final double MAX_VIEWPORT_HEIGHT_RATIO = 0.70;
  private static final PseudoClass GLIDING_PSEUDO_CLASS = PseudoClass.getPseudoClass("gliding");

  private final InstrumentSearchModel model;
  private final Consumer<Instrument> instrumentSelectedHandler;
  private final Dialog root = new Dialog();
  private final Input symbolField = new Input();
  private final KineticListView<Instrument> instruments = new KineticListView<>();

  InstrumentSearchViewBuilder(
    InstrumentSearchModel model,
    ObservableBooleanValue open,
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
    Objects.requireNonNull(open, "open cannot be null");
    Objects.requireNonNull(queryChangedHandler, "queryChangedHandler cannot be null");
    Objects.requireNonNull(catalogRequestedHandler, "catalogRequestedHandler cannot be null");
    Objects.requireNonNull(closeRequestHandler, "closeRequestHandler cannot be null");

    root.getStyleClass().add("instrument-search-dialog");
    instruments.getStyleClass().add("instrument-list");
    instruments.setItems(model.matchingInstrumentsProperty());
    instruments.setCellFactory(ignored -> new InstrumentCell());
    instruments
      .glidingProperty()
      .addListener((ignored, wasGliding, isGliding) ->
        instruments.pseudoClassStateChanged(GLIDING_PSEUDO_CLASS, isGliding)
      );

    symbolField.setText(model.getCurrentSymbol());
    symbolField.textProperty().addListener((ignored, oldValue, newValue) -> queryChangedHandler.accept(newValue));
    model.currentSymbolProperty().addListener((ignored, oldValue, newValue) -> {
      if (!Objects.equals(symbolField.getText(), newValue)) {
        symbolField.setText(newValue);
      }
    });
    model.loadStateProperty().addListener(ignored -> updatePlaceholder());

    open.addListener((ignored, wasOpen, isOpen) -> {
      root.setOpen(isOpen);
      if (isOpen) {
        showSearch(catalogRequestedHandler);
      }
    });
    root.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && open.get()) {
        closeRequestHandler.run();
      }
    });
    root.setOpen(open.get());
    if (open.get()) {
      showSearch(catalogRequestedHandler);
    }

    refreshView();
    updatePlaceholder();
    ChartReloadHooks.register(this);
  }

  @Override
  public Dialog build() {
    return root;
  }

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

  void close() {
    root.close();
  }

  private void showSearch(Runnable catalogRequestedHandler) {
    symbolField.selectAll();
    Platform.runLater(symbolField::requestFocus);
    catalogRequestedHandler.run();
  }

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

  private final class InstrumentCell extends ListCell<Instrument> {

    private final Label symbol = new Label();
    private final Label description = new Label();
    private final Label exchange = new Label();
    private final HBox row = new HBox(symbol, description, exchange);

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
