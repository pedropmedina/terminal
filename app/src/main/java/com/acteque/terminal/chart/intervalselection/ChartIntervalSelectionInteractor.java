package com.acteque.terminal.chart.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartIntervalText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Applies interval-selection state transitions without depending on its layout. */
final class ChartIntervalSelectionInteractor {

  private final ChartIntervalSelectionModel model;

  ChartIntervalSelectionInteractor(ChartIntervalSelectionModel model) {
    this.model = Objects.requireNonNull(model, "model");
  }

  void initialize(ChartInterval currentInterval) {
    model.setCurrentInterval(Objects.requireNonNull(currentInterval, "currentInterval"));
    model.setIntervals(List.of(ChartInterval.values()));
    setQuery("");
  }

  void setCurrentInterval(ChartInterval interval) {
    model.setCurrentInterval(Objects.requireNonNull(interval, "interval"));
  }

  void setQuery(String query) {
    String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    model.setQuery(normalizedQuery);

    Map<String, List<ChartInterval>> matchingByCategory = new LinkedHashMap<>();
    model
      .intervalsProperty()
      .stream()
      .filter(interval -> ChartIntervalText.matches(interval, normalizedQuery))
      .forEach(interval ->
        matchingByCategory
          .computeIfAbsent(ChartIntervalText.category(interval), ignored -> new ArrayList<>())
          .add(interval)
      );
    matchingByCategory.replaceAll((ignored, intervals) -> List.copyOf(intervals));
    model.setMatchingIntervals(Collections.unmodifiableMap(new LinkedHashMap<>(matchingByCategory)));
  }

  void addInterval(ChartInterval interval) {
    model.addInterval(Objects.requireNonNull(interval, "interval"));
    setQuery(model.getQuery());
  }

  void select(ChartInterval interval) {
    model.setCurrentInterval(Objects.requireNonNull(interval, "interval"));
  }

  ChartInterval soleMatch() {
    List<ChartInterval> matches = model.getMatchingIntervals().values().stream().flatMap(List::stream).toList();
    return matches.size() == 1 ? matches.getFirst() : null;
  }
}
