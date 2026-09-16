package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import java.util.Objects;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.scene.image.Image;

/** Observable state shared by the chart status line's MVCI components. */
final class ChartStatusLineModel {

  record LogoState(InstrumentLogo logo, Image image) {
    LogoState {
      Objects.requireNonNull(logo, "logo");
      Objects.requireNonNull(image, "image");
    }
  }

  private final ReadOnlyStringWrapper instrumentName = new ReadOnlyStringWrapper(this, "instrumentName", "");
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<PricePoint> pricePoint = new ReadOnlyObjectWrapper<>(this, "pricePoint");
  private final ReadOnlyObjectWrapper<LogoState> logoState = new ReadOnlyObjectWrapper<>(this, "logoState");

  String getInstrumentName() {
    return instrumentName.get();
  }

  ReadOnlyStringProperty instrumentNameProperty() {
    return instrumentName.getReadOnlyProperty();
  }

  void setInstrumentName(String value) {
    instrumentName.set(value);
  }

  ChartInterval getInterval() {
    return interval.get();
  }

  ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return interval.getReadOnlyProperty();
  }

  void setInterval(ChartInterval value) {
    interval.set(value);
  }

  PricePoint getPricePoint() {
    return pricePoint.get();
  }

  ReadOnlyObjectProperty<PricePoint> pricePointProperty() {
    return pricePoint.getReadOnlyProperty();
  }

  void setPricePoint(PricePoint value) {
    pricePoint.set(value);
  }

  LogoState getLogoState() {
    return logoState.get();
  }

  ReadOnlyObjectProperty<LogoState> logoStateProperty() {
    return logoState.getReadOnlyProperty();
  }

  void setLogoState(LogoState value) {
    logoState.set(value);
  }
}
