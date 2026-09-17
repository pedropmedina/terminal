package com.acteque.terminal.marketdata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Explicit factory registration and ownership of configured provider instances. */
public final class MarketDataProviderRegistry implements AutoCloseable {

  private final Map<String, MarketDataProviderFactory> factories = new LinkedHashMap<>();
  private final List<MarketDataClient> clients = new ArrayList<>();
  private boolean closed;

  public MarketDataProviderRegistry(Collection<? extends MarketDataProviderFactory> factories) {
    for (MarketDataProviderFactory factory : factories) {
      String id = Objects.requireNonNull(factory.provider(), "provider cannot be null");
      if (!id.matches("[a-z][a-z0-9-]*")) {
        throw new IllegalArgumentException("Provider identifiers must use lowercase letters, digits, or hyphens");
      }
      if (this.factories.putIfAbsent(id, factory) != null) {
        throw new IllegalArgumentException("Duplicate market-data provider: " + id);
      }
    }
  }

  /** Each call creates a separate configured instance, even for the same provider identifier. */
  public synchronized MarketDataClient create(String provider, Function<String, String> configuration) {
    if (closed) {
      throw new IllegalStateException("Market-data provider registry is closed");
    }
    Objects.requireNonNull(configuration, "configuration cannot be null");
    MarketDataProviderFactory factory = factories.get(Objects.requireNonNull(provider, "provider cannot be null"));
    if (factory == null) {
      throw new IllegalArgumentException(
        "Unknown market-data provider: " + provider + "; registered: " + factories.keySet()
      );
    }
    MarketDataClient client = Objects.requireNonNull(factory.create(configuration), "factory returned no client");
    if (!provider.equals(client.provider())) {
      IllegalStateException failure = new IllegalStateException(
        "Factory returned a client with a different provider identifier"
      );
      try {
        client.close();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
    clients.add(client);
    return client;
  }

  /** Close consumer sessions first. All clients are closed even if one close fails. */
  @Override
  public void close() {
    List<MarketDataClient> owned;
    synchronized (this) {
      if (closed) return;
      closed = true;
      owned = List.copyOf(clients);
      clients.clear();
    }
    RuntimeException failure = null;
    for (MarketDataClient client : owned.reversed()) {
      try {
        client.close();
      } catch (RuntimeException exception) {
        if (failure == null) {
          failure = new IllegalStateException("Could not close all market-data providers");
        }
        failure.addSuppressed(exception);
      }
    }
    if (failure != null) throw failure;
  }
}
