package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.InstrumentDetails;
import com.acteque.terminal.marketdata.LoadedInstrument;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoRequest;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.core.Button;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

class ChartInstrumentLogosTest {

  @Test
  void composesIndependentSessionsAndDisplaysTheInstrumentEvenWhenLogoLoadingFails() {
    FxTestSupport.runAndWait(() -> {
      for (String outcome : List.of("pending", "missing", "resolution-failed", "download-failed")) {
        StubMarketData marketData = new StubMarketData();
        StubLogos logos = new StubLogos(outcome);
        try (
          Chart chart = new Chart(List.of(), "IBM", ChartInterval.DAILY, List::of, marketData, logos, Runnable::run)
        ) {
          var view = chart.getView();
          chart.loadInitialInstrument();
          assertEquals("Provider Name", ((Button) view.lookup(".chart-symbol-button")).getText());
          assertEquals(new LogoRequest("Provider:Ab.C", Optional.of("Market")), logos.request);
          assertEquals(outcome.equals("pending") || outcome.equals("download-failed") ? 1 : 0, logos.loads);
          if (outcome.equals("pending")) {
            assertFalse(logos.pending.isDone());
          }
        }
        assertTrue(marketData.closed);
        assertTrue(logos.closed);
        assertTrue(logos.pending.isCancelled() || logos.pending.isCompletedExceptionally());
      }
    });
  }

  private static final class StubMarketData implements MarketDataSession {

    private boolean closed;

    public CompletableFuture<LoadedInstrument> loadInitial() {
      return CompletableFuture.completedFuture(
        new LoadedInstrument(
          "IBM",
          "Provider Name",
          List.of(),
          new InstrumentDetails("Provider:Ab.C", Optional.of("Provider Name"), Optional.of("Market"), Optional.empty())
        )
      );
    }

    public CompletableFuture<LoadedInstrument> loadInstrument(String symbol) {
      return loadInitial();
    }

    public CompletableFuture<List<DailyBar>> loadEarlier() {
      return CompletableFuture.completedFuture(List.of());
    }

    public void close() {
      closed = true;
    }
  }

  private static final class StubLogos implements LogoSession {

    private final String outcome;
    private final CompletableFuture<Optional<byte[]>> pending = new CompletableFuture<>();
    private LogoRequest request;
    private int loads;
    private boolean closed;

    private StubLogos(String outcome) {
      this.outcome = outcome;
    }

    public Optional<InstrumentLogo> findLogo(LogoRequest request) {
      this.request = request;
      if (outcome.equals("resolution-failed")) {
        throw new LogoException(LogoException.Code.PROVIDER_ERROR, "lookup unavailable");
      }
      return outcome.equals("missing")
        ? Optional.empty()
        : Optional.of(
            new InstrumentLogo(
              URI.create("https://images.example.com/logo.png"),
              "Independent logos",
              URI.create("https://example.com")
            )
          );
    }

    public CompletableFuture<Optional<byte[]>> load(InstrumentLogo logo) {
      loads++;
      if (outcome.equals("download-failed")) {
        pending.completeExceptionally(new LogoException(LogoException.Code.NETWORK, "offline"));
      }
      return pending;
    }

    public void close() {
      closed = true;
      pending.cancel(false);
    }
  }
}
