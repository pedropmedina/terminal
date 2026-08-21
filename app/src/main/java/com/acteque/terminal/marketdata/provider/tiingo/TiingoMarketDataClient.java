package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.MarketDataException;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Tiingo client for historical end-of-day and intraday market data. */
public final class TiingoMarketDataClient implements MarketDataClient {

  public static final String API_KEY_ENVIRONMENT_VARIABLE = "TIINGO_API_KEY";

  private static final URI DEFAULT_BASE_URI = URI.create("https://api.tiingo.com");

  private final String apiKey;
  private final URI baseUri;
  private final TiingoHttpTransport transport;
  private final TiingoIntradayFeed intradayFeed;

  /** Creates a client using Tiingo's production-recommended IEX intraday feed. */
  public TiingoMarketDataClient(String apiKey) {
    this(apiKey, TiingoIntradayFeed.IEX);
  }

  public TiingoMarketDataClient(String apiKey, TiingoIntradayFeed intradayFeed) {
    this(
      apiKey,
      DEFAULT_BASE_URI,
      TiingoHttpTransport.using(HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()),
      intradayFeed
    );
  }

  TiingoMarketDataClient(String apiKey, URI baseUri, TiingoHttpTransport transport) {
    this(apiKey, baseUri, transport, TiingoIntradayFeed.IEX);
  }

  TiingoMarketDataClient(
    String apiKey,
    URI baseUri,
    TiingoHttpTransport transport,
    TiingoIntradayFeed intradayFeed
  ) {
    this.apiKey = requireApiKey(apiKey);
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri");
    this.transport = Objects.requireNonNull(transport, "transport");
    this.intradayFeed = Objects.requireNonNull(intradayFeed, "intradayFeed");
  }

  public static TiingoMarketDataClient fromEnvironment() {
    return fromEnvironment(TiingoIntradayFeed.IEX);
  }

  public static TiingoMarketDataClient fromEnvironment(TiingoIntradayFeed intradayFeed) {
    String apiKey = Dotenv.configure().ignoreIfMissing().load().get(API_KEY_ENVIRONMENT_VARIABLE);
    return new TiingoMarketDataClient(apiKey, intradayFeed);
  }

  @Override
  public String provider() {
    return "tiingo";
  }

  @Override
  public List<DailyBar> getDailyBars(DailyBarRequest request) {
    return getDailyBarsInternal(request, null);
  }

  /** Returns end-of-day data resampled by Tiingo to the requested frequency. */
  public List<DailyBar> getDailyBars(DailyBarRequest request, TiingoEodResampleFrequency frequency) {
    return getDailyBarsInternal(request, Objects.requireNonNull(frequency, "frequency"));
  }

  private List<DailyBar> getDailyBarsInternal(DailyBarRequest request, TiingoEodResampleFrequency frequency) {
    Objects.requireNonNull(request, "request");
    URI uri = dailyPricesUri(request, frequency);
    return TiingoCsvParser.parse(request.symbol(), getCsv(uri));
  }

  @Override
  public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
    Objects.requireNonNull(request, "request");
    URI uri = intradayPricesUri(request);
    return TiingoIntradayCsvParser.parse(request.symbol(), getCsv(uri));
  }

  private String getCsv(URI uri) {
    TiingoHttpTransport.Response response;
    try {
      response = transport.get(uri, Map.of("Accept", "text/csv", "Authorization", "Token " + apiKey));
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Tiingo request was interrupted", exception);
    } catch (IOException exception) {
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Unable to reach Tiingo", exception);
    }

    ensureSuccess(response);
    return response.body();
  }

  private URI dailyPricesUri(DailyBarRequest request, TiingoEodResampleFrequency frequency) {
    String query = "startDate=" + request.startDate() + "&endDate=" + request.endDate() + "&format=csv";
    if (frequency != null) {
      query += "&resampleFreq=" + frequency.apiValue();
    }
    return baseUri.resolve("/tiingo/daily/" + encodedSymbol(request.symbol()) + "/prices?" + query);
  }

  private URI intradayPricesUri(IntradayBarRequest request) {
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
      "&format=csv";
    return baseUri.resolve(intradayFeed.pathPrefix() + encodedSymbol(request.symbol()) + "/prices?" + query);
  }

  private static String encodedSymbol(String symbol) {
    return URLEncoder.encode(symbol, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String resampleFrequency(Duration interval) {
    long minutes = interval.toMinutes();
    if (minutes % 60 == 0) {
      return (minutes / 60) + "hour";
    }
    return minutes + "min";
  }

  private static void ensureSuccess(TiingoHttpTransport.Response response) {
    int status = response.statusCode();
    if (status >= 200 && status < 300) {
      return;
    }

    MarketDataException.Code code = switch (status) {
      case 401, 403 -> MarketDataException.Code.AUTHENTICATION;
      case 404 -> MarketDataException.Code.NOT_FOUND;
      case 429 -> MarketDataException.Code.RATE_LIMITED;
      default -> MarketDataException.Code.PROVIDER_ERROR;
    };
    throw new MarketDataException(code, "Tiingo request failed with HTTP status " + status);
  }

  private static String requireApiKey(String apiKey) {
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(API_KEY_ENVIRONMENT_VARIABLE + " must be configured before using Tiingo");
    }
    return apiKey.strip();
  }
}
