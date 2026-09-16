package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.HistoricalBarData;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.InstrumentDiscovery;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.provider.tiingo.iex.TiingoIexApi;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.marketdata.provider.tiingo.utilities.TiingoTickerSearchResult;
import com.acteque.terminal.marketdata.provider.tiingo.utilities.TiingoUtilitiesApi;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Optional;

/** Tiingo facade exposing endpoint-aligned daily and IEX APIs. */
public final class TiingoMarketDataClient implements MarketDataClient {

  private final HttpClient httpClient;

  /** Tiingo's {@code /tiingo/daily} endpoint module. */
  public final TiingoDailyApi daily;

  /** Tiingo's {@code /iex} endpoint module. */
  public final TiingoIexApi iex;

  /** Tiingo's supported-ticker catalog module. */
  public final TiingoTickerCatalogApi tickerCatalog;

  /** Provider-neutral view of Tiingo's supported-instrument catalog. */
  private final InstrumentCatalog instrumentCatalog;

  private final TiingoUtilitiesApi utilities;
  private final HistoricalBarData historicalBars;
  private final InstrumentDiscovery discovery;

  /** Creates a client for Tiingo's daily and IEX APIs. */
  public TiingoMarketDataClient(String apiKey) {
    String validatedKey = requireApiKey(apiKey);
    httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
    TiingoRequestExecutor requests = new TiingoRequestExecutor(validatedKey, TiingoHttpTransport.using(httpClient));
    daily = TiingoDailyApi.usingDefaults(requests);
    iex = TiingoIexApi.usingDefaults(requests);
    tickerCatalog = TiingoTickerCatalogApi.usingDefaults(requests);
    instrumentCatalog = new TiingoInstrumentCatalog(tickerCatalog);
    utilities = TiingoUtilitiesApi.usingDefaults(requests);
    historicalBars = new TiingoHistoricalBarData(daily, iex);
    discovery = new TiingoInstrumentDiscovery(daily);
  }

  /** Test-only constructor for injecting a transport and local endpoint base URI. */
  TiingoMarketDataClient(String apiKey, URI baseUri, TiingoHttpTransport transport) {
    httpClient = null; // Injected transports remain owned by the caller.
    TiingoRequestExecutor requests = new TiingoRequestExecutor(requireApiKey(apiKey), transport);
    daily = new TiingoDailyApi(baseUri, requests);
    iex = new TiingoIexApi(baseUri, requests);
    tickerCatalog = new TiingoTickerCatalogApi(baseUri.resolve("/supported_tickers.zip"), requests);
    instrumentCatalog = new TiingoInstrumentCatalog(tickerCatalog);
    utilities = new TiingoUtilitiesApi(baseUri, requests);
    historicalBars = new TiingoHistoricalBarData(daily, iex);
    discovery = new TiingoInstrumentDiscovery(daily);
  }

  @Override
  public String provider() {
    return "tiingo";
  }

  @Override
  public Optional<HistoricalBarData> historicalBars() {
    return Optional.of(historicalBars);
  }

  @Override
  public Optional<InstrumentDiscovery> discovery() {
    return Optional.of(discovery);
  }

  @Override
  public Optional<InstrumentCatalog> catalog() {
    return Optional.of(instrumentCatalog);
  }

  @Override
  public void close() {
    if (httpClient != null) {
      httpClient.close();
    }
  }

  /** Searches Tiingo's utilities endpoint by ticker or asset name. */
  public List<TiingoTickerSearchResult> searchTickers(String query) {
    return utilities.searchTickers(query);
  }

  private static String requireApiKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException("Tiingo requires a nonblank API key");
    }
    return apiKey.strip();
  }
}
