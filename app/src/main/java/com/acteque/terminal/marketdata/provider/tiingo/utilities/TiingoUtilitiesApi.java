package com.acteque.terminal.marketdata.provider.tiingo.utilities;

import com.acteque.terminal.marketdata.provider.tiingo.TiingoRequestExecutor;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoUris;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** Internal client for Tiingo's ticker-search utilities endpoint. */
public final class TiingoUtilitiesApi {

  private static final URI DEFAULT_BASE_URI = URI.create("https://api.tiingo.com");

  private final URI baseUri;
  private final TiingoRequestExecutor requests;

  public static TiingoUtilitiesApi usingDefaults(TiingoRequestExecutor requests) {
    return new TiingoUtilitiesApi(DEFAULT_BASE_URI, requests);
  }

  public TiingoUtilitiesApi(URI baseUri, TiingoRequestExecutor requests) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    this.requests = Objects.requireNonNull(requests, "requests");
  }

  public List<TiingoTickerSearchResult> searchTickers(String query) {
    URI uri = baseUri.resolve("/tiingo/utilities/search?query=" + TiingoUris.queryValue(query));
    return TiingoUtilitiesJsonParser.parseSearchResults(requests.getJson(uri));
  }
}
