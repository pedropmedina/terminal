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
    assertEquals(List.of("1h", "2h", "3h", "4h"), names(model));
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

    assertEquals(List.of("7h"), names(model));
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

  @Test
  void prioritizesExactShortLabelsAndFindsYearlyByName() {
    ChartWorkspaceIntervalSelectionModel model = new ChartWorkspaceIntervalSelectionModel();
    ChartWorkspaceIntervalSelectionInteractor interactor = new ChartWorkspaceIntervalSelectionInteractor(model);
    interactor.initialize(ChartInterval.DAILY);

    interactor.setQuery("M");
    assertEquals(List.of("M"), names(model));
    assertSame(ChartInterval.MONTHLY, interactor.soleMatch());

    interactor.setQuery("1m");
    assertEquals(List.of("1m"), names(model));
    assertSame(ChartInterval.ONE_MINUTE, interactor.soleMatch());

    for (String label : List.of("D", "W", "Y")) {
      interactor.setQuery(label);
      assertEquals(List.of(label), names(model));
    }

    interactor.setQuery("yearly");
    assertEquals(List.of("Y"), names(model));
  }

  private static List<String> names(ChartWorkspaceIntervalSelectionModel model) {
    return model.getMatchingIntervals().values().stream().flatMap(List::stream).map(ChartInterval::name).toList();
  }
}
