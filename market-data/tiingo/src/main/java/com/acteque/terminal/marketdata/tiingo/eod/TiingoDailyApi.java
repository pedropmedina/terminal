package com.acteque.terminal.marketdata.tiingo.eod;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.CalendarRequest;
import com.acteque.terminal.marketdata.tiingo.TiingoRequestExecutor;
import com.acteque.terminal.marketdata.tiingo.TiingoUris;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Client for Tiingo's {@code /tiingo/daily} API. */
public final class TiingoDailyApi {

  private static final URI DEFAULT_BASE_URI = URI.create("https://api.tiingo.com");

  private final URI baseUri;
  private final TiingoRequestExecutor requests;

  public static TiingoDailyApi usingDefaults(TiingoRequestExecutor requests) {
    return new TiingoDailyApi(DEFAULT_BASE_URI, requests);
  }

  public TiingoDailyApi(URI baseUri, TiingoRequestExecutor requests) {
    this.baseUri = Objects.requireNonNull(baseUri, "baseUri cannot be null");
    this.requests = Objects.requireNonNull(requests, "requests cannot be null");
  }

  public List<CalendarData> getCalendarData(String ticker, LocalDate startDate, LocalDate endDate) {
    return getCalendarData(new CalendarRequest(ticker, startDate, endDate));
  }

  public List<CalendarData> getCalendarData(
    String ticker,
    LocalDate startDate,
    LocalDate endDate,
    TiingoEodResampleFrequency resampleFrequency
  ) {
    return getCalendarDataInternal(
      new CalendarRequest(ticker, startDate, endDate),
      Objects.requireNonNull(resampleFrequency, "resampleFrequency cannot be null")
    );
  }

  public List<CalendarData> getCalendarData(CalendarRequest request) {
    Objects.requireNonNull(request, "request cannot be null");
    return getCalendarDataInternal(
      request,
      switch (request.interval()) {
        case DAILY -> null;
        case WEEKLY -> TiingoEodResampleFrequency.WEEKLY;
        case MONTHLY -> TiingoEodResampleFrequency.MONTHLY;
        case YEARLY -> TiingoEodResampleFrequency.ANNUALLY;
      }
    );
  }

  /** Returns descriptive metadata from {@code /tiingo/daily/{ticker}}. */
  public TiingoTickerMetadata getTicker(String ticker) {
    URI uri = baseUri.resolve("/tiingo/daily/" + TiingoUris.ticker(ticker));
    return TiingoDailyJsonParser.parseMetadata(requests.getJson(uri));
  }

  private List<CalendarData> getCalendarDataInternal(
    CalendarRequest request,
    TiingoEodResampleFrequency resampleFrequency
  ) {
    Objects.requireNonNull(request, "request cannot be null");
    String query = "startDate=" + request.startDate() + "&endDate=" + request.endDate() + "&format=json";
    if (resampleFrequency != null) {
      query += "&resampleFreq=" + resampleFrequency.apiValue();
    }
    URI uri = baseUri.resolve("/tiingo/daily/" + TiingoUris.ticker(request.symbol()) + "/prices?" + query);
    return TiingoDailyJsonParser.parseCalendarData(request.symbol(), requests.getJson(uri));
  }
}
