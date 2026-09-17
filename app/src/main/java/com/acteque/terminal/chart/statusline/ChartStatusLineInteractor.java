package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoSession;
import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import javafx.scene.image.Image;

/** Applies status-line state transitions without depending on its layout. */
final class ChartStatusLineInteractor {

  private static final System.Logger LOGGER = System.getLogger(ChartStatusLineInteractor.class.getName());
  private final ChartStatusLineModel model;
  private final LogoSession logoSource;
  private final Executor uiExecutor;
  private long logoGeneration;

  ChartStatusLineInteractor(ChartStatusLineModel model) {
    this(model, LogoSession.NONE, Runnable::run);
  }

  ChartStatusLineInteractor(ChartStatusLineModel model, LogoSession logoSource, Executor uiExecutor) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.logoSource = Objects.requireNonNull(logoSource, "logoSource cannot be null");
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor cannot be null");
  }

  void initialize(String instrumentName, ChartInterval interval) {
    model.setInstrumentName(Objects.requireNonNull(instrumentName, "instrumentName cannot be null"));
    model.setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }

  void setPricePoint(PricePoint point) {
    model.setPricePoint(Objects.requireNonNull(point, "point cannot be null"));
  }

  void clearPricePoint() {
    model.setPricePoint(null);
  }

  void setInstrumentName(String instrumentName) {
    cancelLogoLoad();
    model.setInstrumentName(Objects.requireNonNull(instrumentName, "instrumentName cannot be null"));
    model.setLogoState(null);
  }

  void setInstrument(String instrumentName, Optional<InstrumentLogo> logo) {
    setInstrumentName(instrumentName);
    loadInstrumentLogo(Objects.requireNonNull(logo, "logo cannot be null"));
  }

  void cancelLogoLoad() {
    ++logoGeneration;
    logoSource.cancel();
  }

  private void loadInstrumentLogo(Optional<InstrumentLogo> logo) {
    long requestedGeneration = ++logoGeneration;
    logo.ifPresent(reference ->
      logoSource
        .load(reference)
        .thenApply(bytes -> bytes.map(ChartStatusLineInteractor::decodeLogo))
        .whenComplete((image, failure) ->
          uiExecutor.execute(() -> {
            if (requestedGeneration != logoGeneration) {
              return;
            }
            if (failure != null) {
              LOGGER.log(System.Logger.Level.WARNING, "Could not load instrument logo; keeping fallback icon", failure);
            } else {
              image.ifPresent(value -> setInstrumentLogo(reference, value));
            }
          })
        )
    );
  }

  private static Image decodeLogo(byte[] data) {
    Image image = new Image(new ByteArrayInputStream(data), 64, 64, true, true);
    if (image.isError()) {
      throw new IllegalArgumentException("Logo image could not be decoded", image.getException());
    }
    return image;
  }

  void setInstrumentLogo(InstrumentLogo logo, Image image) {
    Objects.requireNonNull(logo, "logo cannot be null");
    Objects.requireNonNull(image, "image cannot be null");
    if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
      model.setLogoState(null);
      return;
    }
    model.setLogoState(new ChartStatusLineModel.LogoState(logo, image));
  }

  void setInterval(ChartInterval interval) {
    model.setInterval(Objects.requireNonNull(interval, "interval cannot be null"));
  }
}
