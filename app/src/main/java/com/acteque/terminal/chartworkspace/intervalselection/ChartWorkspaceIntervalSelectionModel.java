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

  private Predicate<ChartInterval> availability = ignored -> true;

  boolean isAvailable(ChartInterval interval) {
    return availability.test(interval);
  }

  void setAvailability(Predicate<ChartInterval> value) {
    availability = value;
  }

  private final ReadOnlyObjectWrapper<ChartInterval> currentInterval = new ReadOnlyObjectWrapper<>(
    this,
    "currentInterval"
  );
  private final ReadOnlyBooleanWrapper open = new ReadOnlyBooleanWrapper(this, "open");
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

  ChartInterval getCurrentInterval() {
    return currentInterval.get();
  }

  ReadOnlyObjectProperty<ChartInterval> currentIntervalProperty() {
    return currentInterval.getReadOnlyProperty();
  }

  void setCurrentInterval(ChartInterval value) {
    currentInterval.set(value);
  }

  ReadOnlyBooleanProperty openProperty() {
    return open.getReadOnlyProperty();
  }

  void setOpen(boolean value) {
    open.set(value);
  }

  String getQuery() {
    return query.get();
  }

  void setQuery(String value) {
    query.set(value);
  }

  ReadOnlyListProperty<ChartInterval> intervalsProperty() {
    return intervals.getReadOnlyProperty();
  }

  void setIntervals(List<ChartInterval> values) {
    intervals.setAll(values);
  }

  void addInterval(ChartInterval value) {
    intervals.add(value);
  }

  Map<String, List<ChartInterval>> getMatchingIntervals() {
    return matchingIntervals.get();
  }

  ReadOnlyObjectProperty<Map<String, List<ChartInterval>>> matchingIntervalsProperty() {
    return matchingIntervals.getReadOnlyProperty();
  }

  void setMatchingIntervals(Map<String, List<ChartInterval>> value) {
    matchingIntervals.set(value);
  }
}
