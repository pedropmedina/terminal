package com.acteque.terminal;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartLogoSource;
import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.marketdata.LogoMarketDataClient;
import com.acteque.terminal.marketdata.MarketDataController;
import com.acteque.terminal.marketdata.MarketDataController.LoadedInstrument;
import com.acteque.terminal.marketdata.provider.elbstream.ElbstreamInstrumentLogos;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoMarketDataClient;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionStage;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A small JavaFX Canvas application that plots stock prices fetched from Tiingo.
 *
 * <p>The app uses Canvas instead of JavaFX chart controls so the coordinate mapping, axes, and
 * drawing steps are visible in one place. That makes it easier to understand how raw market data is
 * transformed into screen coordinates.
 */
public class App extends Application {

  private static final String STOCK_SYMBOL = "IBM";
  private static final ChartInterval DATA_INTERVAL = ChartInterval.DAILY;
  private static final double MIN_CANVAS_WIDTH = 760.0;
  private static final double MIN_CANVAS_HEIGHT = 460.0;

  private MarketDataController marketData;
  private Chart chartView;
  private long instrumentLoadGeneration;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    TiingoMarketDataClient client = TiingoMarketDataClient.create();
    marketData = new MarketDataController(
      new LogoMarketDataClient(client, ElbstreamInstrumentLogos.create()),
      STOCK_SYMBOL
    );
    chartView = new Chart(
      List.of(),
      STOCK_SYMBOL,
      DATA_INTERVAL,
      client.tickerCatalog,
      new ChartLogoSource() {
        @Override
        public CompletionStage<Optional<byte[]>> load(InstrumentLogo logo) {
          return marketData.loadLogo(logo);
        }

        @Override
        public void cancel() {
          marketData.cancelLogoLoad();
        }
      },
      Platform::runLater
    );
    chartView.setOnEarlierHistoryRequested(() -> {
      long generation = instrumentLoadGeneration;
      marketData.loadEarlier().whenComplete((updatedBars, failure) -> {
        Platform.runLater(() -> {
          if (generation != instrumentLoadGeneration) {
            return;
          }
          if (failure != null) {
            reportEarlierHistoryLoadFailure(failure);
          } else {
            chartView.setBars(updatedBars);
          }
        });
      });
    });
    chartView.setOnInstrumentSelected(symbol ->
      displayInstrumentLoad(symbol, marketData.loadInstrument(symbol), chartView, stage)
    );

    Scene scene = new Scene(chartView.getView(), MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT);

    // This is find for now, but we might want defined up top if we need to access the theme manager later
    new ThemeManager(scene, AppTheme.LIGHT);

    stage.setTitle(STOCK_SYMBOL);
    stage.setMinWidth(MIN_CANVAS_WIDTH);
    stage.setMinHeight(MIN_CANVAS_HEIGHT);
    stage.setScene(scene);
    stage.show();

    chartView.drawChart();
    displayInstrumentLoad(STOCK_SYMBOL, marketData.loadInitial(), chartView, stage);
  }

  private void displayInstrumentLoad(
    String symbol,
    CompletionStage<LoadedInstrument> load,
    Chart chartView,
    Stage stage
  ) {
    long generation = ++instrumentLoadGeneration;
    chartView.beginInstrumentLoad();
    load.whenComplete((instrument, failure) ->
      Platform.runLater(() -> {
        // A completed background load can still be stale by the time this UI task runs.
        if (generation != instrumentLoadGeneration) {
          return;
        }
        if (failure != null) {
          Throwable cause = failure.getCause() == null ? failure : failure.getCause();
          if (!(cause instanceof CancellationException)) {
            reportInstrumentLoadFailure(symbol, failure);
          }
          return;
        }
        chartView.setInstrument(
          instrument.symbol(),
          instrument.displayName(),
          instrument.bars(),
          instrument.details().logo()
        );
        stage.setTitle(instrument.symbol());
      })
    );
  }

  @Override
  public void stop() {
    ++instrumentLoadGeneration;
    if (chartView != null) {
      chartView.beginInstrumentLoad();
    }
    if (marketData != null) {
      marketData.close();
    }
  }

  private static void reportEarlierHistoryLoadFailure(Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    System.err.println("Unable to load earlier price history: " + cause.getMessage());
  }

  private static void reportInstrumentLoadFailure(String symbol, Throwable failure) {
    Throwable cause = failure.getCause() == null ? failure : failure.getCause();
    System.err.println("Unable to load " + symbol + ": " + cause.getMessage());
  }
}
