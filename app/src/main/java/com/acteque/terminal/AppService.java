package com.acteque.terminal;

import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.MarketDataProviderFactory;
import com.acteque.terminal.marketdata.MarketDataProviderRegistry;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketdata.MarketDataSessionDefault;
import com.acteque.terminal.marketdata.tiingo.TiingoProviderFactory;
import com.acteque.terminal.marketlogos.LogoProvider;
import com.acteque.terminal.marketlogos.LogoProviderFactory;
import com.acteque.terminal.marketlogos.LogoProviderRegistry;
import com.acteque.terminal.marketlogos.LogoSession;
import com.acteque.terminal.marketlogos.LogoSessionDefault;
import com.acteque.terminal.marketlogos.elbstream.ElbstreamProviderFactory;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * Owns shared provider resources and creates independently owned chart sessions.
 *
 * <p>Consumers must close their sessions before closing the application services.
 */
public final class AppService implements AutoCloseable {

  private static final String MARKET_DATA_PROVIDER_KEY = "MARKET_DATA_PROVIDER";
  private static final String DEFAULT_MARKET_DATA_PROVIDER = "tiingo";
  private static final String MARKET_LOGO_PROVIDER_KEY = "MARKET_LOGO_PROVIDER";
  private static final String DEFAULT_MARKET_LOGO_PROVIDER = "elbstream";

  private final MarketDataProviderRegistry marketDataProviders;
  private final LogoProviderRegistry logoProviders;
  private final MarketDataClient marketData;
  private final LogoProvider logos;
  private final InstrumentCatalog catalog;
  private boolean closed;

  /**
   * Creates application services with the built-in provider factories.
   *
   * @param configuration the environment-backed configuration lookup
   * @return services owning the configured providers
   */
  public static AppService create(Function<String, String> configuration) {
    return new AppService(configuration, List.of(new TiingoProviderFactory()), List.of(new ElbstreamProviderFactory()));
  }

  /**
   * Creates application services from explicit provider factories.
   *
   * @param configuration the provider configuration lookup
   * @param marketDataFactories the available market-data provider factories
   * @param logoFactories the available logo provider factories
   */
  public AppService(
    Function<String, String> configuration,
    Collection<? extends MarketDataProviderFactory> marketDataFactories,
    Collection<? extends LogoProviderFactory> logoFactories
  ) {
    Function<String, String> requiredConfiguration = Objects.requireNonNull(
      configuration,
      "configuration cannot be null"
    );
    marketDataProviders = new MarketDataProviderRegistry(marketDataFactories);
    logoProviders = new LogoProviderRegistry(logoFactories);

    try {
      marketData = createMarketData(requiredConfiguration);
      catalog = requireCatalog(marketData);
      requireHistoricalData(marketData);
      logos = createLogoProvider(requiredConfiguration);
    } catch (RuntimeException | Error failure) {
      closeAfterInitializationFailure(failure);
      throw failure;
    }
  }

  /**
   * Returns the configured provider's instrument catalog.
   *
   * @return the shared instrument catalog
   */
  public InstrumentCatalog catalog() {
    return catalog;
  }

  /**
   * Creates an independently owned market-data session backed by the shared provider.
   *
   * @param symbol the instrument symbol loaded by the session
   * @return a new caller-owned session
   */
  public synchronized MarketDataSession createMarketDataSession(String symbol) {
    requireOpen();
    return new MarketDataSessionDefault(marketData, symbol);
  }

  /**
   * Creates an independently owned logo session backed by the shared provider.
   *
   * @return a new caller-owned session
   */
  public synchronized LogoSession createLogoSession() {
    requireOpen();
    return new LogoSessionDefault(logos);
  }

  /** Closes both provider registries once, preserving failures from each registry. */
  @Override
  public synchronized void close() {
    if (closed) {
      return;
    }
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
      if (failure == null) {
        failure = exception;
      } else {
        failure.addSuppressed(exception);
      }
    }

    if (failure != null) {
      throw failure;
    }
  }

  /**
   * Creates the selected market-data provider.
   *
   * @param configuration the provider configuration lookup
   * @return the configured market-data provider
   */
  private MarketDataClient createMarketData(Function<String, String> configuration) {
    String provider = selectedProvider(configuration, MARKET_DATA_PROVIDER_KEY, DEFAULT_MARKET_DATA_PROVIDER);
    return marketDataProviders.create(provider, configuration);
  }

  /**
   * Creates the selected logo provider.
   *
   * @param configuration the provider configuration lookup
   * @return the configured logo provider
   */
  private LogoProvider createLogoProvider(Function<String, String> configuration) {
    String provider = selectedProvider(configuration, MARKET_LOGO_PROVIDER_KEY, DEFAULT_MARKET_LOGO_PROVIDER);
    return logoProviders.create(provider, configuration);
  }

  /**
   * Returns the catalog required by the application.
   *
   * @param client the selected market-data client
   * @return the provider's instrument catalog
   * @throws IllegalStateException when the provider does not expose a catalog
   */
  private static InstrumentCatalog requireCatalog(MarketDataClient client) {
    return client
      .catalog()
      .orElseThrow(() ->
        new IllegalStateException(
          "Provider " + client.provider() + " does not support the instrument catalog required by this application"
        )
      );
  }

  /**
   * Verifies that the selected market-data provider supports historical bars.
   *
   * @param client the selected market-data client
   * @throws IllegalStateException when the provider does not expose historical data
   */
  private static void requireHistoricalData(MarketDataClient client) {
    if (client.historical().isEmpty()) {
      throw new IllegalStateException("Provider " + client.provider() + " does not support historical bars");
    }
  }

  /**
   * Resolves a provider identifier from configuration, falling back only when no value exists.
   *
   * @param configuration the provider configuration lookup
   * @param key the configuration key
   * @param fallback the identifier used when the key is absent
   * @return the stripped configured identifier or the fallback
   */
  private static String selectedProvider(Function<String, String> configuration, String key, String fallback) {
    String value = configuration.apply(key);
    return value == null ? fallback : value.strip();
  }

  /** Ensures that sessions cannot be created after shared providers have closed. */
  private void requireOpen() {
    if (closed) {
      throw new IllegalStateException("Application services are closed");
    }
  }

  /**
   * Releases providers acquired during construction and suppresses any cleanup failure.
   *
   * @param initializationFailure the failure that interrupted service initialization
   */
  private void closeAfterInitializationFailure(Throwable initializationFailure) {
    try {
      close();
    } catch (RuntimeException closeFailure) {
      initializationFailure.addSuppressed(closeFailure);
    }
  }
}
