package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartIntervalText;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import javafx.beans.value.ObservableBooleanValue;

/** Applies interval-selection state transitions without depending on its layout. */
final class ChartWorkspaceIntervalSelectionInteractor {

  private final ChartWorkspaceIntervalSelectionModel model;

  ChartWorkspaceIntervalSelectionInteractor(ChartWorkspaceIntervalSelectionModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  void initialize(ChartInterval currentInterval) {
    model.setCurrentInterval(Objects.requireNonNull(currentInterval, "currentInterval cannot be null"));
    model.setIntervals(List.of(ChartInterval.values()));
    model.setOpen(false);
    setQuery("");
  }

  ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  void show() {
    model.setOpen(true);
  }

  void close() {
    model.setOpen(false);
  }

  void setCurrentInterval(ChartInterval interval) {
    model.setCurrentInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  /**
   * Filters intervals by short label first, then by descriptive text.
   *
   * @param query the entered search text, or null to show all intervals
   */
  void setQuery(String query) {
    String normalizedQuery = query == null ? "" : query.strip().toLowerCase(Locale.ROOT);
    model.setQuery(normalizedQuery);

    List<ChartInterval> exactMatches = model
      .intervalsProperty()
      .stream()
      .filter(interval -> interval.name().toLowerCase(Locale.ROOT).equals(normalizedQuery))
      .toList();
    List<ChartInterval> matches = exactMatches.isEmpty()
      ? model
          .intervalsProperty()
          .stream()
          .filter(interval -> ChartIntervalText.matches(interval, normalizedQuery))
          .toList()
      : exactMatches;
    Map<String, List<ChartInterval>> matchingByCategory = new LinkedHashMap<>();
    matches.forEach(interval ->
      matchingByCategory
        .computeIfAbsent(ChartIntervalText.category(interval), ignored -> new ArrayList<>())
        .add(interval)
    );
    matchingByCategory.replaceAll((ignored, intervals) -> List.copyOf(intervals));
    model.setMatchingIntervals(Collections.unmodifiableMap(new LinkedHashMap<>(matchingByCategory)));
  }

  void addInterval(ChartInterval interval) {
    model.addInterval(Objects.requireNonNull(interval, "interval cannot be null"));
    setQuery(model.getQuery());
  }

  void select(ChartInterval interval) {
    model.setCurrentInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  ChartInterval soleMatch() {
    List<ChartInterval> matches = model.getMatchingIntervals().values().stream().flatMap(List::stream).toList();
    return matches.size() == 1 ? matches.getFirst() : null;
  }
}
