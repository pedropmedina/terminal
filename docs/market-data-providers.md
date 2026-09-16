# Market-data providers

The market-data subsystem uses provider contracts, adapters, and a session service. It has no dependency on JavaFX or `marketlogos`.

## Boundaries

- `MarketDataClient` exposes optional history, instrument discovery, and catalog capabilities. An empty optional means unsupported; a supported capability may return an empty result. Capability implementations are reusable and must support concurrent calls from independent sessions.
- `DefaultMarketDataSession` requires history and owns one chart's selected instrument, accumulated bars, pagination, stale-result protection, and executor. Missing discovery uses the symbol as the display name. Closing a session waits for its submitted work and releases its executor; it does not close the provider.
- `MarketDataProviderFactory` creates fresh clients from an injected configuration lookup. Provider-specific keys and validation stay in the provider package. Factories do not read the environment themselves.
- `MarketDataProviderRegistry` selects explicitly registered factories and owns the clients it creates. Each creation is a separate configured instance. Close sessions before closing the registry, which closes all its clients.
- Provider packages own endpoint APIs, authentication, transport, parsing, normalization, and connection cleanup. Shared application and chart code consume only the shared contracts.

## Application configuration

`App` loads environment configuration and registers built-in factories. `MARKET_DATA_PROVIDER` selects the factory and defaults to `tiingo` when omitted. Tiingo's factory requires `TIINGO_API_KEY`. The current chart requires history and a catalog; startup rejects providers lacking either. Instrument discovery is optional.

Tiingo owns and closes the HTTP client it constructs. Its test constructor borrows an injected transport, which remains owned by the test.

## Adding a provider

1. Implement the relevant capability interfaces under `marketdata/provider/<name>/`. Preserve shared ordering, symbol normalization, decimal precision, and error semantics. Symbols remain provider scoped; no automatic cross-provider instrument mapping or fallback is performed.
2. Implement `MarketDataClient`, returning a present optional for each supported capability and closing owned resources idempotently.
3. Implement `MarketDataProviderFactory`, with a stable lowercase identifier matching its clients and explicit validation of required configuration. Never include credential values in errors or logs.
4. Register the factory in `App`, then select its identifier through `MARKET_DATA_PROVIDER`. No chart or session changes are needed.
5. Extend `HistoricalMarketDataContract` for historical providers using injected fixture transports, and add provider-specific tests for other capabilities, responses, and errors.

Selection uses explicit registration, not runtime plugin installation. Multiple accounts can use separate clients from the same factory. Multiple charts can share a client while keeping independent sessions.

## Verification

```sh
./gradlew test --tests 'com.acteque.terminal.marketdata.*'
./gradlew test
```
