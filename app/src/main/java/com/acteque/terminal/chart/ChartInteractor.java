package com.acteque.terminal.chart;

import java.util.Objects;
import java.util.function.Consumer;

/** Applies chart-level state transitions without depending on JavaFX controls or layout. */
final class ChartInteractor {

  private final ChartModel model;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};

  ChartInteractor(ChartModel model) {
    this.model = Objects.requireNonNull(model, "model");
  }

  void initialize(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval"));
  }

  void openInstrumentSearch() {
    if (!model.isModalOpen()) {
      model.setInstrumentSearchOpen(true);
    }
  }

  void closeInstrumentSearch() {
    model.setInstrumentSearchOpen(false);
  }

  void openIntervalSelection() {
    if (!model.isModalOpen()) {
      model.setIntervalSelectionOpen(true);
    }
  }

  void closeIntervalSelection() {
    model.setIntervalSelectionOpen(false);
  }

  void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback");
  }

  void selectInterval(ChartInterval interval) {
    ChartInterval selectedInterval = Objects.requireNonNull(interval, "interval");
    model.setInterval(selectedInterval);
    closeIntervalSelection();
    intervalSelectedHandler.accept(selectedInterval);
  }
}
