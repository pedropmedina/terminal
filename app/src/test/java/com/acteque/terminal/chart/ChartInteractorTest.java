package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class ChartInteractorTest {

  @Test
  void initializesTheCurrentInterval() {
    ChartModel model = new ChartModel();
    ChartInteractor interactor = new ChartInteractor(model);

    interactor.initialize(ChartInterval.DAILY);

    assertEquals(ChartInterval.DAILY, model.getInterval());
  }

  @Test
  void keepsChartModalsMutuallyExclusive() {
    ChartModel model = new ChartModel();
    ChartInteractor interactor = new ChartInteractor(model);

    interactor.openInstrumentSearch();
    interactor.openIntervalSelection();

    assertTrue(model.isInstrumentSearchOpen());
    assertFalse(model.isIntervalSelectionOpen());
    assertTrue(model.isModalOpen());

    interactor.closeInstrumentSearch();
    interactor.openIntervalSelection();

    assertFalse(model.isInstrumentSearchOpen());
    assertTrue(model.isIntervalSelectionOpen());
  }

  @Test
  void selectingAnIntervalUpdatesStateAndClosesTheDialog() {
    ChartModel model = new ChartModel();
    ChartInteractor interactor = new ChartInteractor(model);
    interactor.initialize(ChartInterval.DAILY);
    interactor.openIntervalSelection();
    AtomicReference<ChartInterval> selectedInterval = new AtomicReference<>();
    interactor.onIntervalSelected(selectedInterval::set);

    interactor.selectInterval(ChartInterval.ONE_HOUR);

    assertEquals(ChartInterval.ONE_HOUR, model.getInterval());
    assertEquals(ChartInterval.ONE_HOUR, selectedInterval.get());
    assertFalse(model.isIntervalSelectionOpen());
    assertFalse(model.isModalOpen());
  }
}
