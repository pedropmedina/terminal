package com.emulator.app;

import java.util.List;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

/**
 * A small JavaFX Canvas application that plots AAPL close prices from a bundled CSV file.
 *
 * <p>The app uses Canvas instead of JavaFX chart controls so the coordinate mapping, axes, and
 * drawing steps are visible in one place. That makes it easier to understand how raw market data is
 * transformed into screen coordinates.
 */
public class App extends Application {

  private static final String CSV_RESOURCE = "/daily_IBM.csv";
  private static final double MIN_CANVAS_WIDTH = 760.0;
  private static final double MIN_CANVAS_HEIGHT = 460.0;
  private static final String CHART_TITLE = "AAPL Close Price - Last 2 Months";

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    List<PricePoint> pricePoints = PriceDataLoader.loadFromResource(CSV_RESOURCE);
    PriceChartCanvas chart = new PriceChartCanvas(pricePoints, CHART_TITLE);

    Pane root = new Pane(chart);
    Scene scene = new Scene(root, MIN_CANVAS_WIDTH, MIN_CANVAS_HEIGHT, Color.WHITE);

    // Keep the canvas the same size as the window content and redraw after every resize.
    chart.widthProperty().bind(root.widthProperty());
    chart.heightProperty().bind(root.heightProperty());

    stage.setTitle(CHART_TITLE);
    stage.setMinWidth(MIN_CANVAS_WIDTH);
    stage.setMinHeight(MIN_CANVAS_HEIGHT);
    stage.setScene(scene);
    stage.show();

    chart.drawChart();
  }
}
