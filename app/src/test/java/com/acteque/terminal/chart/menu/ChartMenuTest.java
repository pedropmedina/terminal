package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartSplitDirection;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.popover.PopoverTrigger;
import java.util.ArrayList;
import java.util.List;
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

  @Test
  void routesDirectionalSplitActionsAndShowsCloseOnlyWhenAvailable() {
    FxTestSupport.runAndWait(() -> {
      ChartMenu menu = new ChartMenu("ACME", ChartInterval.DAILY);
      List<ChartSplitDirection> splits = new ArrayList<>();
      AtomicInteger closes = new AtomicInteger();
      menu.onSplitRequested(splits::add);
      menu.onCloseRequested(closes::incrementAndGet);

      ChartMenuItems items = assertInstanceOf(ChartMenuItems.class, menu.getView());
      PopoverTrigger trigger = assertInstanceOf(PopoverTrigger.class, items.getChildren().get(3));
      trigger
        .getPopover()
        .getContent()
        .lookupAll(".chart-split-action")
        .stream()
        .map(Button.class::cast)
        .sorted((first, second) -> first.getText().compareTo(second.getText()))
        .forEach(Button::fire);

      assertEquals(
        List.of(
          ChartSplitDirection.BOTTOM,
          ChartSplitDirection.LEFT,
          ChartSplitDirection.RIGHT,
          ChartSplitDirection.TOP
        ),
        splits
      );
      assertEquals(4, items.getChildren().size());

      menu.setCloseAvailable(true);
      assertEquals(5, items.getChildren().size());
      assertEquals("Close chart", ((Button) items.getChildren().getLast()).getAccessibleText());
      ((Button) items.getChildren().getLast()).fire();
      assertEquals(1, closes.get());

      menu.setCloseAvailable(false);
      assertEquals(4, items.getChildren().size());
      menu.close();
    });
  }
}
