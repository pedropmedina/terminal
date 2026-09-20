package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javafx.beans.value.ObservableBooleanValue;

/** Applies instrument-search state transitions without depending on its layout. */
final class InstrumentSearchInteractor {

  private final InstrumentSearchModel model;
  private final InstrumentCatalog catalog;
  private final Executor backgroundExecutor;
  private final Executor uiExecutor;

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

  void initialize(String currentSymbol) {
    setCurrentSymbol(currentSymbol);
    model.setOpen(false);
  }

  ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  void show() {
    model.setOpen(true);
  }

  void close() {
    model.setOpen(false);
  }

  void setCurrentSymbol(String symbol) {
    String value = Objects.requireNonNull(symbol, "symbol cannot be null");
    model.setCurrentSymbol(value);
    setQuery(value);
  }

  void setQuery(String query) {
    String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    model.setQuery(normalizedQuery);
    model.setMatchingInstruments(
      model
        .instrumentsProperty()
        .stream()
        .filter(instrument -> matches(instrument, normalizedQuery))
        .toList()
    );
  }

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
        setQuery(model.getQuery());
        model.setLoadState(InstrumentSearchModel.LoadState.LOADED);
      })
    );
  }

  void select(Instrument instrument) {
    setCurrentSymbol(Objects.requireNonNull(instrument, "instrument cannot be null").symbol());
  }

  private static boolean matches(Instrument instrument, String normalizedQuery) {
    return (
      normalizedQuery.isEmpty() ||
      instrument.symbol().toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
      instrument.exchange().orElse("").toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
  }
}
