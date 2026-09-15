package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.InstrumentDetails;
import com.acteque.terminal.marketdata.provider.tiingo.tickercatalog.TiingoTickerCatalogApi;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Adapts Tiingo's supported-ticker catalog to the shared market-data contract. */
final class TiingoInstrumentCatalog implements InstrumentCatalog {

  private final TiingoTickerCatalogApi tickerCatalog;

  TiingoInstrumentCatalog(TiingoTickerCatalogApi tickerCatalog) {
    this.tickerCatalog = Objects.requireNonNull(tickerCatalog, "tickerCatalog");
  }

  @Override
  public List<InstrumentDetails> getSupportedInstruments() {
    return tickerCatalog
      .getSupportedTickers()
      .stream()
      .map(ticker ->
        new InstrumentDetails(ticker.ticker(), Optional.empty(), Optional.of(ticker.exchange()), Optional.empty())
      )
      .toList();
  }
}
