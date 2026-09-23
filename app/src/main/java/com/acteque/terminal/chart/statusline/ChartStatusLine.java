package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.PricePoint;
import javafx.scene.layout.Region;

/** Composes and exposes the chart status line's MVCI feature. */
public final class ChartStatusLine {

  private final ChartStatusLineInteractor interactor;
  private final ChartStatusLineViewBuilder viewBuilder;

  /** Creates a passive chart status line for OHLCV metadata. */
  public ChartStatusLine() {
    ChartStatusLineModel model = new ChartStatusLineModel();
    interactor = new ChartStatusLineInteractor(model);
    viewBuilder = new ChartStatusLineViewBuilder(model);
  }

  /**
   * Returns the status-line view.
   *
   * @return the reusable {@link Region} containing OHLCV metadata
   */
  public Region getView() {
    return viewBuilder.build();
  }

  /**
   * Displays the supplied price point.
   *
   * @param point the non-null {@link PricePoint} to display
   */
  public void setPricePoint(PricePoint point) {
    interactor.setPricePoint(point);
  }

  /** Clears the displayed price point. */
  public void clearPricePoint() {
    interactor.clearPricePoint();
  }
}
