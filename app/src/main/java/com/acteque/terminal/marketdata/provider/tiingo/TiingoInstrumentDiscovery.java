package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.InstrumentDetails;
import com.acteque.terminal.marketdata.InstrumentDiscovery;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoDailyApi;
import com.acteque.terminal.marketdata.provider.tiingo.eod.TiingoTickerMetadata;
import java.util.Objects;
import java.util.Optional;

final class TiingoInstrumentDiscovery implements InstrumentDiscovery {

  private final TiingoDailyApi daily;

  TiingoInstrumentDiscovery(TiingoDailyApi daily) {
    this.daily = Objects.requireNonNull(daily, "daily");
  }

  @Override
  public InstrumentDetails getInstrument(String symbol) {
    TiingoTickerMetadata metadata = daily.getTicker(symbol);
    // Tiingo's exchange code is a provider-reported label, not a guaranteed MIC.
    return new InstrumentDetails(
      metadata.ticker(),
      Optional.of(metadata.name()),
      Optional.of(metadata.exchangeCode()),
      metadata.description()
    );
  }
}
