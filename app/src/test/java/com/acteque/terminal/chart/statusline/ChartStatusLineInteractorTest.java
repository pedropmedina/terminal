package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.chart.PricePoint;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ChartStatusLineInteractorTest {

  @Test
  void updatesAndClearsStatusMetadata() {
    ChartStatusLineModel model = new ChartStatusLineModel();
    ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
    PricePoint point = new PricePoint(LocalDate.of(2026, 8, 24), 1, 2, 0.5, 1.5, 100);

    interactor.setPricePoint(point);

    assertSame(point, model.getPricePoint());

    interactor.clearPricePoint();

    assertNull(model.getPricePoint());
  }
}
