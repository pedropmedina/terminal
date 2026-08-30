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
- Provider-neutral market-data contracts: `marketdata/`
- Provider integrations: `marketdata/provider/`
- Truly reusable JavaFX components and behaviors: `ui/`
- Development-only hot-reload code: `app/src/hotreload/`
- Tests: `app/src/test/java/`
- Shared Gradle conventions: `build-logic/`

# Implementation Rules

- Keep provider-specific URLs, authentication, transport, parsing, and response handling inside the corresponding provider package.
- Keep shared market-data types independent of any provider.
- Do not make one provider privileged in shared interfaces, domain models, or UI behavior.
- Normalize provider results through `MarketDataClient` before passing them to application or chart code.
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
