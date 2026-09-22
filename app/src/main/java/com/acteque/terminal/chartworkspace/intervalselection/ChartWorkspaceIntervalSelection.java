package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.ui.dialog.Dialog;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the chart interval-selection MVCI feature. */
public final class ChartWorkspaceIntervalSelection {

  private final ChartWorkspaceIntervalSelectionInteractor interactor;
  private final ChartWorkspaceIntervalSelectionViewBuilder viewBuilder;
  private Consumer<ChartInterval> intervalSelectedHandler = ignored -> {};
  private Runnable closeRequestHandler = () -> {};

  public ChartWorkspaceIntervalSelection(ChartInterval currentInterval) {
    ChartWorkspaceIntervalSelectionModel model = new ChartWorkspaceIntervalSelectionModel();
    interactor = new ChartWorkspaceIntervalSelectionInteractor(model);
    interactor.initialize(currentInterval);
    viewBuilder = new ChartWorkspaceIntervalSelectionViewBuilder(
      model,
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

  /**
   * Updates which visible intervals the active chart can request.
   *
   * @param availability the active chart's interval support check
   */
  public void setAvailability(Predicate<ChartInterval> availability) {
    interactor.setAvailability(availability);
    viewBuilder.setAvailability(availability);
  }

  public ObservableBooleanValue openProperty() {
    return interactor.openProperty();
  }

  public void show() {
    interactor.show();
  }

  public void close() {
    interactor.close();
  }

  public void onIntervalSelected(Consumer<ChartInterval> callback) {
    intervalSelectedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  public void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  private void select(ChartInterval interval) {
    if (!interactor.isAvailable(interval)) {
      return;
    }
    interactor.select(interval);
    interactor.close();
    intervalSelectedHandler.accept(interval);
  }

  private void selectSoleMatch() {
    ChartInterval interval = interactor.soleMatch();
    if (interval != null) {
      select(interval);
    }
  }

  private void requestClose() {
    interactor.close();
    closeRequestHandler.run();
  }
}
