package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/** Applies instrument-search state transitions without depending on its layout. */
final class InstrumentSearchInteractor {

  private final InstrumentSearchModel model;
  private final InstrumentCatalog catalog;
  private final Executor backgroundExecutor;
  private final Executor uiExecutor;
  private Consumer<String> instrumentSelectedHandler = ignored -> {};
  private Runnable closeRequestHandler = () -> {};

  /**
   * Creates an interactor backed by the supplied state and catalog service.
   *
   * @param model the observable instrument-search state
   * @param catalog the source of searchable instruments
   * @param backgroundExecutor the executor used to load the catalog
   * @param uiExecutor the executor used to publish catalog results to JavaFX state
   */
  InstrumentSearchInteractor(
    InstrumentSearchModel model,
    InstrumentCatalog catalog,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.catalog = Objects.requireNonNull(catalog, "catalog cannot be null");
    this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor cannot be null");
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
  }

  /**
   * Initializes the current symbol and closes the search dialog.
   *
   * @param currentSymbol the initial chart symbol
   */
  void initialize(String currentSymbol) {
    setCurrentSymbol(currentSymbol);
    model.setOpen(false);
  }

  /** Opens the instrument-search dialog. */
  void show() {
    model.setOpen(true);
  }

  /** Closes the instrument-search dialog. */
  void close() {
    model.setOpen(false);
  }

  /**
   * Updates the current symbol and uses it as the search query.
   *
   * @param symbol the active chart symbol
   */
  void setCurrentSymbol(String symbol) {
    String value = Objects.requireNonNull(symbol, "symbol cannot be null");

    model.setCurrentSymbol(value);
  }

  /** Loads the instrument catalog once and publishes its result on the UI executor. */
  void loadCatalog() {
    if (model.getLoadState() != InstrumentSearchModel.LoadState.NOT_LOADED) {
      return;
    }
    model.setLoadState(InstrumentSearchModel.LoadState.LOADING);

    CompletableFuture.supplyAsync(catalog::getInstruments, backgroundExecutor).whenComplete((instruments, failure) ->
      uiExecutor.execute(() -> {
        if (failure != null) {
          model.setLoadState(InstrumentSearchModel.LoadState.FAILED);
          return;
        }
        model.setInstruments(List.copyOf(instruments));
        model.setLoadState(InstrumentSearchModel.LoadState.LOADED);
      })
    );
  }

  /**
   * Registers the action invoked after an instrument is selected.
   *
   * @param callback the selected-symbol callback
   */
  void onInstrumentSelected(Consumer<String> callback) {
    instrumentSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when the search dialog requests external closure.
   *
   * @param callback the close-request callback
   */
  void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Applies a user-selected instrument, closes the dialog, and notifies its listener.
   *
   * @param instrument the selected instrument
   */
  void selectInstrument(Instrument instrument) {
    String symbol = Objects.requireNonNull(instrument, "instrument cannot be null").symbol();

    setCurrentSymbol(symbol);
    close();
    instrumentSelectedHandler.accept(symbol);
  }

  /** Closes the dialog and notifies the external close-request listener. */
  void requestClose() {
    close();
    closeRequestHandler.run();
  }
}
