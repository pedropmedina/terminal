package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.PricePoint;
import java.util.Objects;

/** Applies status-line state transitions without depending on its layout. */
final class ChartStatusLineInteractor {

  private final ChartStatusLineModel model;

  /**
   * Creates an interactor for the supplied status-line model.
   *
   * @param model the {@link ChartStatusLineModel} receiving state transitions
   */
  ChartStatusLineInteractor(ChartStatusLineModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
  }

  /**
   * Sets the price point displayed by the status line.
   *
   * @param point the non-null {@link PricePoint} to display
   */
  void setPricePoint(PricePoint point) {
    model.setPricePoint(Objects.requireNonNull(point, "point cannot be null"));
  }

  /** Clears the displayed price point. */
  void clearPricePoint() {
    model.setPricePoint(null);
  }
}
