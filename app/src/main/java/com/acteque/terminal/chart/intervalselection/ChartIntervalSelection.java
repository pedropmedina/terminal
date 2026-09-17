package com.acteque.terminal.chart.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.ui.core.dialog.Dialog;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the chart interval-selection MVCI feature. */
public final class ChartIntervalSelection {

  private final ChartIntervalSelectionInteractor interactor;
  private final ChartIntervalSelectionViewBuilder viewBuilder;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Runnable closeRequestHandler = () -> {};

  public ChartIntervalSelection(ChartInterval currentInterval, ObservableBooleanValue open) {
    ChartIntervalSelectionModel model = new ChartIntervalSelectionModel();
    interactor = new ChartIntervalSelectionInteractor(model);
    interactor.initialize(currentInterval);
    viewBuilder = new ChartIntervalSelectionViewBuilder(
      model,
      Objects.requireNonNull(open, "open cannot be null"),
      interactor::setQuery,
      interactor::addInterval,
      this::select,
      this::selectSoleMatch,
      this::requestClose
    );
  }

  public Dialog getView() {
    return viewBuilder.build();
  }

  public void setCurrentInterval(ChartInterval interval) {
    interactor.setCurrentInterval(interval);
  }

  public void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  private void select(ChartInterval interval) {
    interactor.select(interval);
    viewBuilder.close();
    intervalSelectedHandler.accept(interval);
  }

  private void selectSoleMatch() {
    ChartInterval interval = interactor.soleMatch();
    if (interval != null) {
      select(interval);
    }
  }

  private void requestClose() {
    closeRequestHandler.run();
  }
}
