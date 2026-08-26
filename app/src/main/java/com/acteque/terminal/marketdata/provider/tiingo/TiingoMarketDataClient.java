package com.acteque.terminal.marketdata.provider.tiingo;

import java.net.URI;
import java.net.http.HttpClient;
import java.util.List;
import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.provider.tiingo.iex.TiingoIexApi;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import com.acteque.terminal.marketdata.provider.tiingo.utilities.TiingoTickerSearchResult;
import com.acteque.terminal.marketdata.provider.tiingo.utilities.TiingoUtilitiesApi;
import io.github.cdimascio.dotenv.Dotenv;

/** Tiingo facade exposing endpoint-aligned daily and IEX APIs. */
public final class TiingoMarketDataClient implements MarketDataClient {

  public static final String API_KEY_ENVIRONMENT_VARIABLE = "TIINGO_API_KEY";

  /** Tiingo's {@code /tiingo/daily} endpoint module. */
  public final TiingoDailyApi daily;

  /** Tiingo's {@code /iex} endpoint module. */
  public final TiingoIexApi iex;

  /** Tiingo's supported-ticker catalog module. */
  public final TiingoTickerCatalogApi tickerCatalog;

  private final TiingoUtilitiesApi utilities;

  /** Creates a client for Tiingo's daily and IEX APIs. */
  public TiingoMarketDataClient(String apiKey) {
    TiingoRequestExecutor requests = new TiingoRequestExecutor(
      requireApiKey(apiKey),
      TiingoHttpTransport.using(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build())
    );
    daily = TiingoDailyApi.usingDefaults(requests);
    iex = TiingoIexApi.usingDefaults(requests);
    tickerCatalog = TiingoTickerCatalogApi.usingDefaults(requests);
    utilities = TiingoUtilitiesApi.usingDefaults(requests);
  }

  /** Test-only constructor for injecting a transport and local endpoint base URI. */
  TiingoMarketDataClient(String apiKey, URI baseUri, TiingoHttpTransport transport) {
    TiingoRequestExecutor requests = new TiingoRequestExecutor(requireApiKey(apiKey), transport);
    daily = new TiingoDailyApi(baseUri, requests);
    iex = new TiingoIexApi(baseUri, requests);
    tickerCatalog = new TiingoTickerCatalogApi(baseUri.resolve("/supported_tickers.zip"), requests);
    utilities = new TiingoUtilitiesApi(baseUri, requests);
  }

  public static TiingoMarketDataClient create() {
    String apiKey = Dotenv.configure().ignoreIfMissing().load().get(API_KEY_ENVIRONMENT_VARIABLE);
    return new TiingoMarketDataClient(apiKey);
  }

  @Override
  public String provider() {
    return "tiingo";
  }

  @Override
  public List<DailyBar> getDailyBars(DailyBarRequest request) {
    return daily.getBars(request);
  }

  @Override
  public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
    return iex.getPrices(request);
  }

  /** Searches Tiingo's utilities endpoint by ticker or asset name. */
  public List<TiingoTickerSearchResult> searchTickers(String query) {
    return utilities.searchTickers(query);
  }

  private static String requireApiKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(API_KEY_ENVIRONMENT_VARIABLE + " must be configured before using Tiingo");
    }
    return apiKey.strip();
  }
}
