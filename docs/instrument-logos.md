# Instrument logos

The chart loads company/instrument logos from [Elbstream](https://elbstream.com/logos) after displaying the selected instrument's prices. No additional API key or dependency is required.

## Behavior

- An initial-letter fallback occupies the logo slot while loading or when a logo cannot be resolved or loaded. If metadata is unavailable, lookup uses the selected symbol.
- The status line displays the logo beside the instrument name. Symbol selection and OHLCV interactions are unchanged.
- The symbol-selection tooltip includes the non-interactive **Logos by Elbstream** attribution at 12pt whenever the logo is displayed.
- Only selected instruments are requested, not the ticker catalog. Loading prices and earlier history never waits for a logo download.
- New selections and application shutdown invalidate pending logo updates, including updates already queued on the JavaFX thread.
- There is no disk cache, self-hosting, or reusable image cache. The current decoded image is retained only for display and view refreshes.

## Integration

Market logo support lives in `com.acteque.terminal.marketlogos`, independently of `marketdata`. Neither subsystem imports the other. The package currently supports instrument logos; the broader name leaves room for exchange and broker logos. `InstrumentLogos` is the provider contract; `LogoRequest` carries symbol and exchange hints without depending on market-data models. `InstrumentLogo` carries the image reference and attribution, and `LogoException` normalizes provider failures.

`App` injects a market-data session and a separate `LogoSession` into the chart. The chart maps loaded metadata into a `LogoRequest`; logos are no longer part of `Instrument` or `MarketDataClient`. A chart without logo support uses `LogoSession.NONE`.

`ElbstreamInstrumentLogos` owns URL construction, transport, response validation, and error normalization. It requests:

```text
https://api.elbstream.com/logos/symbol/{encoded-symbol}?format=png&size=64
```

This is best-effort ticker matching. Symbols remain provider-scoped in our metadata, and Elbstream's symbol lookup does not disambiguate exchanges. The integration preserves ticker punctuation and does not guess exchange suffixes or company domains. ISIN-based matching would be preferable if a future metadata source supplies a stable ISIN.

`LogoSessionDefault` wraps any `InstrumentLogos` implementation and owns its executor and cancellable download task. Each chart owns its own session and closes it on shutdown. Providers may be shared between sessions; closing one session does not close or cancel another. The status-line interactor owns decoding, stale-result suppression, and JavaFX-thread model updates. The chart owns image presentation and does not perform HTTP requests.

To add a provider, implement `LogoProvider` and `LogoProviderFactory` in a `market-logos/<name>/` module, keeping URLs, transport, parsing, and provider-specific mapping there. Reference resolution must be local and fast; `load` performs blocking I/O on the session's executor. Implementations must support concurrent calls from independent sessions. Register its factory in `AppService`, which creates shared providers and independent sessions. No chart or market-data changes are required. Provider selection is explicit dependency injection; dynamic plugin discovery is not implemented.

Requests have connection and whole-body timeouts, a 1 MiB streamed payload limit, PNG validation, a fixed endpoint allowlist, and no redirects or automatic retries. A 404 means no logo; other failures use `LogoException` codes and leave the fallback in place. No Tiingo credentials are sent to Elbstream.

## External service and terms

Selecting an instrument sends its metadata symbol to Elbstream; Elbstream also receives the normal connection information, such as the user's IP address. No price history or account credentials are sent with logo requests.

Elbstream advertises its logo endpoint as free without authentication, subject to fair use and visible attribution. Its pricing reserves custom caching and self-hosting for Enterprise. Logos remain the property of their respective owners and are displayed only to identify instruments.

Before distributing the application, confirm that the desktop attribution treatment and intended usage satisfy the current terms. The website advertises a free Hobby tier while its general terms say business customers only; this implementation does not resolve that inconsistency or confer redistribution rights.

References checked during implementation:

- [Logo API and pricing](https://elbstream.com/logos)
- [OpenAPI specification](https://api.elbstream.com/openapi.json)
- [Terms](https://elbstream.com/terms)

## Validation

Provider tests use injected transports and local HTTP fixtures, not live provider endpoints. Tests cover provider substitution, session isolation, metadata preservation, error normalization, bounded downloads, async loading, stale completions, decoding failures, fallback display, tooltip attribution, theme sizing, and view refreshes.

```sh
./gradlew :market-logos:core:test :market-logos:elbstream:test :market-data:core:test
./gradlew :app:test --tests 'com.acteque.terminal.chart.statusline.ChartStatusLineLogoInteractorTest' --tests 'com.acteque.terminal.chart.statusline.ChartStatusLineTest'
./gradlew test
```

## Library modules and provider selection

`market-logos/core` contains contracts, domain objects, asynchronous sessions, and the logo factory registry. `market-logos/elbstream` contains the Elbstream adapter and factory. Neither library depends on JavaFX or market data. The application decodes image bytes and updates the UI.

`AppService` registers `ElbstreamProviderFactory`. `MARKET_LOGO_PROVIDER` selects the provider and defaults to `elbstream`. To add a provider, create a module depending on logo core, implement `LogoProvider` and `LogoProviderFactory`, and register the factory in `AppService`. Include the module in settings, app dependencies, and hot-reload library paths.

The registry owns providers; each chart owns its sessions. Close sessions before application services. Elbstream closes its owned HTTP client; injected HTTP clients remain owned by the caller.

Run library tests with `./gradlew :market-logos:core:test :market-logos:elbstream:test`. Cross-subsystem isolation coverage stays in the app tests.
