# Instrument logos

The chart loads company/instrument logos from [Elbstream](https://elbstream.com/logos) after displaying the selected instrument's prices. No additional API key or dependency is required.

## Behavior

- An initial-letter fallback occupies the logo slot while loading, when metadata is unavailable, or when a logo is missing or cannot be loaded.
- The status line displays the logo beside the instrument name. Symbol selection and OHLCV interactions are unchanged.
- The symbol-selection tooltip includes the non-interactive **Logos by Elbstream** attribution at 12pt whenever the logo is displayed.
- Only selected instruments are requested, not the ticker catalog. Loading prices and earlier history never waits for a logo download.
- New selections and application shutdown invalidate pending logo updates, including updates already queued on the JavaFX thread.
- There is no disk cache, self-hosting, or reusable image cache. The current decoded image is retained only for display and view refreshes.

## Integration

`LogoMarketDataClient` composes a market-data client with the provider-neutral `InstrumentLogos` feature. Discovery returns `InstrumentDetails` containing an optional `InstrumentLogo` reference and attribution. Existing clients without branding continue to work through the no-op default feature.

`ElbstreamInstrumentLogos` owns URL construction, transport, response validation, and error normalization. It requests:

```text
https://api.elbstream.com/logos/symbol/{encoded-symbol}?format=png&size=64
```

This is best-effort ticker matching. Symbols remain provider-scoped in our metadata, and Elbstream's symbol lookup does not disambiguate exchanges. The integration preserves ticker punctuation and does not guess exchange suffixes or company domains. ISIN-based matching would be preferable if a future metadata source supplies a stable ISIN.

`MarketDataController.loadLogo` downloads separately from price loading through the chart's `ChartLogoSource` port. The status-line interactor owns decoding, stale-result suppression, cancellation, and JavaFX-thread model updates. The chart owns image presentation and does not perform HTTP requests.

Requests have connection and whole-body timeouts, a 1 MiB streamed payload limit, PNG validation, a fixed endpoint allowlist, and no redirects or automatic retries. A 404 means no logo; other failures use normalized market-data errors and leave the fallback in place. No Tiingo credentials are sent to Elbstream.

## External service and terms

Selecting an instrument sends its metadata symbol to Elbstream; Elbstream also receives the normal connection information, such as the user's IP address. No price history or account credentials are sent with logo requests.

Elbstream advertises its logo endpoint as free without authentication, subject to fair use and visible attribution. Its pricing reserves custom caching and self-hosting for Enterprise. Logos remain the property of their respective owners and are displayed only to identify instruments.

Before distributing the application, confirm that the desktop attribution treatment and intended usage satisfy the current terms. The website advertises a free Hobby tier while its general terms say business customers only; this implementation does not resolve that inconsistency or confer redistribution rights.

References checked during implementation:

- [Logo API and pricing](https://elbstream.com/logos)
- [OpenAPI specification](https://api.elbstream.com/openapi.json)
- [Terms](https://elbstream.com/terms)

## Validation

Provider tests use injected transports and local HTTP fixtures, not live provider endpoints. Tests cover metadata enrichment, error normalization, bounded downloads, async loading, stale completions, decoding failures, fallback display, tooltip attribution, theme sizing, and view refreshes.

```sh
./gradlew test --tests 'com.acteque.terminal.marketdata.*'
./gradlew test --tests 'com.acteque.terminal.chart.statusline.ChartStatusLineLogoInteractorTest' --tests 'com.acteque.terminal.chart.statusline.ChartStatusLineTest'
./gradlew test
```
