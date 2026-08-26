package com.acteque.terminal.marketdata.provider.tiingo.eod;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoRequestExecutor;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoUris;

/** Client for Tiingo's {@code /tiingo/daily} API. */
public final class TiingoDailyApi {

  private static final URI DEFAULT_BASE_URI = URI.create("https://api.tiingo.com");

  private final URI baseUri;
  private final TiingoRequestExecutor requests;

  public static TiingoDailyApi usingDefaults(TiingoRequestExecutor requests) {
    return new TiingoDailyApi(DEFAULT_BASE_URI, requests);
  }

  public TiingoDailyApi(URI baseUri, TiingoRequestExecutor requests) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    this.requests = Objects.requireNonNull(requests, "requests");
  }

  public List<DailyBar> getBars(String ticker, LocalDate startDate, LocalDate endDate) {
    return getBars(new DailyBarRequest(ticker, startDate, endDate));
  }

  public List<DailyBar> getBars(
    String ticker,
    LocalDate startDate,
    LocalDate endDate,
    TiingoEodResampleFrequency resampleFrequency
  ) {
    return getBars(new DailyBarRequest(ticker, startDate, endDate), resampleFrequency);
  }

  public List<DailyBar> getBars(DailyBarRequest request) {
    return getBarsInternal(request, null);
  }

  public List<DailyBar> getBars(DailyBarRequest request, TiingoEodResampleFrequency resampleFrequency) {
    return getBarsInternal(request, Objects.requireNonNull(resampleFrequency, "resampleFrequency"));
  }

  /** Returns descriptive metadata from {@code /tiingo/daily/{ticker}}. */
  public TiingoTickerMetadata getTicker(String ticker) {
    URI uri = baseUri.resolve("/tiingo/daily/" + TiingoUris.ticker(ticker));
    return TiingoDailyJsonParser.parseMetadata(requests.getJson(uri));
  }

  private List<DailyBar> getBarsInternal(DailyBarRequest request, TiingoEodResampleFrequency resampleFrequency) {
    Objects.requireNonNull(request, "request");
    String query = "startDate=" + request.startDate() + "&endDate=" + request.endDate() + "&format=json";
    if (resampleFrequency != null) {
      query += "&resampleFreq=" + resampleFrequency.apiValue();
    }
    URI uri = baseUri.resolve("/tiingo/daily/" + TiingoUris.ticker(request.symbol()) + "/prices?" + query);
    return TiingoDailyJsonParser.parseBars(request.symbol(), requests.getJson(uri));
  }
}
