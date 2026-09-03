package com.acteque.terminal;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketdata.MarketDataController;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoMarketDataClient;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
import java.util.concurrent.CancellationException;
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

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    TiingoMarketDataClient client = TiingoMarketDataClient.create();
    marketData = new MarketDataController(client, STOCK_SYMBOL);
    List<PricePoint> pricePoints = marketData.loadInitial();
    Chart chartView = new Chart(pricePoints, STOCK_SYMBOL, DATA_INTERVAL, client.tickerCatalog);
    chartView.setOnEarlierHistoryRequested(() ->
      marketData.loadEarlier().whenComplete((updatedPoints, failure) -> {
        if (failure != null) {
          reportEarlierHistoryLoadFailure(failure);
        } else {
          Platform.runLater(() -> chartView.setPricePoints(updatedPoints));
        }
      })
    );
    chartView.setOnInstrumentSelected(symbol ->
      marketData.loadInstrument(symbol).whenComplete((updatedPoints, failure) -> {
        if (failure != null && !(failure instanceof CancellationException)) {
          reportInstrumentLoadFailure(symbol, failure);
        } else if (failure == null) {
          Platform.runLater(() -> {
            chartView.setInstrument(symbol, updatedPoints);
            stage.setTitle(symbol);
          });
        }
      })
    );

    Scene scene = new Scene(chartView, MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT);

    // This is find for now, but we might want defined up top if we need to access the theme manager later
    new ThemeManager(scene, AppTheme.LIGHT);

    stage.setTitle(STOCK_SYMBOL);
    stage.setMinWidth(MIN_CANVAS_WIDTH);
    stage.setMinHeight(MIN_CANVAS_HEIGHT);
    stage.setScene(scene);
    stage.show();

    chartView.drawChart();
  }

  @Override
  public void stop() {
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
