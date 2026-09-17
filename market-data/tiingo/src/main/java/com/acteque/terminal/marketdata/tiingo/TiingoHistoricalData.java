package com.acteque.terminal.marketdata.tiingo;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.CalendarRequest;
import com.acteque.terminal.marketdata.HistoricalData;
import com.acteque.terminal.marketdata.IntradayData;
import com.acteque.terminal.marketdata.IntradayRequest;
import com.acteque.terminal.marketdata.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.tiingo.iex.TiingoIexApi;
import java.util.List;
import java.util.Objects;

final class TiingoHistoricalData implements HistoricalData {

  private final TiingoDailyApi daily;
  private final TiingoIexApi iex;

  TiingoHistoricalData(TiingoDailyApi daily, TiingoIexApi iex) {
    this.daily = Objects.requireNonNull(daily, "daily cannot be null");
    this.iex = Objects.requireNonNull(iex, "iex cannot be null");
  }

  @Override
  public List<CalendarData> getCalendarData(CalendarRequest request) {
    return daily.getCalendarData(request);
  }

  @Override
  public List<IntradayData> getIntradayData(IntradayRequest request) {
    return iex.getPrices(request);
  }
}
