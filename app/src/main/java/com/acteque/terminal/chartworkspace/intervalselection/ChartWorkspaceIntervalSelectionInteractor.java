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
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Applies interval-selection state transitions without depending on its layout. */
final class ChartWorkspaceIntervalSelectionInteractor {

  private final ChartWorkspaceIntervalSelectionModel model;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Runnable closeRequestHandler = () -> {};

  /**
   * Creates an interactor backed by the supplied interval-selection state.
   *
   * @param model the observable interval-selection state
   */
  ChartWorkspaceIntervalSelectionInteractor(ChartWorkspaceIntervalSelectionModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  /**
   * Initializes the standard intervals, current selection, query, and closed state.
   *
   * @param currentInterval the interval initially displayed as selected
   */
  void initialize(ChartInterval currentInterval) {
    model.setCurrentInterval(Objects.requireNonNull(currentInterval, "currentInterval cannot be null"));
    model.setIntervals(List.of(ChartInterval.values()));
    model.setOpen(false);
    setQuery("");
  }

  /** Opens the interval-selection dialog. */
  void show() {
    model.setOpen(true);
  }

  /** Closes the interval-selection dialog. */
  void close() {
    model.setOpen(false);
  }

  /**
   * Updates the interval displayed as selected without notifying the selection callback.
   *
   * @param interval the active chart interval
   */
  void setCurrentInterval(ChartInterval interval) {
    model.setCurrentInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  /**
   * Updates which intervals the active chart can request.
   *
   * @param availability the active chart's interval support check
   */
  void setAvailability(Predicate<ChartInterval> availability) {
    model.setAvailability(Objects.requireNonNull(availability, "availability cannot be null"));
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

  /**
   * Adds an available custom interval and refreshes the current query results.
   *
   * @param interval the custom interval to add
   */
  void addInterval(ChartInterval interval) {
    ChartInterval requestedInterval = Objects.requireNonNull(interval, "interval cannot be null");
    if (!model.isAvailable(requestedInterval)) {
      return;
    }

    model.addInterval(requestedInterval);
    setQuery(model.getQuery());
  }

  /**
   * Registers the action invoked after an interval is selected.
   *
   * @param callback the selected-interval callback
   */
  void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Registers the action invoked when the dialog requests external closure.
   *
   * @param callback the close-request callback
   */
  void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /**
   * Applies an available user-selected interval, closes the dialog, and notifies its listener.
   *
   * @param interval the selected interval
   */
  void selectInterval(ChartInterval interval) {
    ChartInterval selectedInterval = Objects.requireNonNull(interval, "interval cannot be null");
    if (!model.isAvailable(selectedInterval)) {
      return;
    }

    model.setCurrentInterval(selectedInterval);
    close();
    intervalSelectedHandler.accept(selectedInterval);
  }

  /** Selects the sole available query match when one exists. */
  void selectSoleMatch() {
    ChartInterval interval = soleMatch();
    if (interval != null) {
      selectInterval(interval);
    }
  }

  /** Closes the dialog and notifies the external close-request listener. */
  void requestClose() {
    close();
    closeRequestHandler.run();
  }

  /**
   * Returns the sole available query match when exactly one exists.
   *
   * @return the sole available match, or null when the result count is not one
   */
  ChartInterval soleMatch() {
    List<ChartInterval> matches = model
      .getMatchingIntervals()
      .values()
      .stream()
      .flatMap(List::stream)
      .filter(model::isAvailable)
      .toList();
    return matches.size() == 1 ? matches.getFirst() : null;
  }
}
