package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.chart.menu.ChartMenuModel.Item;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChartMenuInteractorTest {

  @Test
  void initializesItemsAndRoutesRequests() {
    ChartMenuModel model = new ChartMenuModel();
    ChartMenuInteractor interactor = new ChartMenuInteractor(model);
    AtomicReference<Item> request = new AtomicReference<>();
    interactor.onActionRequested(request::set);

    interactor.initialize();
    interactor.request(Item.INTERVAL);

    assertEquals(List.of(Item.INSTRUMENT, Item.INTERVAL, Item.CHART_TYPE), model.getItems());
    assertSame(Item.INTERVAL, request.get());
  }
}
