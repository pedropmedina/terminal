package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.buttongroup.ButtonGroup;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ChartMenuTest {

  @Test
  void exposesAnIndependentActionForEachMenuItem() {
    FxTestSupport.runAndWait(() -> {
      ChartMenu menu = new ChartMenu();
      AtomicInteger instrumentRequests = new AtomicInteger();
      AtomicInteger intervalRequests = new AtomicInteger();
      AtomicInteger chartTypeRequests = new AtomicInteger();
      menu.onInstrumentSelectionRequested(instrumentRequests::incrementAndGet);
      menu.onIntervalSelectionRequested(intervalRequests::incrementAndGet);
      menu.onChartTypeSelectionRequested(chartTypeRequests::incrementAndGet);

      ButtonGroup group = assertInstanceOf(ButtonGroup.class, menu.getView().getChildrenUnmodifiable().getFirst());
      group.getChildren().forEach(node -> assertInstanceOf(Button.class, node).fire());

      assertEquals(1, instrumentRequests.get());
      assertEquals(1, intervalRequests.get());
      assertEquals(1, chartTypeRequests.get());
    });
  }
}
