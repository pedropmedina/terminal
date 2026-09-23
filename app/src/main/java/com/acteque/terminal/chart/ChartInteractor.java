package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.HistoricalInterval;
import com.acteque.terminal.marketdata.HistoricalPage;
import com.acteque.terminal.marketdata.InstrumentHistoryLoadResult;
import com.acteque.terminal.marketdata.InstrumentLoadResult;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoSession;
import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import javafx.scene.image.Image;

/** Coordinates chart state and market-data workflows without depending on JavaFX controls or layout. */
final class ChartInteractor implements AutoCloseable {

  private static final System.Logger LOGGER = System.getLogger(ChartInteractor.class.getName());
  private final ChartModel model;
  private final MarketDataSession marketDataSession;
  private final LogoSession logoSession;
  private final Executor uiExecutor;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Runnable instrumentLoadStartedHandler = () -> {};
  private Consumer<InstrumentLoadResult> instrumentLoadedHandler = ignored -> {};
  private Consumer<List<CalendarData>> earlierHistoryLoadedHandler = ignored -> {};
  private BiConsumer<String, Throwable> instrumentLoadFailedHandler = (symbol, failure) -> {};
  private Consumer<Throwable> earlierHistoryLoadFailedHandler = ignored -> {};
  private Consumer<InstrumentHistoryLoadResult> historyLoadedHandler = ignored -> {};
  private Consumer<HistoricalPage> earlierSelectedHistoryLoadedHandler = ignored -> {};
  private long instrumentLoadGeneration;
  private String requestedSymbol;
  private ChartInterval requestedInterval;
  private boolean historyLoadInProgress;
  private boolean closed;
  private long logoGeneration;

  /** @param model the chart state mutated by this interactor */
  ChartInteractor(ChartModel model) {
    this(model, null, LogoSession.NONE, Runnable::run);
  }

  /**
   * @param model the chart state mutated by this interactor
   * @param marketDataSession the owned market-data session
   * @param uiExecutor the executor used to publish asynchronous results
   */
  ChartInteractor(ChartModel model, MarketDataSession marketDataSession, Executor uiExecutor) {
    this(model, marketDataSession, LogoSession.NONE, uiExecutor);
  }

  /**
   * @param model the chart state mutated by this interactor
   * @param logoSession the chart's logo session
   * @param uiExecutor the executor used to publish asynchronous results
   */
  ChartInteractor(ChartModel model, LogoSession logoSession, Executor uiExecutor) {
    this(model, null, logoSession, uiExecutor);
  }

  /**
   * @param model the chart state mutated by this interactor
   * @param marketDataSession the optional owned market-data session
   * @param logoSession the chart's logo session
   * @param uiExecutor the executor used to publish asynchronous results
   */
  ChartInteractor(ChartModel model, MarketDataSession marketDataSession, LogoSession logoSession, Executor uiExecutor) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.marketDataSession = marketDataSession;
    this.logoSession = Objects.requireNonNull(logoSession, "logoSession cannot be null");
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
  }

  /** @param interval the initial selected interval */
  void initialize(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
    requestedInterval = interval;
    requestedSymbol = model.getSymbol();
  }

  /** Opens instrument search unless another chart modal is open. */
  void openInstrumentSearch() {
    if (!model.isModalOpen()) {
      model.setInstrumentSearchOpen(true);
    }
  }

  /** Closes instrument search. */
  void closeInstrumentSearch() {
    model.setInstrumentSearchOpen(false);
  }

  /** Opens interval selection unless another chart modal is open. */
  void openIntervalSelection() {
    if (!model.isModalOpen()) {
      model.setIntervalSelectionOpen(true);
    }
  }

  /** Closes interval selection. */
  void closeIntervalSelection() {
    model.setIntervalSelectionOpen(false);
  }

  /** @param callback the callback notified after an interval transition */
  void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param interval the interval to publish and select */
  void selectInterval(ChartInterval interval) {
    ChartInterval selectedInterval = Objects.requireNonNull(interval, "interval cannot be null");
    model.setInterval(selectedInterval);
    closeIntervalSelection();
    intervalSelectedHandler.accept(selectedInterval);
  }

  /** @param callback the callback invoked before an instrument request starts */
  void onInstrumentLoadStarted(Runnable callback) {
    instrumentLoadStartedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving legacy instrument results */
  void onInstrumentLoaded(Consumer<InstrumentLoadResult> callback) {
    instrumentLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving earlier calendar history */
  void onEarlierHistoryLoaded(Consumer<List<CalendarData>> callback) {
    earlierHistoryLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving a failed symbol and its cause */
  void onInstrumentLoadFailed(BiConsumer<String, Throwable> callback) {
    instrumentLoadFailedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving earlier-history failures */
  void onEarlierHistoryLoadFailed(Consumer<Throwable> callback) {
    earlierHistoryLoadFailedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving interval-aware instrument results */
  void onHistoryLoaded(Consumer<InstrumentHistoryLoadResult> callback) {
    historyLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** @param callback the callback receiving earlier interval-aware history */
  void onEarlierSelectedHistoryLoaded(Consumer<HistoricalPage> callback) {
    earlierSelectedHistoryLoadedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Clears the current logo and loads the supplied logo metadata when present.
   *
   * @param logo the optional {@link InstrumentLogo} to load
   */
  void setInstrumentLogo(Optional<InstrumentLogo> logo) {
    cancelInstrumentLogoLoad();
    model.setInstrumentLogoImage(null);
    long requestedGeneration = logoGeneration;
    Objects.requireNonNull(logo, "logo cannot be null").ifPresent(reference ->
      startInstrumentLogoLoad(reference, requestedGeneration)
    );
  }

  /** Invalidates pending logo updates and cancels the current session request. */
  void cancelInstrumentLogoLoad() {
    ++logoGeneration;
    logoSession.cancel();
  }

  /**
   * Reports whether the configured provider supports a chart interval.
   *
   * @param interval the interval to test
   * @return true when it maps to a provider-supported history interval
   */
  boolean supports(ChartInterval interval) {
    return ChartIntervalHistoryMapper.map(interval)
      .filter(candidate -> marketDataSession != null && marketDataSession.supports(candidate))
      .isPresent();
  }

  /** @param symbol the initial symbol whose interval-aware history is requested */
  void loadInitialHistory(String symbol) {
    requestedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    HistoricalInterval interval = selectedHistoryInterval();
    displayHistoryLoad(symbol, requestedInterval, requireMarketData().loadInitial(interval));
  }

  /** @param symbol the selected symbol whose interval-aware history is requested */
  void selectInstrumentHistory(String symbol) {
    requestedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    HistoricalInterval interval = selectedHistoryInterval();
    displayHistoryLoad(symbol, requestedInterval, requireMarketData().loadInstrumentHistory(symbol, interval));
  }

  /** @param interval the interval to select or load from the configured provider */
  void requestInterval(ChartInterval interval) {
    ChartInterval requested = Objects.requireNonNull(interval, "interval cannot be null");
    if (marketDataSession == null) {
      selectInterval(requested);
      return;
    }
    HistoricalInterval historical = ChartIntervalHistoryMapper.map(requested)
      .filter(marketDataSession::supports)
      .orElseThrow(() -> new IllegalArgumentException("Unsupported chart interval: " + requested));
    closeIntervalSelection();
    if (requested.equals(model.getInterval()) && !historyLoadInProgress) {
      return;
    }
    requestedInterval = requested;
    String symbol = Objects.requireNonNull(requestedSymbol, "requestedSymbol cannot be null");
    displayHistoryLoad(symbol, requested, marketDataSession.loadInstrumentHistory(symbol, historical));
  }

  /** Requests an earlier page for the current interval-aware history session. */
  void loadEarlierSelectedHistory() {
    long generation = instrumentLoadGeneration;
    requireMarketData()
      .loadEarlierHistory()
      .whenComplete((page, failure) ->
        uiExecutor.execute(() -> {
          if (closed || generation != instrumentLoadGeneration) {
            return;
          }
          if (failure != null) {
            earlierHistoryLoadFailedHandler.accept(unwrap(failure));
          } else {
            earlierSelectedHistoryLoadedHandler.accept(page);
          }
        })
      );
  }

  /** @return the provider-supported history interval for the requested chart interval */
  private HistoricalInterval selectedHistoryInterval() {
    return ChartIntervalHistoryMapper.map(requestedInterval)
      .filter(candidate -> requireMarketData().supports(candidate))
      .orElseThrow(() -> new IllegalArgumentException("Unsupported chart interval: " + requestedInterval));
  }

  /**
   * Publishes an interval-aware request result if it is still current.
   *
   * @param symbol the requested symbol
   * @param requestedInterval the requested chart interval
   * @param load the asynchronous provider result
   */
  private void displayHistoryLoad(
    String symbol,
    ChartInterval requestedInterval,
    CompletionStage<InstrumentHistoryLoadResult> load
  ) {
    long generation = ++instrumentLoadGeneration;
    historyLoadInProgress = true;
    model.setLoadError(null);
    instrumentLoadStartedHandler.run();
    load.whenComplete((result, failure) ->
      uiExecutor.execute(() -> {
        if (closed || generation != instrumentLoadGeneration) {
          return;
        }
        historyLoadInProgress = false;
        if (failure != null) {
          Throwable cause = unwrap(failure);
          requestedSymbol = model.getSymbol();
          this.requestedInterval = model.getInterval();
          if (!(cause instanceof CancellationException)) {
            model.setLoadError(
              "Unable to load " + symbol + " at " + requestedInterval.name() + ": " + cause.getMessage()
            );
            instrumentLoadFailedHandler.accept(symbol, cause);
          }
          return;
        }
        requestedSymbol = result.symbol();
        this.requestedInterval = requestedInterval;
        model.setInterval(requestedInterval);
        model.setLoadError(null);
        intervalSelectedHandler.accept(requestedInterval);
        historyLoadedHandler.accept(result);
      })
    );
  }

  /** @param symbol the initial symbol loaded through the legacy calendar workflow */
  void loadInitialInstrument(String symbol) {
    displayInstrumentLoad(Objects.requireNonNull(symbol, "symbol cannot be null"), requireMarketData().loadInitial());
  }

  /** @param symbol the selected symbol loaded through the legacy calendar workflow */
  void selectInstrument(String symbol) {
    String selectedSymbol = Objects.requireNonNull(symbol, "symbol cannot be null");
    displayInstrumentLoad(selectedSymbol, requireMarketData().loadInstrument(selectedSymbol));
  }

  /** Requests an earlier page through the legacy calendar workflow. */
  void loadEarlierHistory() {
    long generation = instrumentLoadGeneration;
    requireMarketData()
      .loadEarlier()
      .whenComplete((bars, failure) ->
        uiExecutor.execute(() -> {
          if (closed || generation != instrumentLoadGeneration) {
            return;
          }
          if (failure != null) {
            earlierHistoryLoadFailedHandler.accept(unwrap(failure));
          } else {
            earlierHistoryLoadedHandler.accept(bars);
          }
        })
      );
  }

  /** Cancels pending work and closes the owned market-data session once. */
  @Override
  public void close() {
    if (closed) {
      return;
    }
    closed = true;
    ++instrumentLoadGeneration;
    try {
      cancelInstrumentLogoLoad();
    } finally {
      if (marketDataSession != null) {
        marketDataSession.close();
      }
    }
  }

  /**
   * Starts decoding a resolved instrument logo asynchronously.
   *
   * @param reference the provider logo reference
   * @param requestedGeneration the generation that must remain current
   */
  private void startInstrumentLogoLoad(InstrumentLogo reference, long requestedGeneration) {
    CompletionStage<Optional<byte[]>> request;
    try {
      request = Objects.requireNonNull(logoSession.load(reference), "logo load cannot be null");
    } catch (RuntimeException failure) {
      reportLogoFailure(failure);
      return;
    }
    request
      .thenApply(bytes -> bytes.map(ChartInteractor::decodeLogo))
      .whenComplete((image, failure) -> {
        try {
          uiExecutor.execute(() -> completeInstrumentLogoLoad(requestedGeneration, image, failure));
        } catch (RuntimeException schedulingFailure) {
          reportLogoFailure(schedulingFailure);
        }
      });
  }

  /**
   * Publishes a decoded logo only when its request is current and successful.
   *
   * @param requestedGeneration the completed request generation
   * @param image the optionally decoded image
   * @param failure the asynchronous failure, or {@code null}
   */
  private void completeInstrumentLogoLoad(long requestedGeneration, Optional<Image> image, Throwable failure) {
    if (closed || requestedGeneration != logoGeneration) {
      return;
    }
    if (failure != null) {
      reportLogoFailure(failure);
      return;
    }
    image.filter(ChartInteractor::isValidLogo).ifPresent(model::setInstrumentLogoImage);
  }

  /** @param failure the logo failure to classify and log */
  private static void reportLogoFailure(Throwable failure) {
    for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
      if (cause instanceof LogoException logoFailure && logoFailure.code() == LogoException.Code.RATE_LIMITED) {
        LOGGER.log(System.Logger.Level.INFO, "Instrument logo provider rate limited; keeping symbol fallback");
        return;
      }
    }
    LOGGER.log(System.Logger.Level.WARNING, "Could not load instrument logo; keeping symbol fallback", failure);
  }

  /**
   * Decodes provider bytes as a bounded JavaFX image.
   *
   * @param data the encoded image bytes
   * @return the decoded valid image
   */
  private static Image decodeLogo(byte[] data) {
    Image image = new Image(new ByteArrayInputStream(data), 64, 64, true, true);
    if (!isValidLogo(image)) {
      throw new IllegalArgumentException("Logo image could not be decoded", image.getException());
    }
    return image;
  }

  /**
   * Reports whether a decoded image can be displayed.
   *
   * @param image the image to validate
   * @return true when decoding succeeded with positive dimensions
   */
  private static boolean isValidLogo(Image image) {
    return !image.isError() && image.getWidth() > 0 && image.getHeight() > 0;
  }

  /**
   * Publishes a legacy instrument request result if it is still current.
   *
   * @param symbol the requested symbol
   * @param load the asynchronous provider result
   */
  private void displayInstrumentLoad(String symbol, CompletionStage<InstrumentLoadResult> load) {
    long generation = ++instrumentLoadGeneration;
    instrumentLoadStartedHandler.run();
    load.whenComplete((instrument, failure) ->
      uiExecutor.execute(() -> {
        if (closed || generation != instrumentLoadGeneration) {
          return;
        }
        if (failure != null) {
          Throwable cause = unwrap(failure);
          if (!(cause instanceof CancellationException)) {
            instrumentLoadFailedHandler.accept(symbol, cause);
          }
          return;
        }
        instrumentLoadedHandler.accept(instrument);
      })
    );
  }

  /** @return the configured market-data session */
  private MarketDataSession requireMarketData() {
    if (marketDataSession == null) {
      throw new IllegalStateException("This chart has no market-data session");
    }
    return marketDataSession;
  }

  /**
   * Removes one completion-wrapper layer from an asynchronous failure.
   *
   * @param failure the reported failure
   * @return its direct cause when completion-wrapped, otherwise the original failure
   */
  private static Throwable unwrap(Throwable failure) {
    return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
  }
}
