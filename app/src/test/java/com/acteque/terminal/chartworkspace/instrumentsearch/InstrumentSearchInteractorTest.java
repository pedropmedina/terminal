package com.acteque.terminal.chartworkspace.instrumentsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.marketdata.Instrument;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class InstrumentSearchInteractorTest {

  private static final Instrument APPLE = instrument("AAPL", "NASDAQ");
  private static final Instrument IBM = instrument("IBM", "NYSE");

  @Test
  void initializesClosedAndControlsWorkspaceVisibility() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of());

    interactor.initialize("IBM");
    assertFalse(model.isOpen());

    interactor.show();
    assertTrue(model.isOpen());

    interactor.close();
    assertFalse(model.isOpen());
  }

  @Test
  void initializesCurrentSymbolAndLoadsCatalog() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of(APPLE, IBM));

    interactor.initialize("IBM");
    interactor.loadCatalog();

    assertEquals("IBM", model.getCurrentSymbol());
    assertEquals(List.of(APPLE, IBM), List.copyOf(model.instrumentsProperty()));
    assertSame(InstrumentSearchModel.LoadState.LOADED, model.getLoadState());
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
  void selectionUpdatesStateClosesAndRoutesTheSelectedSymbol() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of());
    AtomicReference<String> selected = new AtomicReference<>();
    interactor.onInstrumentSelected(selected::set);
    interactor.show();

    interactor.selectInstrument(APPLE);

    assertEquals("AAPL", model.getCurrentSymbol());
    assertFalse(model.isOpen());
    assertEquals("AAPL", selected.get());
  }

  @Test
  void closeRequestsUpdateStateAndNotifyTheListener() {
    InstrumentSearchModel model = new InstrumentSearchModel();
    InstrumentSearchInteractor interactor = interactor(model, List.of());
    AtomicInteger requests = new AtomicInteger();
    interactor.onRequestClose(requests::incrementAndGet);
    interactor.show();

    interactor.requestClose();

    assertFalse(model.isOpen());
    assertEquals(1, requests.get());
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
