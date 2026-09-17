package com.acteque.terminal.marketdata.tiingo;

import com.acteque.terminal.marketdata.Instrument;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.tiingo.eod.TiingoTickerMetadata;
import com.acteque.terminal.marketdata.tiingo.tickercatalog.TiingoTickerCatalogApi;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapts Tiingo's supported tickers and metadata to the shared instrument catalog. */
final class TiingoInstrumentCatalog implements InstrumentCatalog {

  private final TiingoTickerCatalogApi tickerCatalog;

  private final TiingoDailyApi daily;

  TiingoInstrumentCatalog(TiingoTickerCatalogApi tickerCatalog, TiingoDailyApi daily) {
    this.daily = Objects.requireNonNull(daily, "daily cannot be null");
    this.tickerCatalog = Objects.requireNonNull(tickerCatalog, "tickerCatalog cannot be null");
  }

  @Override
  public Instrument getInstrument(String symbol) {
    TiingoTickerMetadata metadata = daily.getTicker(symbol);
    // Tiingo's exchange code is a provider-reported label, not a guaranteed MIC.
    return new Instrument(
      metadata.ticker(),
      Optional.of(metadata.name()),
      Optional.of(metadata.exchangeCode()),
      metadata.description()
    );
  }

  @Override
  public List<Instrument> getInstruments() {
    return tickerCatalog
      .getSupportedTickers()
      .stream()
      .map(ticker ->
        new Instrument(ticker.ticker(), Optional.empty(), Optional.of(ticker.exchange()), Optional.empty())
      )
      .toList();
  }
}
