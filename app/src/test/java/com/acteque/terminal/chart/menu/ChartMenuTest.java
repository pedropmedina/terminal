package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChartMenuTest {

  @Test
  void exposesAnIndependentActionForEachMenuItem() {
    FxTestSupport.runAndWait(() -> {
      ChartMenu menu = new ChartMenu("ACME", ChartInterval.DAILY);
      AtomicInteger instrumentRequests = new AtomicInteger();
      AtomicInteger intervalRequests = new AtomicInteger();
      AtomicInteger chartTypeRequests = new AtomicInteger();
      menu.onInstrumentSelectionRequested(instrumentRequests::incrementAndGet);
      menu.onIntervalSelectionRequested(intervalRequests::incrementAndGet);
      menu.onChartTypeSelectionRequested(chartTypeRequests::incrementAndGet);

      ChartMenuItems items = assertInstanceOf(ChartMenuItems.class, menu.getView());
      items.getChildren().forEach(node -> assertInstanceOf(Button.class, node).fire());

      assertEquals(1, instrumentRequests.get());
      assertEquals(1, intervalRequests.get());
      assertEquals(1, chartTypeRequests.get());
    });
  }
}
