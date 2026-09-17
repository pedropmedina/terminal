package com.acteque.terminal;

import com.acteque.terminal.marketdata.*;
import com.acteque.terminal.marketdata.tiingo.TiingoProviderFactory;
import com.acteque.terminal.marketlogos.*;
import com.acteque.terminal.marketlogos.elbstream.ElbstreamProviderFactory;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/** Plain Java composition root. Consumers close their sessions before closing these services. */
public final class AppService implements AutoCloseable {

  private final MarketDataProviderRegistry marketDataProviders;
  private final LogoProviderRegistry logoProviders;
  private final MarketDataClient marketData;
  private final LogoProvider logos;
  private final InstrumentCatalog catalog;
  private boolean closed;

  public static AppService create(Function<String, String> configuration) {
    return new AppService(configuration, List.of(new TiingoProviderFactory()), List.of(new ElbstreamProviderFactory()));
  }

  public AppService(
    Function<String, String> configuration,
    Collection<? extends MarketDataProviderFactory> marketDataFactories,
    Collection<? extends LogoProviderFactory> logoFactories
  ) {
    marketDataProviders = new MarketDataProviderRegistry(marketDataFactories);
    logoProviders = new LogoProviderRegistry(logoFactories);
    try {
      marketData = marketDataProviders.create(selected(configuration, "MARKET_DATA_PROVIDER", "tiingo"), configuration);
      catalog = marketData
        .catalog()
        .orElseThrow(() ->
          new IllegalStateException(
            "Provider " +
              marketData.provider() +
              " does not support the instrument catalog required by this application"
          )
        );
      if (marketData.historical().isEmpty()) {
        throw new IllegalStateException("Provider " + marketData.provider() + " does not support historical bars");
      }
      logos = logoProviders.create(selected(configuration, "MARKET_LOGO_PROVIDER", "elbstream"), configuration);
    } catch (RuntimeException | Error failure) {
      try {
        close();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private static String selected(Function<String, String> configuration, String key, String fallback) {
    String value = configuration.apply(key);
    return value == null ? fallback : value.strip();
  }

  public InstrumentCatalog catalog() {
    return catalog;
  }

  /** The caller owns the returned session. Providers remain shared. */
  public synchronized MarketDataSession createMarketDataSession(String symbol) {
    requireOpen();
    return new MarketDataSessionDefault(marketData, symbol);
  }

  /** The caller owns the returned session. Providers remain shared. */
  public synchronized LogoSession createLogoSession() {
    requireOpen();
    return new LogoSessionDefault(logos);
  }

  private void requireOpen() {
    if (closed) throw new IllegalStateException("Application services are closed");
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    RuntimeException failure = null;
    try {
      logoProviders.close();
    } catch (RuntimeException exception) {
      failure = exception;
    }
    try {
      marketDataProviders.close();
    } catch (RuntimeException exception) {
      if (failure == null) failure = exception;
      else failure.addSuppressed(exception);
    }
    if (failure != null) throw failure;
  }
}
