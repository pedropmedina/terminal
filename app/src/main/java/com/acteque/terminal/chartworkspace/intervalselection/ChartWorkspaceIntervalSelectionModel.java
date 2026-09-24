package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;

/** Observable state shared by the interval-selection MVCI components. */
final class ChartWorkspaceIntervalSelectionModel {

  private final ReadOnlyObjectWrapper<ChartInterval> currentInterval = new ReadOnlyObjectWrapper<>(
    this,
    "currentInterval"
  );
  private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");
  private final ReadOnlyObjectWrapper<Predicate<ChartInterval>> availability = new ReadOnlyObjectWrapper<>(
    this,
    "availability",
    ignored -> true
  );
  private final ReadOnlyStringWrapper query = new ReadOnlyStringWrapper(this, "query", "");
  private final ReadOnlyListWrapper<ChartInterval> intervals = new ReadOnlyListWrapper<>(
    this,
    "intervals",
    FXCollections.observableArrayList()
  );
  private final ReadOnlyObjectWrapper<Map<String, List<ChartInterval>>> matchingIntervals = new ReadOnlyObjectWrapper<>(
    this,
    "matchingIntervals",
    Map.of()
  );

  /**
   * Returns the interval displayed as selected.
   *
   * @return the current interval
   */
  ChartInterval getCurrentInterval() {
    return currentInterval.get();
  }

  /**
   * Returns the observable current interval.
   *
   * @return the read-only current-interval property
   */
  ReadOnlyObjectProperty<ChartInterval> currentIntervalProperty() {
    return currentInterval.getReadOnlyProperty();
  }

  /**
   * Updates the interval displayed as selected.
   *
   * @param value the current interval
   */
  void setCurrentInterval(ChartInterval value) {
    currentInterval.set(value);
  }

  /**
   * Reports whether the interval-selection dialog is open.
   *
   * @return true when the dialog is open
   */
  boolean isOpen() {
    return open.get();
  }

  /**
   * Returns the observable dialog state.
   *
   * @return the read-only open property
   */
  ReadOnlyBooleanProperty openProperty() {
    return open.getReadOnlyProperty();
  }

  /**
   * Updates the dialog state.
   *
   * @param value true to open the dialog
   */
  void setOpen(boolean value) {
    open.set(value);
  }

  /**
   * Reports whether the active chart supports an interval.
   *
   * @param interval the interval to inspect
   * @return true when the interval is available
   */
  boolean isAvailable(ChartInterval interval) {
    return availability.get().test(interval);
  }

  /**
   * Returns the observable interval support check.
   *
   * @return the read-only availability property
   */
  ReadOnlyObjectProperty<Predicate<ChartInterval>> availabilityProperty() {
    return availability.getReadOnlyProperty();
  }

  /**
   * Updates the interval support check.
   *
   * @param value the active chart's interval support check
   */
  void setAvailability(Predicate<ChartInterval> value) {
    availability.set(value);
  }

  /**
   * Returns the normalized filter query.
   *
   * @return the normalized query
   */
  String getQuery() {
    return query.get();
  }

  /**
   * Updates the normalized filter query.
   *
   * @param value the normalized query
   */
  void setQuery(String value) {
    query.set(value);
  }

  /**
   * Returns all standard and custom interval choices.
   *
   * @return the read-only interval-list property
   */
  ReadOnlyListProperty<ChartInterval> intervalsProperty() {
    return intervals.getReadOnlyProperty();
  }

  /**
   * Replaces all interval choices.
   *
   * @param values the interval choices
   */
  void setIntervals(List<ChartInterval> values) {
    intervals.setAll(values);
  }

  /**
   * Adds a custom interval choice.
   *
   * @param value the custom interval
   */
  void addInterval(ChartInterval value) {
    intervals.add(value);
  }

  /**
   * Returns query matches grouped by presentation category.
   *
   * @return the immutable category-to-interval mapping
   */
  Map<String, List<ChartInterval>> getMatchingIntervals() {
    return matchingIntervals.get();
  }

  /**
   * Returns the observable categorized query results.
   *
   * @return the read-only matching-interval property
   */
  ReadOnlyObjectProperty<Map<String, List<ChartInterval>>> matchingIntervalsProperty() {
    return matchingIntervals.getReadOnlyProperty();
  }

  /**
   * Replaces the categorized query results.
   *
   * @param value the immutable category-to-interval mapping
   */
  void setMatchingIntervals(Map<String, List<ChartInterval>> value) {
    matchingIntervals.set(value);
  }
}
