package com.acteque.terminal.marketdata.tiingo;

import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.DailyBarRequest;
import com.acteque.terminal.marketdata.HistoricalBarData;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.IntradayBarRequest;
import com.acteque.terminal.marketdata.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.tiingo.iex.TiingoIexApi;
import java.util.List;
import java.util.Objects;

final class TiingoHistoricalBarData implements HistoricalBarData {

  private final TiingoDailyApi daily;
  private final TiingoIexApi iex;

  TiingoHistoricalBarData(TiingoDailyApi daily, TiingoIexApi iex) {
    this.daily = Objects.requireNonNull(daily, "daily");
    this.iex = Objects.requireNonNull(iex, "iex");
  }

  @Override
  public List<DailyBar> getDailyBars(DailyBarRequest request) {
    return daily.getBars(request);
  }

  @Override
  public List<IntradayBar> getIntradayBars(IntradayBarRequest request) {
    return iex.getPrices(request);
  }
}
