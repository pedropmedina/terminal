package com.acteque.terminal;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.marketdata.LogoMarketDataClient;
import com.acteque.terminal.marketdata.MarketDataController;
import com.acteque.terminal.marketdata.provider.elbstream.ElbstreamInstrumentLogos;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoMarketDataClient;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.List;
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
  private static final double MIN_CANVAS_WIDTH = 1060.0;
  private static final double MIN_CANVAS_HEIGHT = 760.0;

  private Chart chartView;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    TiingoMarketDataClient client = TiingoMarketDataClient.create();
    MarketDataController marketData = new MarketDataController(
      new LogoMarketDataClient(client, ElbstreamInstrumentLogos.create()),
      STOCK_SYMBOL
    );
    chartView = new Chart(
      List.of(),
      STOCK_SYMBOL,
      DATA_INTERVAL,
      client.instrumentCatalog,
      marketData,
      Platform::runLater
    );

    Scene scene = new Scene(chartView.getView(), MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT);

    // This is find for now, but we might want defined up top if we need to access the theme manager later
    new ThemeManager(scene, AppTheme.LIGHT);

    stage.setTitle("Terminal");
    stage.setMinWidth(MIN_CANVAS_WIDTH);
    stage.setMinHeight(MIN_CANVAS_HEIGHT);
    stage.setScene(scene);
    stage.show();

    chartView.drawChart();
    chartView.loadInitialInstrument();
  }

  @Override
  public void stop() {
    if (chartView != null) {
      chartView.close();
    }
  }
}
