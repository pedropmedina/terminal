package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import java.util.List;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;

/** Observable state shared by the instrument-search MVCI components. */
final class InstrumentSearchModel {

  /** Describes the current instrument-catalog loading state. */
  enum LoadState {
    /** The catalog has not been requested. */
    NOT_LOADED,
    /** The catalog request is in progress. */
    LOADING,
    /** The catalog loaded successfully. */
    LOADED,
    /** The catalog request failed. */
    FAILED,
  }

  private final ReadOnlyStringWrapper currentSymbol = new ReadOnlyStringWrapper(this, "currentSymbol", "");
  private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");
  private final ReadOnlyListWrapper<Instrument> instruments = new ReadOnlyListWrapper<>(
    this,
    "instruments",
    FXCollections.observableArrayList()
  );
  private final ReadOnlyObjectWrapper<LoadState> loadState = new ReadOnlyObjectWrapper<>(
    this,
    "loadState",
    LoadState.NOT_LOADED
  );

  /**
   * Returns the symbol displayed as current.
   *
   * @return the current symbol
   */
  String getCurrentSymbol() {
    return currentSymbol.get();
  }

  /**
   * Returns the observable current symbol.
   *
   * @return the read-only current-symbol property
   */
  ReadOnlyStringProperty currentSymbolProperty() {
    return currentSymbol.getReadOnlyProperty();
  }

  /**
   * Updates the symbol displayed as current.
   *
   * @param value the current symbol
   */
  void setCurrentSymbol(String value) {
    currentSymbol.set(value);
  }

  /**
   * Reports whether the search dialog is open.
   *
   * @return true when the search dialog is open
   */
  boolean isOpen() {
    return open.get();
  }

  /**
   * Returns the observable dialog state.
   *
   * @return the read-only open property
   */
  ReadOnlyBooleanProperty openProperty() {
    return open.getReadOnlyProperty();
  }

  /**
   * Updates the dialog state.
   *
   * @param value true to open the search dialog
   */
  void setOpen(boolean value) {
    open.set(value);
  }

  /**
   * Returns the loaded instrument catalog.
   *
   * @return the read-only instrument-list property
   */
  ReadOnlyListProperty<Instrument> instrumentsProperty() {
    return instruments.getReadOnlyProperty();
  }

  /**
   * Replaces the loaded instrument catalog.
   *
   * @param values the loaded instruments
   */
  void setInstruments(List<Instrument> values) {
    instruments.setAll(values);
  }

  /**
   * Returns the current catalog loading state.
   *
   * @return the catalog loading state
   */
  LoadState getLoadState() {
    return loadState.get();
  }

  /**
   * Returns the observable catalog loading state.
   *
   * @return the read-only load-state property
   */
  ReadOnlyObjectProperty<LoadState> loadStateProperty() {
    return loadState.getReadOnlyProperty();
  }

  /**
   * Updates the catalog loading state.
   *
   * @param value the catalog loading state
   */
  void setLoadState(LoadState value) {
    loadState.set(value);
  }
}
