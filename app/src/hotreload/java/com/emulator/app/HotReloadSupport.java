package com.emulator.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import javafx.application.Platform;

/** Development hooks invoked by the project's HotswapAgent plugin. */
public final class HotReloadSupport implements ChartReloadRegistrar {

  private static final Set<PriceChartCanvas> CHARTS =
      Collections.newSetFromMap(new WeakHashMap<>());

  @Override
  public void register(Object chart) {
    if (!(chart instanceof PriceChartCanvas priceChart)) {
      return;
    }

    synchronized (CHARTS) {
      CHARTS.add(priceChart);
    }
  }

  public static void redrawChartsAfterReload() {
    Platform.runLater(
        () -> {
          ArrayList<PriceChartCanvas> charts;
          synchronized (CHARTS) {
            charts = new ArrayList<>(CHARTS);
          }
          charts.forEach(PriceChartCanvas::drawChart);
        });
  }
}
