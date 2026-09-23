package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import com.acteque.terminal.chart.PricePoint;
import java.util.List;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart canvas MVCI components. */
final class ChartCanvasModel {

  /** Pointer-drag behaviors supported by the canvas. */
  enum DragMode {
    NONE,
    ZOOM_DATE,
    ZOOM_PRICE,
    PAN,
  }

  /**
   * Immutable displayed price range.
   *
   * @param min the lower displayed price
   * @param max the upper displayed price
   */
  record PriceRange(double min, double max) {
    /** @return the distance between the maximum and minimum price */
    double span() {
      return max - min;
    }

    /**
     * @param scale the multiplier applied around the range midpoint
     * @return the scaled range
     */
    PriceRange scale(double scale) {
      double midpoint = (min + max) / 2.0;
      double scaledSpan = span() * scale;
      return new PriceRange(midpoint - scaledSpan / 2.0, midpoint + scaledSpan / 2.0);
    }

    /**
     * @param priceDelta the signed price displacement
     * @return the translated range
     */
    PriceRange translate(double priceDelta) {
      return new PriceRange(min + priceDelta, max + priceDelta);
    }
  }

  /**
   * Visible data slice and its origin in the complete series.
   *
   * @param points the points visible in the viewport
   * @param firstDataIndex the source-series index of the first visible point
   */
  record VisibleWindow(List<PricePoint> points, int firstDataIndex) {}

  ChartInterval interval;
  ChartType chartType = ChartType.LINE;
  List<PricePoint> pricePoints = List.of();
  int visiblePricePointCount;
  int visiblePricePointOffset;
  double yZoomScale = 1.0;
  PriceRange lockedPriceRange;
  Integer hoveredVisiblePointIndex;
  Double crosshairX;
  Double crosshairY;
  boolean priceAxisHovered;
  boolean autoscaleButtonPressed;
  DragMode dragMode = DragMode.NONE;

  double xZoomDragStart;
  int xZoomDragStartVisiblePricePointCount;
  double xPanDragStart;
  int xPanDragStartOffset;
  double yPanDragStart;
  PriceRange yPanDragStartPriceRange;
  double yZoomDragStart;
  double yZoomDragStartZoomScale;
  double yZoomDragDefaultPriceSpan;
  PriceRange yZoomDragStartPriceRange;

  private final ReadOnlyLongWrapper revision = new ReadOnlyLongWrapper(this, "revision");
  private final ReadOnlyObjectWrapper<PricePoint> displayedPricePoint = new ReadOnlyObjectWrapper<>(
    this,
    "displayedPricePoint"
  );

  /** @return the revision used to trigger canvas redraws */
  ReadOnlyLongProperty revisionProperty() {
    return revision.getReadOnlyProperty();
  }

  /** @return the point currently shown in the status line */
  ReadOnlyObjectProperty<PricePoint> displayedPricePointProperty() {
    return displayedPricePoint.getReadOnlyProperty();
  }

  /**
   * Publishes a displayed point and increments the redraw revision.
   *
   * @param point the displayed point, or {@code null} when none is available
   */
  void publish(PricePoint point) {
    displayedPricePoint.set(point);
    revision.set(revision.get() + 1);
  }
}
