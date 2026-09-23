package com.acteque.terminal.chartworkspace.instrumentsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.test.FxTestSupport;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InstrumentSearchTest {

  @Test
  void mirrorsProgrammaticVisibilityIntoTheDialog() {
    FxTestSupport.runAndWait(() -> {
      InstrumentSearch search = search();

      assertFalse(search.openProperty().get());
      assertFalse(search.getView().isOpen());

      search.show();
      assertTrue(search.openProperty().get());
      assertTrue(search.getView().isOpen());

      search.close();
      assertFalse(search.openProperty().get());
      assertFalse(search.getView().isOpen());
    });
  }

  @Test
  void mirrorsDialogDismissalsIntoStateAndRoutesTheCloseRequest() {
    FxTestSupport.runAndWait(() -> {
      InstrumentSearch search = search();
      AtomicInteger requests = new AtomicInteger();
      search.onRequestClose(requests::incrementAndGet);
      search.show();

      search.getView().close();

      assertFalse(search.openProperty().get());
      assertFalse(search.getView().isOpen());
      assertEquals(1, requests.get());
    });
  }

  private static InstrumentSearch search() {
    return new InstrumentSearch("IBM", new StubInstrumentCatalog(List::of), Runnable::run, Runnable::run);
  }
}
