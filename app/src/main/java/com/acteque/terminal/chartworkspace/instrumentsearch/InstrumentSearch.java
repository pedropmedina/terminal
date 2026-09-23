package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.ui.dialog.Dialog;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the instrument-search MVCI feature. */
public final class InstrumentSearch {

  private final InstrumentSearchModel model;
  private final InstrumentSearchInteractor interactor;
  private final InstrumentSearchViewBuilder viewBuilder;

  /**
   * Creates and connects the instrument-search MVCI components.
   *
   * @param currentSymbol the symbol initially displayed in the search field
   * @param catalog the source of searchable instruments
   * @param backgroundExecutor the executor used to load the catalog
   * @param uiExecutor the executor used to publish catalog results to JavaFX state
   */
  public InstrumentSearch(
    String currentSymbol,
    InstrumentCatalog catalog,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    model = new InstrumentSearchModel();
    interactor = new InstrumentSearchInteractor(model, catalog, backgroundExecutor, uiExecutor);
    interactor.initialize(currentSymbol);

    viewBuilder = new InstrumentSearchViewBuilder(
      model,
      interactor::setQuery,
      interactor::loadCatalog,
      interactor::selectInstrument,
      interactor::requestClose
    );
  }

  /**
   * Returns the composed instrument-search dialog.
   *
   * @return the instrument-search dialog
   */
  public Dialog getView() {
    return viewBuilder.build();
  }

  /**
   * Returns the observable dialog state.
   *
   * @return the read-only open state
   */
  public ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  /**
   * Updates the symbol displayed and used as the current search query.
   *
   * @param symbol the active chart symbol
   */
  public void setCurrentSymbol(String symbol) {
    interactor.setCurrentSymbol(symbol);
  }

  /** Opens the instrument-search dialog. */
  public void show() {
    interactor.show();
  }

  /** Closes the instrument-search dialog. */
  public void close() {
    interactor.close();
  }

  /**
   * Registers the action invoked after an instrument is selected.
   *
   * @param callback the selected-symbol callback
   */
  public void onInstrumentSelected(Consumer<String> callback) {
    interactor.onInstrumentSelected(callback);
  }

  /**
   * Registers the action invoked when the dialog requests external closure.
   *
   * @param callback the close-request callback
   */
  public void onRequestClose(Runnable callback) {
    interactor.onRequestClose(callback);
  }
}
