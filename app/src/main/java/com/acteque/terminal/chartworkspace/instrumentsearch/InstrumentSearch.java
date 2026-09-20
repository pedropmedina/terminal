package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.ui.dialog.Dialog;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the instrument-search MVCI feature. */
public final class InstrumentSearch {

  private final InstrumentSearchInteractor interactor;
  private final InstrumentSearchViewBuilder viewBuilder;
  private Consumer<String> instrumentSelectedHandler = ignored -> {};
  private Runnable closeRequestHandler = () -> {};

  public InstrumentSearch(
    String currentSymbol,
    InstrumentCatalog catalog,
    Executor backgroundExecutor,
    Executor uiExecutor
  ) {
    InstrumentSearchModel model = new InstrumentSearchModel();
    interactor = new InstrumentSearchInteractor(model, catalog, backgroundExecutor, uiExecutor);
    interactor.initialize(currentSymbol);
    viewBuilder = new InstrumentSearchViewBuilder(
      model,
      interactor.openProperty(),
      interactor::setQuery,
      interactor::loadCatalog,
      this::select,
      this::requestClose
    );
  }

  public Dialog getView() {
    return viewBuilder.build();
  }

  public void setCurrentSymbol(String symbol) {
    interactor.setCurrentSymbol(symbol);
  }

  public ObservableBooleanValue openProperty() {
    return interactor.openProperty();
  }

  public void show() {
    interactor.show();
  }

  public void close() {
    interactor.close();
  }

  public void onInstrumentSelected(Consumer<String> callback) {
    instrumentSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  private void select(Instrument instrument) {
    interactor.select(instrument);
    close();
    instrumentSelectedHandler.accept(instrument.symbol());
  }

  private void requestClose() {
    closeRequestHandler.run();
  }
}
