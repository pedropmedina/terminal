package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.ui.dialog.Dialog;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javafx.beans.value.ObservableBooleanValue;

/** Composes and exposes the chart interval-selection MVCI feature. */
public final class ChartWorkspaceIntervalSelection {

  private final ChartWorkspaceIntervalSelectionModel model;
  private final ChartWorkspaceIntervalSelectionInteractor interactor;
  private final ChartWorkspaceIntervalSelectionViewBuilder viewBuilder;

  /**
   * Creates and connects the interval-selection MVCI components.
   *
   * @param currentInterval the interval initially displayed as selected
   */
  public ChartWorkspaceIntervalSelection(ChartInterval currentInterval) {
    model = new ChartWorkspaceIntervalSelectionModel();
    interactor = new ChartWorkspaceIntervalSelectionInteractor(model);
    interactor.initialize(currentInterval);

    viewBuilder = new ChartWorkspaceIntervalSelectionViewBuilder(
      model,
      interactor::setQuery,
      interactor::addInterval,
      interactor::selectInterval,
      interactor::selectSoleMatch,
      interactor::requestClose
    );
  }

  /**
   * Returns the composed interval-selection dialog.
   *
   * @return the interval-selection dialog
   */
  public Dialog getView() {
    return viewBuilder.build();
  }

  /**
   * Returns the observable dialog state.
   *
   * @return the read-only open state
   */
  public ObservableBooleanValue openProperty() {
    return model.openProperty();
  }

  /**
   * Updates the interval displayed as selected without notifying the selection callback.
   *
   * @param interval the active chart interval
   */
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
  }

  /** Opens the interval-selection dialog. */
  public void show() {
    interactor.show();
  }

  /** Closes the interval-selection dialog. */
  public void close() {
    interactor.close();
  }

  /**
   * Registers the action invoked after an interval is selected.
   *
   * @param callback the selected-interval callback
   */
  public void onIntervalSelected(Consumer<ChartInterval> callback) {
    interactor.onIntervalSelected(callback);
  }

  /**
   * Registers the action invoked when the dialog requests external closure.
   *
   * @param callback the close-request callback
   */
  public void onRequestClose(Runnable callback) {
    interactor.onRequestClose(callback);
  }
}
