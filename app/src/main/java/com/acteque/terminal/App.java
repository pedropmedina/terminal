package com.acteque.terminal;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chartworkspace.ChartWorkspace;
import io.github.cdimascio.dotenv.Dotenv;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Loads application configuration and manages the JavaFX and service lifecycles. */
public class App extends Application {

  private static final String SYMBOL = "IBM";
  private static final ChartInterval INTERVAL = ChartInterval.DAILY;
  private static final ChartType CHART_TYPE = ChartType.CANDLESTICK;
  private static final AppTheme THEME = AppTheme.LIGHT;
  private static final double WINDOW_WIDTH = 1060.0;
  private static final double WINDOW_HEIGHT = 760.0;

  private AppService service;
  private ChartWorkspace chartWorkspace;

  /**
   * Launches the JavaFX application.
   *
   * @param arguments the command-line arguments forwarded to JavaFX
   */
  public static void main(String[] args) {
    launch(args);
  }

  /**
   * Composes and displays the primary application window.
   *
   * @param stage the primary JavaFX stage
   */
  @Override
  public void start(Stage stage) {
    Dotenv configuration = Dotenv.configure().ignoreIfMissing().load();
    service = AppService.create(configuration::get);
    try {
      Scene scene = initializeWorkspace();
      showStage(stage, scene);
      chartWorkspace.start();
    } catch (RuntimeException | Error failure) {
      closeAfterStartupFailure(failure);
      throw failure;
    }
  }

  /** Closes the workspace before releasing its shared application services. */
  @Override
  public void stop() {
    try {
      if (chartWorkspace != null) {
        chartWorkspace.close();
      }
    } finally {
      if (service != null) {
        service.close();
      }
    }
  }

  /**
   * Creates the workspace and its scene-level theme manager.
   *
   * @return the composed application scene
   */
  private Scene initializeWorkspace() {
    chartWorkspace = new ChartWorkspace(service, SYMBOL, INTERVAL, CHART_TYPE, Platform::runLater);
    Scene scene = new Scene(chartWorkspace.getView(), WINDOW_WIDTH, WINDOW_HEIGHT);

    // TODO: Come back to the theme once we have a better idea of how to implement settings
    new AppThemeManager(scene, THEME);

    return scene;
  }

  /**
   * Configures and displays the primary stage.
   *
   * @param stage the primary JavaFX stage
   * @param scene the composed application scene
   */
  private void showStage(Stage stage, Scene scene) {
    stage.setTitle("Terminal");
    stage.setMinWidth(WINDOW_WIDTH);
    stage.setMinHeight(WINDOW_HEIGHT);
    stage.setScene(scene);
    stage.show();
  }

  /**
   * Releases partially initialized resources and attaches cleanup failures to the startup failure.
   *
   * @param startupFailure the failure that interrupted application startup
   */
  private void closeAfterStartupFailure(Throwable startupFailure) {
    try {
      stop();
    } catch (RuntimeException closeFailure) {
      startupFailure.addSuppressed(closeFailure);
    }
  }
}
