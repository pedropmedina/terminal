package com.acteque.terminal.chartworkspace.intervalselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.chart.ChartInterval;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChartWorkspaceIntervalSelectionInteractorTest {

  @Test
  void initializesCategorizedIntervalsAndFiltersNormalizedQueries() {
    ChartWorkspaceIntervalSelectionModel model = new ChartWorkspaceIntervalSelectionModel();
    ChartWorkspaceIntervalSelectionInteractor interactor = new ChartWorkspaceIntervalSelectionInteractor(model);

    interactor.initialize(ChartInterval.DAILY);

    assertSame(ChartInterval.DAILY, model.getCurrentInterval());
    assertEquals(
      List.of("Ticks", "Seconds", "Minutes", "Hours", "Days"),
      model.getMatchingIntervals().keySet().stream().toList()
    );

    interactor.setQuery("  HOUR  ");

    assertEquals("hour", model.getQuery());
    assertEquals(List.of("1H", "2H", "3H", "4H"), names(model));
  }

  @Test
  void addsCustomIntervalsToTheCurrentResults() {
    ChartWorkspaceIntervalSelectionModel model = new ChartWorkspaceIntervalSelectionModel();
    ChartWorkspaceIntervalSelectionInteractor interactor = new ChartWorkspaceIntervalSelectionInteractor(model);
    interactor.initialize(ChartInterval.DAILY);
    interactor.setQuery("7h");
    assertEquals(List.of(), names(model));

    ChartInterval customInterval = ChartInterval.of(7, ChartInterval.Classification.HOURS);
    interactor.addInterval(customInterval);

    assertEquals(List.of("7H"), names(model));
    assertSame(customInterval, interactor.soleMatch());
  }

  @Test
  void findsASoleMatchAndUpdatesTheSelection() {
    ChartWorkspaceIntervalSelectionModel model = new ChartWorkspaceIntervalSelectionModel();
    ChartWorkspaceIntervalSelectionInteractor interactor = new ChartWorkspaceIntervalSelectionInteractor(model);
    interactor.initialize(ChartInterval.DAILY);

    assertNull(interactor.soleMatch());

    interactor.setQuery("4h");
    ChartInterval match = interactor.soleMatch();
    interactor.select(match);

    assertSame(ChartInterval.FOUR_HOURS, match);
    assertSame(ChartInterval.FOUR_HOURS, model.getCurrentInterval());
  }

  private static List<String> names(ChartWorkspaceIntervalSelectionModel model) {
    return model.getMatchingIntervals().values().stream().flatMap(List::stream).map(ChartInterval::name).toList();
  }
}
