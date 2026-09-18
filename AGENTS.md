# Commands

- Build and verify: `./gradlew build`
- Run all tests: `./gradlew test`
- Run one test class:
  `./gradlew test --tests 'com.acteque.terminal.chart.XAxisTickCalculatorTest'`
- Run the application: configure `.env` from `.env.example`, then run `./gradlew run`
- Run with JavaFX hot reload: `./scripts/dev`
- Format files: `npm run format`

# Project Structure

- Application code: `app/src/main/java/com/acteque/terminal/`
- JavaFX charting and interaction: `chart/`
- Provider-neutral market-data contracts, factories, registry, and sessions: `market-data/core/`
- Market-data provider integrations: `market-data/tiingo/` (one module per provider)
- Independent market logo contracts and sessions: `market-logos/core/`
- Market logo provider integrations: `market-logos/elbstream/` (one module per provider)
- Reusable JavaFX controls and styles: `ui/core/`; independent Lucide icon components and assets: `ui/icons/`
- Development-only hot-reload code and Gradle tasks: `hot-reload/`
- Tests: each module’s `src/test/java/`; shared provider contracts in `market-data/core/src/test-fixtures/java/`
- Shared Gradle conventions: `build-logic/`

# Implementation Rules

- Use lowercase kebab-case for project and non-package directory names (for example, `market-data`, `market-logos`, and `test-fixtures`). Java package directories must follow valid lowercase Java package names without hyphens. Keep Gradle source-set identifiers such as `testFixtures` and explicitly configure their kebab-case source directories.
- Name classes and other types with the primary domain concept first, followed by a suffix describing their purpose, role, or specialization. Choose the shared prefix based on semantics so related types appear together in alphabetical file listings and autocomplete. For example, use `Instrument.java` and `InstrumentLoadResult.java` rather than `LoadedInstrument.java`.
- Group related variable declarations and initialization together when it improves readability, using blank lines to separate distinct logical groups. Preserve required execution ordering, especially validation before state changes, and avoid widening variable scope unnecessarily.
- Keep provider-specific URLs, authentication, transport, parsing, and response handling inside the corresponding provider package.
- Keep shared market-data types independent of any provider.
- Keep market-data and market-logo library modules independent of JavaFX and `app`; provider modules depend on their corresponding core only. Keep `ui/core` and `ui/icons` independent of `app`.
- Keep concrete provider registration and shared resource ownership in `AppService`; `App` loads environment configuration and manages JavaFX.
- Construct market-data providers through registered factories. Keep environment access at the application boundary; factories validate provider-specific configuration.
- Each chart owns a `MarketDataSessionDefault`; the application registry owns shared provider clients and closes them after sessions.
- Do not make one provider privileged in shared interfaces, domain models, or UI behavior.
- Normalize market-data provider results through `MarketDataClient` before passing them to application or chart code.
- Keep instrument logos independent of market data. Plug logo providers in through `InstrumentLogos`; each chart owns its own `LogoSession`.
- Preserve `BigDecimal` values in market-data models; convert to drawing-friendly primitives only at the chart boundary.
- Perform JavaFX UI updates on the JavaFX application thread. Use `Platform.runLater` when completing background work.
- Keep styling separate from application logic. Define colors, spacing, typography, borders, and other presentation rules in CSS resources; Java code should manage structure, state, and behavior.
- Avoid inline JavaFX styles and presentation constants in Java when they can be expressed in CSS.
- Keep only genuinely reusable, feature-independent JavaFX components and behaviors in `ui/`.
- Keep feature-specific controls and behavior in their feature packages; do not use `ui/` as a general-purpose dumping ground.
- Keep hot-reload classes in the `hotreload` source set; they must not enter the production runtime or distribution.
- Normal builds use the Java 26 toolchain. Hot reload intentionally uses JetBrains Runtime 25; do not unify these toolchains without revisiting the enhanced-class-redefinition setup.
- Do not add dependencies or change Java or JavaFX versions unless the task requires it.

# Testing

- Add or update JUnit tests for behavior changes.
- Test provider integrations using injected transports and representative responses; tests must not call live provider endpoints.
- Verify that provider results satisfy the ordering, normalization, and error semantics defined by the shared market-data contracts.
- Inject `Clock` or executors when behavior depends on time or concurrency.
- For chart calculations, cover empty, boundary, zoomed, and panned states.
- Run the most specific affected test first, followed by `./gradlew test`.
