package com.acteque.terminal.chartworkspace.instrumentsearch;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.command.Command;
import com.acteque.terminal.ui.command.CommandDialog;
import com.acteque.terminal.ui.command.CommandEmpty;
import com.acteque.terminal.ui.command.CommandInput;
import com.acteque.terminal.ui.command.CommandItem;
import com.acteque.terminal.ui.command.CommandList;
import com.acteque.terminal.ui.dialog.Dialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.Builder;

/** Builds the instrument-search view from the shared command component family. */
final class InstrumentSearchViewBuilder implements Builder<Dialog>, ReloadTarget {

  private static final String DIALOG_TITLE = "Instrument search";
  private static final String DIALOG_DESCRIPTION = "Search instruments by symbol or exchange";
  private static final int MAX_RENDERED_RESULTS = 50;

  private final InstrumentSearchModel model;
  private final Consumer<Instrument> instrumentSelectedHandler;
  private final Command command = new Command();
  private final CommandInput input = new CommandInput(command);
  private final CommandEmpty empty = new CommandEmpty(command, "Loading instruments…");
  private final CommandList instruments = new CommandList(command, empty);
  private final CommandDialog root = new CommandDialog(command, DIALOG_TITLE, DIALOG_DESCRIPTION);
  private final List<CommandItem> instrumentItems = new ArrayList<>();

  /**
   * Creates and connects the instrument-search command composition.
   *
   * @param model the observable instrument-search state
   * @param catalogRequestedHandler the catalog-load callback
   * @param instrumentSelectedHandler the instrument-selection callback
   * @param closeRequestHandler the dialog-close callback
   */
  InstrumentSearchViewBuilder(
    InstrumentSearchModel model,
    Runnable catalogRequestedHandler,
    Consumer<Instrument> instrumentSelectedHandler,
    Runnable closeRequestHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.instrumentSelectedHandler = Objects.requireNonNull(
      instrumentSelectedHandler,
      "instrumentSelectedHandler cannot be null"
    );
    Runnable validatedCatalogRequestedHandler = Objects.requireNonNull(
      catalogRequestedHandler,
      "catalogRequestedHandler cannot be null"
    );
    Runnable validatedCloseRequestHandler = Objects.requireNonNull(
      closeRequestHandler,
      "closeRequestHandler cannot be null"
    );

    configureView();
    connectComponents(validatedCatalogRequestedHandler, validatedCloseRequestHandler);
    refreshView();
    displayOpenState(model.isOpen(), validatedCatalogRequestedHandler);
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

  /** Rebuilds reloadable command content while retaining model listeners and controls. */
  @Override
  public void refreshView() {
    input.getEditor().setPromptText("Symbol");
    input.getEditor().setAccessibleText("Stock symbol");
    command.getChildren().setAll(input, instruments);
    command.setSearchText(model.getCurrentSymbol());
    rebuildInstrumentItems();
    updateEmptyState();
  }

  /** Configures the command dialog's stable structural styling. */
  private void configureView() {
    root.getStyleClass().add("instrument-search-dialog");
    command.getStyleClass().add("instrument-search-command");
    command.setFilter(null);
    instruments.getStyleClass().add("instrument-search-list");
  }

  /**
   * Connects model observations and forwards control intents to the interactor.
   *
   * @param catalogRequestedHandler the validated catalog-load callback
   * @param closeRequestHandler the validated dialog-close callback
   */
  private void connectComponents(Runnable catalogRequestedHandler, Runnable closeRequestHandler) {
    command.searchTextProperty().addListener(ignored -> rebuildInstrumentItems());
    model.currentSymbolProperty().addListener((ignored, previous, current) -> command.setSearchText(current));
    model.instrumentsProperty().addListener((ListChangeListener<Instrument>) change -> rebuildInstrumentItems());
    model.loadStateProperty().addListener(ignored -> updateEmptyState());
    model.openProperty().addListener((ignored, wasOpen, isOpen) -> displayOpenState(isOpen, catalogRequestedHandler));
    root.openProperty().addListener((ignored, wasOpen, isOpen) -> {
      if (!isOpen && model.isOpen()) {
        closeRequestHandler.run();
      }
    });
  }

  /**
   * Mirrors model visibility into the dialog and prepares the command input when opened.
   *
   * @param open true to open the dialog
   * @param catalogRequestedHandler the catalog-load callback
   */
  private void displayOpenState(boolean open, Runnable catalogRequestedHandler) {
    root.setOpen(open);
    if (!open) {
      return;
    }

    command.setSearchText(model.getCurrentSymbol());
    input.getEditor().selectAll();
    Platform.runLater(() -> {
      input.getEditor().requestFocus();
      input.getEditor().selectAll();
    });
    catalogRequestedHandler.run();
  }

  /** Replaces the rendered commands with a bounded window of matching catalog entries. */
  private void rebuildInstrumentItems() {
    instrumentItems.forEach(CommandItem::dispose);
    instrumentItems.clear();
    instruments.getEntries().setAll(empty);

    String normalizedQuery = command.getSearchText().strip().toLowerCase(Locale.ROOT);
    for (Instrument instrument : model.instrumentsProperty()) {
      if (!matches(instrument, normalizedQuery)) {
        continue;
      }
      CommandItem item = createInstrumentItem(instrument);
      instrumentItems.add(item);
      instruments.getEntries().add(item);
      if (instrumentItems.size() == MAX_RENDERED_RESULTS) {
        break;
      }
    }
  }

  /**
   * Creates one searchable and invokable instrument command.
   *
   * @param instrument the instrument represented by the command
   * @return the configured command item
   */
  private CommandItem createInstrumentItem(Instrument instrument) {
    Label symbol = new Label(instrument.symbol());
    Label description = new Label(instrument.name().orElse(""));
    Label exchange = new Label(instrument.exchange().orElse(""));
    HBox row = new HBox(symbol, description, exchange);

    row.getStyleClass().add("instrument-search-row");
    row.setMaxWidth(Double.MAX_VALUE);
    symbol.getStyleClass().add("instrument-search-symbol");
    description.getStyleClass().add("instrument-search-description");
    description.setMaxWidth(Double.MAX_VALUE);
    HBox.setHgrow(description, Priority.ALWAYS);
    exchange.getStyleClass().add("instrument-search-exchange");
    HBox.setHgrow(row, Priority.ALWAYS);

    CommandItem item = new CommandItem(command, instrument.symbol(), row);
    instrument.exchange().ifPresent(item.getKeywords()::add);
    item.setOnAction(ignored -> instrumentSelectedHandler.accept(instrument));
    return item;
  }

  /**
   * Reports whether an instrument matches the normalized command query.
   *
   * @param instrument the candidate instrument
   * @param normalizedQuery the stripped, lower-case command query
   * @return true when the symbol or exchange contains the query
   */
  private static boolean matches(Instrument instrument, String normalizedQuery) {
    return (
      normalizedQuery.isEmpty() ||
      instrument.symbol().toLowerCase(Locale.ROOT).contains(normalizedQuery) ||
      instrument.exchange().orElse("").toLowerCase(Locale.ROOT).contains(normalizedQuery)
    );
  }

  /** Displays placeholder text appropriate to the current catalog loading state. */
  private void updateEmptyState() {
    empty.setText(
      switch (model.getLoadState()) {
        case NOT_LOADED, LOADING -> "Loading instruments…";
        case LOADED -> "No matching instruments";
        case FAILED -> "Unable to load instruments";
      }
    );
  }
}
