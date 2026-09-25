package com.acteque.terminal.chartworkspace.instrumentsearch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.StubInstrumentCatalog;
import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.command.Command;
import com.acteque.terminal.ui.command.CommandDialog;
import com.acteque.terminal.ui.command.CommandEmpty;
import com.acteque.terminal.ui.command.CommandItem;
import com.acteque.terminal.ui.command.CommandList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.Test;

class InstrumentSearchTest {

  private static final Instrument APPLE = instrument("AAPL", "Apple Inc.", "NASDAQ");
  private static final Instrument IBM = instrument("IBM", "International Business Machines", "NYSE");

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

  @Test
  void composesCommandItemsAndFiltersBySymbolOrExchange() {
    FxTestSupport.runAndWait(() -> {
      InstrumentSearch search = search(List.of(APPLE, IBM));
      CommandDialog dialog = assertInstanceOf(CommandDialog.class, search.getView());
      Command command = dialog.getCommand();

      search.show();

      assertEquals("IBM", command.getSearchText());
      assertEquals("Instrument search", dialog.getAccessibleText());
      assertEquals("Search instruments by symbol or exchange", dialog.getAccessibleHelp());

      CommandList list = assertInstanceOf(CommandList.class, command.getChildren().get(1));
      CommandEmpty empty = assertInstanceOf(CommandEmpty.class, list.getEntries().getFirst());
      CommandItem ibm = assertInstanceOf(CommandItem.class, list.getEntries().get(1));
      assertEquals(2, list.getEntries().size());
      assertSame(ibm, command.getHighlightedItem());

      command.setSearchText("nas");
      CommandItem apple = assertInstanceOf(CommandItem.class, list.getEntries().get(1));
      assertEquals(2, list.getEntries().size());
      assertTrue(apple.isVisible());
      assertFalse(empty.isVisible());

      command.setSearchText("Apple Inc.");
      assertEquals(1, list.getEntries().size());
      assertTrue(empty.isVisible());
      assertEquals("No matching instruments", empty.getText());
    });
  }

  @Test
  void boundsRenderedCommandsWhileSearchingTheEntireCatalog() {
    FxTestSupport.runAndWait(() -> {
      List<Instrument> catalog = IntStream.range(0, 1_000)
        .mapToObj(index -> instrument("SYM%04d".formatted(index), "Instrument " + index, "NASDAQ"))
        .toList();
      InstrumentSearch search = search(catalog);
      Command command = assertInstanceOf(CommandDialog.class, search.getView()).getCommand();
      CommandList list = assertInstanceOf(CommandList.class, command.getChildren().get(1));

      search.show();
      command.setSearchText("SYM");

      assertEquals(51, list.getEntries().size());

      command.setSearchText("SYM0999");

      assertEquals(2, list.getEntries().size());
      CommandItem match = assertInstanceOf(CommandItem.class, list.getEntries().get(1));
      assertEquals("SYM0999", match.getValue());
    });
  }

  @Test
  void alignsInstrumentItemBackgroundWithTheSearchInput() {
    FxTestSupport.runAndWait(() -> {
      List<Instrument> catalog = IntStream.range(0, 100)
        .mapToObj(index -> instrument("SYM%04d".formatted(index), "Instrument " + index, "NASDAQ"))
        .toList();
      InstrumentSearch search = search(catalog);
      StackPane root = new StackPane(search.getView());
      new AppThemeManager(new Scene(root, 800.0, 600.0), AppTheme.LIGHT);

      search.show();
      assertInstanceOf(CommandDialog.class, search.getView()).getCommand().setSearchText("");
      root.applyCss();
      root.layout();

      Region input = assertInstanceOf(Region.class, root.lookup(".core-command-input-group"));
      Region item = assertInstanceOf(Region.class, root.lookup(".core-command-item"));
      Bounds inputBounds = input.localToScene(input.getBoundsInLocal());
      Bounds itemBounds = item.localToScene(item.getBoundsInLocal());

      assertEquals(inputBounds.getMinX(), itemBounds.getMinX(), 0.01);
      assertEquals(inputBounds.getMaxX(), itemBounds.getMaxX(), 0.01);
    });
  }

  @Test
  void routesCommandInvocationThroughInstrumentSelection() {
    FxTestSupport.runAndWait(() -> {
      InstrumentSearch search = search(List.of(APPLE, IBM));
      AtomicReference<String> selected = new AtomicReference<>();
      search.onInstrumentSelected(selected::set);
      search.show();

      Command command = assertInstanceOf(CommandDialog.class, search.getView()).getCommand();
      command.setSearchText("AAPL");
      command.getHighlightedItem().fire();

      assertEquals("AAPL", selected.get());
      assertEquals("AAPL", command.getSearchText());
      assertFalse(search.openProperty().get());
      assertFalse(search.getView().isOpen());
    });
  }

  @Test
  void displaysCatalogFailureInTheCommandEmptyState() {
    FxTestSupport.runAndWait(() -> {
      InstrumentSearch search = new InstrumentSearch(
        "IBM",
        new StubInstrumentCatalog(() -> {
          throw new IllegalStateException("Test catalog failure");
        }),
        Runnable::run,
        Runnable::run
      );

      search.show();

      Command command = assertInstanceOf(CommandDialog.class, search.getView()).getCommand();
      CommandList list = assertInstanceOf(CommandList.class, command.getChildren().get(1));
      CommandEmpty empty = assertInstanceOf(CommandEmpty.class, list.getEntries().getFirst());
      assertTrue(empty.isVisible());
      assertEquals("Unable to load instruments", empty.getText());
    });
  }

  private static InstrumentSearch search() {
    return search(List.of());
  }

  private static InstrumentSearch search(List<Instrument> instruments) {
    return new InstrumentSearch("IBM", new StubInstrumentCatalog(() -> instruments), Runnable::run, Runnable::run);
  }

  private static Instrument instrument(String symbol, String name, String exchange) {
    return new Instrument(symbol, Optional.of(name), Optional.of(exchange), Optional.empty());
  }
}
