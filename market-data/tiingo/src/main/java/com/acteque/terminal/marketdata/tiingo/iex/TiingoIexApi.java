package com.acteque.terminal.marketdata.tiingo.iex;

import com.acteque.terminal.marketdata.IntradayData;
import com.acteque.terminal.marketdata.IntradayRequest;
import com.acteque.terminal.marketdata.MarketDataException;
import com.acteque.terminal.marketdata.tiingo.TiingoRequestExecutor;
import com.acteque.terminal.marketdata.tiingo.TiingoUris;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Client for Tiingo's {@code /iex} API. */
public final class TiingoIexApi {

  private static final URI DEFAULT_BASE_URI = URI.create("https://api.tiingo.com");

  private final URI baseUri;
  private final TiingoRequestExecutor requests;

  public static TiingoIexApi usingDefaults(TiingoRequestExecutor requests) {
    return new TiingoIexApi(DEFAULT_BASE_URI, requests);
  }

  public TiingoIexApi(URI baseUri, TiingoRequestExecutor requests) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri cannot be null");
    this.requests = Objects.requireNonNull(requests, "requests cannot be null");
  }

  /** Returns top-of-book and last-sale snapshots for all IEX tickers. */
  public List<TiingoIexLastPrice> getLastPrices() {
    return TiingoIexJsonParser.parseLastPrices(requests.getJson(baseUri.resolve("/iex")));
  }

  /** Returns the top-of-book and last-sale snapshot for one IEX ticker. */
  public TiingoIexLastPrice getLastPrice(String ticker) {
    URI uri = baseUri.resolve("/iex/" + TiingoUris.ticker(ticker));
    List<TiingoIexLastPrice> prices = TiingoIexJsonParser.parseLastPrices(requests.getJson(uri));
    if (prices.size() != 1) {
      throw new MarketDataException(
        MarketDataException.Code.INVALID_RESPONSE,
        "Expected one Tiingo IEX last price but received " + prices.size()
      );
    }
    return prices.getFirst();
  }

  public List<IntradayData> getPrices(
    String ticker,
    LocalDate startDate,
    LocalDate endDate,
    Duration resampleFrequency
  ) {
    return getPrices(new IntradayRequest(ticker, startDate, endDate, resampleFrequency));
  }

  public List<IntradayData> getPrices(IntradayRequest request) {
    Objects.requireNonNull(request, "request cannot be null");
    String query =
      "startDate=" +
      request.startDate() +
      "&endDate=" +
      request.endDate() +
      "&resampleFreq=" +
      resampleFrequency(request.interval()) +
      "&columns=open,high,low,close,volume" +
      "&afterHours=" +
      request.includeAfterHours() +
      "&forceFill=" +
      request.forceFill() +
      "&format=json";
    URI uri = baseUri.resolve("/iex/" + TiingoUris.ticker(request.symbol()) + "/prices?" + query);
    return TiingoIexJsonParser.parsePrices(request.symbol(), requests.getJson(uri));
  }

  private static String resampleFrequency(Duration interval) {
    long minutes = interval.toMinutes();
    if (minutes % 60 == 0) {
      return minutes / 60 + "hour";
    }
    return minutes + "min";
  }
}
