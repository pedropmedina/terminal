package com.acteque.terminal.search;

import com.acteque.terminal.marketdata.Instrument;
import java.util.List;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;

/** Observable state shared by the instrument-search MVCI components. */
final class InstrumentSearchModel {

  enum LoadState {
    NOT_LOADED,
    LOADING,
    LOADED,
    FAILED,
  }

  private final ReadOnlyStringWrapper currentSymbol = new ReadOnlyStringWrapper(this, "currentSymbol", "");
  private final ReadOnlyStringWrapper query = new ReadOnlyStringWrapper(this, "query", "");
  private final ReadOnlyListWrapper<Instrument> instruments = new ReadOnlyListWrapper<>(
    this,
    "instruments",
    FXCollections.observableArrayList()
  );
  private final ReadOnlyListWrapper<Instrument> matchingInstruments = new ReadOnlyListWrapper<>(
    this,
    "matchingInstruments",
    FXCollections.observableArrayList()
  );
  private final ReadOnlyObjectWrapper<LoadState> loadState = new ReadOnlyObjectWrapper<>(
    this,
    "loadState",
    LoadState.NOT_LOADED
  );

  String getCurrentSymbol() {
    return currentSymbol.get();
  }

  ReadOnlyStringProperty currentSymbolProperty() {
    return currentSymbol.getReadOnlyProperty();
  }

  void setCurrentSymbol(String value) {
    currentSymbol.set(value);
  }

  String getQuery() {
    return query.get();
  }

  void setQuery(String value) {
    query.set(value);
  }

  ReadOnlyListProperty<Instrument> instrumentsProperty() {
    return instruments.getReadOnlyProperty();
  }

  void setInstruments(List<Instrument> values) {
    instruments.setAll(values);
  }

  ReadOnlyListProperty<Instrument> matchingInstrumentsProperty() {
    return matchingInstruments.getReadOnlyProperty();
  }

  void setMatchingInstruments(List<Instrument> values) {
    matchingInstruments.setAll(values);
  }

  LoadState getLoadState() {
    return loadState.get();
  }

  ReadOnlyObjectProperty<LoadState> loadStateProperty() {
    return loadState.getReadOnlyProperty();
  }

  void setLoadState(LoadState value) {
    loadState.set(value);
  }
}
