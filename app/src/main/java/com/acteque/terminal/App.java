package com.acteque.terminal;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chartworkspace.ChartWorkspace;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * A small JavaFX Canvas application that plots stock prices fetched from the configured provider.
 *
 * <p>The app uses Canvas instead of JavaFX chart controls so the coordinate mapping, axes, and
 * drawing steps are visible in one place. That makes it easier to understand how raw market data is
 * transformed into screen coordinates.
 */
public class App extends Application {

  private static final String SYMBOL = "IBM";
  private static final ChartInterval INTERVAL = ChartInterval.DAILY;
  private static final ChartType CHART_TYPE = ChartType.CANDLESTICK;
  private static final double MIN_CANVAS_WIDTH = 1060.0;
  private static final double MIN_CANVAS_HEIGHT = 760.0;

  private ChartWorkspace chartWorkspace;
  private AppService services;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    Dotenv configuration = Dotenv.configure().ignoreIfMissing().load();
    services = AppService.create(configuration::get);
    try {
      startChart(stage);
    } catch (RuntimeException | Error failure) {
      try {
        stop();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private void startChart(Stage stage) {
    chartWorkspace = new ChartWorkspace(services, SYMBOL, INTERVAL, CHART_TYPE, Platform::runLater);
    Scene scene = new Scene(chartWorkspace.getView(), MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT);

    // This is find for now, but we might want defined up top if we need to access the theme manager later
    new AppThemeManager(scene, AppTheme.LIGHT);

    stage.setTitle("Terminal");
    stage.setMinWidth(MIN_CANVAS_WIDTH);
    stage.setMinHeight(MIN_CANVAS_HEIGHT);
    stage.setScene(scene);
    stage.show();

    chartWorkspace.start();
  }

  @Override
  public void stop() {
    try {
      if (chartWorkspace != null) {
        chartWorkspace.close();
      }
    } finally {
      if (services != null) {
        services.close();
      }
    }
  }
}
