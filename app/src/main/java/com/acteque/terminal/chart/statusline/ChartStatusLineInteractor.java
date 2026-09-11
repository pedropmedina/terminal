package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketdata.InstrumentLogo;
import java.util.Objects;
import javafx.scene.image.Image;

/** Applies status-line state transitions without depending on its layout. */
final class ChartStatusLineInteractor {

  private final ChartStatusLineModel model;

  ChartStatusLineInteractor(ChartStatusLineModel model) {
    this.model = Objects.requireNonNull(model, "model");
  }

  void initialize(String instrumentName, ChartInterval interval) {
    model.setInstrumentName(Objects.requireNonNull(instrumentName, "instrumentName"));
    model.setInterval(Objects.requireNonNull(interval, "interval"));
  }

  void setPricePoint(PricePoint point) {
    model.setPricePoint(Objects.requireNonNull(point, "point"));
  }

  void clearPricePoint() {
    model.setPricePoint(null);
  }

  void setInstrumentName(String instrumentName) {
    model.setInstrumentName(Objects.requireNonNull(instrumentName, "instrumentName"));
    model.setLogoState(null);
  }

  void setInstrumentLogo(InstrumentLogo logo, Image image) {
    Objects.requireNonNull(logo, "logo");
    Objects.requireNonNull(image, "image");
    if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
      model.setLogoState(null);
      return;
    }
    model.setLogoState(new ChartStatusLineModel.LogoState(logo, image));
  }

  void setInterval(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval"));
  }
}
