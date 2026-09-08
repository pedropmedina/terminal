package com.acteque.terminal.marketdata;

import java.util.Objects;

/** Adds a provider-independent logo feature without changing the market-data provider or history. */
public final class LogoMarketDataClient implements MarketDataClient {

  private final MarketDataClient delegate;
  private final InstrumentLogos logos;
  private final InstrumentDiscovery discovery;

  public LogoMarketDataClient(MarketDataClient delegate, InstrumentLogos logos) {
    this.delegate = Objects.requireNonNull(delegate, "delegate");
    this.logos = Objects.requireNonNull(logos, "logos");
    this.discovery = symbol -> {
      InstrumentDetails details = this.delegate.discovery().getInstrument(symbol);
      return details.withLogo(this.logos.findLogo(details));
    };
  }

  @Override
  public String provider() {
    return delegate.provider();
  }

  @Override
  public HistoricalBarData historicalBars() {
    return delegate.historicalBars();
  }

  @Override
  public InstrumentDiscovery discovery() {
    return discovery;
  }

  @Override
  public InstrumentLogos instrumentLogos() {
    return logos;
  }
}
