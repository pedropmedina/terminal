package com.acteque.terminal.search;

import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.InstrumentDetails;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

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
    this.model = Objects.requireNonNull(model, "model");
    this.catalog = Objects.requireNonNull(catalog, "catalog");
    this.backgroundExecutor = Objects.requireNonNull(backgroundExecutor, "backgroundExecutor");
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
  }

  void initialize(String currentSymbol) {
    setCurrentSymbol(currentSymbol);
  }

  void setCurrentSymbol(String symbol) {
    String value = Objects.requireNonNull(symbol, "symbol");
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

    CompletableFuture.supplyAsync(catalog::getSupportedInstruments, backgroundExecutor).whenComplete(
      (instruments, failure) ->
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

  void select(InstrumentDetails instrument) {
    setCurrentSymbol(Objects.requireNonNull(instrument, "instrument").symbol());
  }

  private static boolean matches(InstrumentDetails instrument, String normalizedQuery) {
    return (
      normalizedQuery.isEmpty() ||
      instrument.symbol().toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
      instrument.exchange().orElse("").toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
  }
}
