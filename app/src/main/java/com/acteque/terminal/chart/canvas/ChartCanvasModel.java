package com.acteque.terminal.chart.canvas;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import java.util.List;
import javafx.beans.property.ReadOnlyLongProperty;
import javafx.beans.property.ReadOnlyLongWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;

/** Observable state shared by the chart canvas MVCI components. */
final class ChartCanvasModel {

  enum DragMode {
    NONE,
    ZOOM_DATE,
    ZOOM_PRICE,
    PAN,
  }

  record PriceRange(double min, double max) {
    double span() {
      return max - min;
    }

    PriceRange scale(double scale) {
      double midpoint = (min + max) / 2.0;
      double scaledSpan = span() * scale;
      return new PriceRange(midpoint - scaledSpan / 2.0, midpoint + scaledSpan / 2.0);
    }

    PriceRange translate(double priceDelta) {
      return new PriceRange(min + priceDelta, max + priceDelta);
    }
  }

  record VisibleWindow(List<PricePoint> points, int firstDataIndex) {}

  ChartInterval interval;
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

  ReadOnlyLongProperty revisionProperty() {
    return revision.getReadOnlyProperty();
  }

  ReadOnlyObjectProperty<PricePoint> displayedPricePointProperty() {
    return displayedPricePoint.getReadOnlyProperty();
  }

  void publish(PricePoint point) {
    displayedPricePoint.set(point);
    revision.set(revision.get() + 1);
  }
}
