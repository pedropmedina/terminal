package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.PricePoint;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart status line's MVCI components. */
final class ChartStatusLineModel {

  private final ReadOnlyObjectWrapper<PricePoint> pricePoint = new ReadOnlyObjectWrapper<>(this, "pricePoint");

  /** @return the currently displayed price point, or {@code null} */
  PricePoint getPricePoint() {
    return pricePoint.get();
  }

  /** @return the read-only displayed price-point property */
  ReadOnlyObjectProperty<PricePoint> pricePointProperty() {
    return pricePoint.getReadOnlyProperty();
  }

  /** @param value the point to display, or {@code null} to clear it */
  void setPricePoint(PricePoint value) {
    pricePoint.set(value);
  }
}
