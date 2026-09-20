package com.acteque.terminal.chartworkspace;

import com.acteque.terminal.AppService;
import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.marketdata.MarketDataSession;
import com.acteque.terminal.marketlogos.LogoSession;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import javafx.scene.layout.StackPane;

/** Composes and exposes the recursively split chart workspace. */
public final class ChartWorkspace implements AutoCloseable {

  private final ChartWorkspaceInteractor interactor;
  private final ChartWorkspaceViewBuilder viewBuilder;

  public ChartWorkspace(
    AppService services,
    String symbol,
    ChartInterval interval,
    ChartType chartType,
    Executor uiExecutor
  ) {
    this(new ChartWorkspaceSettings(symbol, interval, chartType), settings ->
      createChart(services, settings, uiExecutor)
    );
  }

  ChartWorkspace(ChartWorkspaceSettings settings, ChartWorkspaceChartFactory chartFactory) {
    ChartWorkspaceModel model = new ChartWorkspaceModel();
    interactor = new ChartWorkspaceInteractor(model, chartFactory);
    interactor.initialize(settings);
    viewBuilder = new ChartWorkspaceViewBuilder(model, interactor::activate);
  }

  public StackPane getView() {
    return viewBuilder.build();
  }

  public void start() {
    interactor.start();
  }

  @Override
  public void close() {
    interactor.close();
  }

  private static Chart createChart(AppService services, ChartWorkspaceSettings settings, Executor uiExecutor) {
    Objects.requireNonNull(services, "services cannot be null");
    Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
    MarketDataSession marketData = services.createMarketDataSession(settings.symbol());
    LogoSession logos = null;
    try {
      logos = services.createLogoSession();
      Chart chart = new Chart(
        List.of(),
        settings.symbol(),
        settings.interval(),
        services.catalog(),
        marketData,
        logos,
        uiExecutor
      );
      chart.setChartType(settings.chartType());
      return chart;
    } catch (RuntimeException | Error failure) {
      if (logos != null) {
        try {
          logos.close();
        } catch (RuntimeException closeFailure) {
          failure.addSuppressed(closeFailure);
        }
      }
      try {
        marketData.close();
      } catch (RuntimeException closeFailure) {
        failure.addSuppressed(closeFailure);
      }
      throw failure;
    }
  }
}
