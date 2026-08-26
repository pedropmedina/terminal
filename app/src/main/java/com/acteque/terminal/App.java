package com.acteque.terminal;

import java.util.List;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoMarketDataClient;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
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
    marketData = new MarketDataController(TiingoMarketDataClient.create(), STOCK_SYMBOL);
    List<PricePoint> pricePoints = marketData.loadInitial();
    Chart chartView = new Chart(pricePoints, STOCK_SYMBOL, DATA_INTERVAL);
    chartView.setOnEarlierHistoryRequested(() ->
      marketData.loadEarlier().whenComplete((updatedPoints, failure) -> {
        if (failure != null) {
          reportEarlierHistoryLoadFailure(failure);
        } else {
          Platform.runLater(() -> chartView.setPricePoints(updatedPoints));
        }
      })
    );

    Scene scene = new Scene(chartView, MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT, Color.WHITE);

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
}
