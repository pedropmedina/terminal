# Market-data providers

The market-data subsystem uses provider contracts, adapters, and a session service. It has no dependency on JavaFX or `marketlogos`.

## Boundaries

- `MarketDataClient` exposes optional history and instrument catalog capabilities. The catalog provides both instrument listing and metadata lookup. An empty optional means unsupported; a supported capability may return an empty result. Capability implementations are reusable and must support concurrent calls from independent sessions.
- `MarketDataSessionDefault` requires history and owns one chart's selected instrument, accumulated bars, pagination, stale-result protection, and executor. Missing catalog uses the symbol as the display name. Closing a session waits for its submitted work and releases its executor; it does not close the provider.
- `MarketDataProviderFactory` creates fresh clients from an injected configuration lookup. Provider-specific keys and validation stay in the provider package. Factories do not read the environment themselves.
- `MarketDataProviderRegistry` selects explicitly registered factories and owns the clients it creates. Each creation is a separate configured instance. Close sessions before closing the registry, which closes all its clients.
- Provider packages own endpoint APIs, authentication, transport, parsing, normalization, and connection cleanup. Shared application and chart code consume only the shared contracts.

## Historical data

`HistoricalData.getCalendarData(CalendarRequest)` returns `CalendarData` for a `CalendarInterval`: `DAILY`, `WEEKLY`, `MONTHLY`, or `YEARLY`. The three-argument request constructor defaults to daily. These are calendar periods, not fixed durations. `HistoricalData.getIntradayData(IntradayRequest)` returns `IntradayData` for a duration shorter than a day, with the existing after-hours and fill options.

Calendar results retain the provider's date labels and optional adjusted prices and corporate-action values. A period label need not be a trading date; callers should not infer period completeness or corporate-action aggregation from the label alone. Tiingo maps calendar intervals to its EOD endpoint and `YEARLY` to `annually`; intraday requests use IEX. The chart session currently requests daily data and keeps a daily history cache.

## Application configuration

`App` loads environment configuration. Plain Java `ApplicationServices` registers built-in factories and owns shared providers. `MARKET_DATA_PROVIDER` selects the factory and defaults to `tiingo` when omitted. Tiingo's factory requires `TIINGO_API_KEY`. The current chart requires history and a catalog; startup rejects providers lacking either.

Tiingo owns and closes the HTTP client it constructs. Its test constructor borrows an injected transport, which remains owned by the test.

## Adding a provider

1. Implement the relevant capability interfaces in a new `market-data/<name>/` library module, depending on `market-data/core/`. Preserve shared ordering, symbol normalization, decimal precision, and error semantics. Symbols remain provider scoped; no automatic cross-provider instrument mapping or fallback is performed.
2. Implement `MarketDataClient`, returning a present optional for each supported capability and closing owned resources idempotently.
3. Implement `MarketDataProviderFactory`, with a stable lowercase identifier matching its clients and explicit validation of required configuration. Never include credential values in errors or logs.
4. Add the module to settings and the app dependencies. Register the factory in `ApplicationServices`, then select its identifier through `MARKET_DATA_PROVIDER`. No chart or session changes are needed.
5. Extend `HistoricalMarketDataContract` for historical providers using injected fixture transports, and add provider-specific tests for other capabilities, responses, and errors.

Selection uses explicit registration, not runtime plugin installation. Multiple accounts can use separate clients from the same factory. Multiple charts can share a client while keeping independent sessions.

## Verification

```sh
./gradlew :market-data:core:test :market-data:tiingo:test
./gradlew test
```

## Module boundaries

`market-data/core` contains provider-neutral models, capabilities, sessions, factories, and the registry. `market-data/tiingo` depends on core and owns its Jackson dependency. Neither module depends on JavaFX or the application. The reusable `HistoricalMarketDataContract` lives in core's test fixtures and is consumed by provider tests.

Modules build together using project dependencies. Publishing is unnecessary; the application distribution includes the provider and core JARs with distinct archive names.

For a new library module, also add its source/resource paths to `libraryPaths` in `app/build.gradle.kts`. Hot reload compiles all application and library sources with Java 25 and excludes the normal Java 26 project JARs from its runtime. Normal builds retain Java 26 throughout.
