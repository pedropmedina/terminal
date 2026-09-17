package com.acteque.terminal.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.marketdata.Instrument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InstrumentSearchInteractorTest {

  private static final Instrument APPLE = instrument("AAPL", "NASDAQ");
  private static final Instrument IBM = instrument("IBM", "NYSE");

  @Test
  void initializesAndFiltersNormalizedQueriesBySymbolOrExchange() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of(APPLE, IBM));

    interactor.initialize("IBM");
    interactor.loadCatalog();

    assertEquals("IBM", model.getCurrentSymbol());
    assertEquals("ibm", model.getQuery());
    assertEquals(List.of(IBM), List.copyOf(model.matchingInstrumentsProperty()));
    assertSame(InstrumentSearchModel.LoadState.LOADED, model.getLoadState());

    interactor.setQuery("  nas  ");
    assertEquals("nas", model.getQuery());
    assertEquals(List.of(APPLE), List.copyOf(model.matchingInstrumentsProperty()));

    interactor.setQuery(null);
    assertEquals(List.of(APPLE, IBM), List.copyOf(model.matchingInstrumentsProperty()));
  }

  @Test
  void loadsCatalogOnlyOnce() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    AtomicInteger loads = new AtomicInteger();
    InstrumentSearchInteractor interactor = new InstrumentSearchInteractor(
      model,
      new StubInstrumentCatalog(() -> {
        loads.incrementAndGet();
        return List.of(APPLE);
      }),
      Runnable::run,
      Runnable::run
    );

    interactor.loadCatalog();
    interactor.loadCatalog();

    assertEquals(1, loads.get());
    assertSame(InstrumentSearchModel.LoadState.LOADED, model.getLoadState());
  }

  @Test
  void publishesSuccessAndFailureOnTheUiExecutor() {
    InstrumentSearchModel successfulModel = new InstrumentSearchModel();
    List<Runnable> successfulUiQueue = new ArrayList<>();
    InstrumentSearchInteractor successful = new InstrumentSearchInteractor(
      successfulModel,
      new StubInstrumentCatalog(() -> List.of(APPLE)),
      Runnable::run,
      successfulUiQueue::add
    );

    successful.loadCatalog();
    assertSame(InstrumentSearchModel.LoadState.LOADING, successfulModel.getLoadState());
    successfulUiQueue.removeFirst().run();
    assertSame(InstrumentSearchModel.LoadState.LOADED, successfulModel.getLoadState());

    InstrumentSearchModel failedModel = new InstrumentSearchModel();
    List<Runnable> failedUiQueue = new ArrayList<>();
    InstrumentSearchInteractor failed = new InstrumentSearchInteractor(
      failedModel,
      new StubInstrumentCatalog(() -> {
        throw new IllegalStateException("Test catalog failure");
      }),
      Runnable::run,
      failedUiQueue::add
    );

    failed.loadCatalog();
    assertSame(InstrumentSearchModel.LoadState.LOADING, failedModel.getLoadState());
    failedUiQueue.removeFirst().run();
    assertSame(InstrumentSearchModel.LoadState.FAILED, failedModel.getLoadState());
  }

  @Test
  void selectionUpdatesTheCurrentSymbolAndQuery() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of());

    interactor.select(APPLE);

    assertEquals("AAPL", model.getCurrentSymbol());
    assertEquals("aapl", model.getQuery());
  }

  private static InstrumentSearchInteractor interactor(InstrumentSearchModel model, List<Instrument> instruments) {
    return new InstrumentSearchInteractor(
      model,
      new StubInstrumentCatalog(() -> instruments),
      Runnable::run,
      Runnable::run
    );
  }

  private static Instrument instrument(String symbol, String exchange) {
    return new Instrument(symbol, Optional.empty(), Optional.of(exchange), Optional.empty());
  }
}
