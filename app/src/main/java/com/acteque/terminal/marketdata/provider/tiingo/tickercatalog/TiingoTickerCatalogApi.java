package com.acteque.terminal.marketdata.provider.tiingo.tickercatalog;

import com.acteque.terminal.marketdata.provider.tiingo.TiingoRequestExecutor;
import java.net.URI;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Client for Tiingo's daily supported-ticker catalog. */
public final class TiingoTickerCatalogApi {

  private static final URI DEFAULT_CATALOG_URI = URI.create(
    "https://apimedia.tiingo.com/docs/tiingo/daily/supported_tickers.zip"
  );

  private final URI catalogUri;
  private final TiingoRequestExecutor requests;
  private final Clock clock;

  private volatile SupportedTickerSnapshot snapshot;

  public static TiingoTickerCatalogApi usingDefaults(TiingoRequestExecutor requests) {
    return new TiingoTickerCatalogApi(DEFAULT_CATALOG_URI, requests, Clock.systemDefaultZone());
  }

  public TiingoTickerCatalogApi(URI catalogUri, TiingoRequestExecutor requests) {
    this(catalogUri, requests, Clock.systemDefaultZone());
  }

  public TiingoTickerCatalogApi(URI catalogUri, TiingoRequestExecutor requests, Clock clock) {
    this.catalogUri = Objects.requireNonNull(catalogUri, "catalogUri");
    this.requests = Objects.requireNonNull(requests, "requests");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Returns the catalog, cached for one local calendar day. */
  public List<TiingoSupportedTicker> getSupportedTickers() {
    LocalDate today = LocalDate.now(clock);
    SupportedTickerSnapshot current = snapshot;
    if (current != null && current.fetchedOn().equals(today)) {
      return current.tickers();
    }

    synchronized (this) {
      current = snapshot;
      if (current == null || !current.fetchedOn().equals(today)) {
        List<TiingoSupportedTicker> tickers = TiingoSupportedTickerParser.parse(
          requests.getBytes(catalogUri, "application/zip", false)
        );
        current = new SupportedTickerSnapshot(today, tickers);
        snapshot = current;
      }
      return current.tickers();
    }
  }

  private record SupportedTickerSnapshot(LocalDate fetchedOn, List<TiingoSupportedTicker> tickers) {}
}
