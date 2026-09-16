package com.acteque.terminal;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.marketdata.DefaultMarketDataSession;
import com.acteque.terminal.marketdata.InstrumentCatalog;
import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.MarketDataProviderRegistry;
import com.acteque.terminal.marketdata.provider.tiingo.TiingoProviderFactory;
import com.acteque.terminal.marketlogos.DefaultLogoSession;
import com.acteque.terminal.marketlogos.provider.elbstream.ElbstreamInstrumentLogos;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import io.github.cdimascio.dotenv.Dotenv;
import java.util.List;
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
  private static final double MIN_CANVAS_WIDTH = 1060.0;
  private static final double MIN_CANVAS_HEIGHT = 760.0;

  private Chart chartView;
  private MarketDataProviderRegistry providers;

  public static void main(String[] args) {
    launch(args);
  }

  @Override
  public void start(Stage stage) {
    Dotenv configuration = Dotenv.configure().ignoreIfMissing().load();
    providers = new MarketDataProviderRegistry(List.of(new TiingoProviderFactory()));
    try {
      startChart(stage, configuration);
    } catch (RuntimeException | Error failure) {
      try {
        stop();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }

  private void startChart(Stage stage, Dotenv configuration) {
    String selectedProvider = configuration.get("MARKET_DATA_PROVIDER");
    MarketDataClient client = providers.create(
      selectedProvider == null ? "tiingo" : selectedProvider.strip(),
      configuration::get
    );
    InstrumentCatalog catalog = client
      .catalog()
      .orElseThrow(() ->
        new IllegalStateException(
          "Provider " + client.provider() + " does not support the instrument catalog required by this application"
        )
      );
    DefaultMarketDataSession marketData = new DefaultMarketDataSession(client, SYMBOL);
    DefaultLogoSession logos = new DefaultLogoSession(ElbstreamInstrumentLogos.create());

    try {
      chartView = new Chart(List.of(), SYMBOL, INTERVAL, catalog, marketData, logos, Platform::runLater);
    } catch (RuntimeException | Error failure) {
      logos.close();
      marketData.close();
      throw failure;
    }

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
    try {
      if (chartView != null) {
        chartView.close();
      }
    } finally {
      if (providers != null) {
        providers.close();
      }
    }
  }
}
