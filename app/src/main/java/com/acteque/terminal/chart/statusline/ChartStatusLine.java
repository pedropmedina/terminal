package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoSession;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executor;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;

/** Composes and exposes the chart status line's MVCI feature. */
public final class ChartStatusLine {

  private final ChartStatusLineInteractor interactor;
  private final ChartStatusLineViewBuilder viewBuilder;
  private final Runnable instrumentSelectionAction;
  private final Runnable intervalSelectionAction;

  public ChartStatusLine(
    String instrumentName,
    ChartInterval interval,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentSelectionAction,
    Runnable intervalSelectionAction
  ) {
    this(
      instrumentName,
      interval,
      tooltipsSuppressed,
      instrumentSelectionAction,
      intervalSelectionAction,
      LogoSession.NONE,
      Runnable::run
    );
  }

  public ChartStatusLine(
    String instrumentName,
    ChartInterval interval,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentSelectionAction,
    Runnable intervalSelectionAction,
    LogoSession logoSource,
    Executor uiExecutor
  ) {
    this(
      instrumentName,
      interval,
      new SimpleObjectProperty<>(Color.TRANSPARENT),
      new SimpleBooleanProperty(false),
      tooltipsSuppressed,
      instrumentSelectionAction,
      intervalSelectionAction,
      logoSource,
      uiExecutor
    );
  }

  public ChartStatusLine(
    String instrumentName,
    ChartInterval interval,
    ObservableValue<Color> identifierColor,
    ObservableBooleanValue identifierVisible,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentSelectionAction,
    Runnable intervalSelectionAction,
    LogoSession logoSource,
    Executor uiExecutor
  ) {
    this.instrumentSelectionAction = Objects.requireNonNull(
      instrumentSelectionAction,
      "instrumentSelectionAction cannot be null"
    );
    this.intervalSelectionAction = Objects.requireNonNull(
      intervalSelectionAction,
      "intervalSelectionAction cannot be null"
    );
    ChartStatusLineModel model = new ChartStatusLineModel();
    interactor = new ChartStatusLineInteractor(model, logoSource, uiExecutor);
    interactor.initialize(instrumentName, interval);
    viewBuilder = new ChartStatusLineViewBuilder(
      model,
      Objects.requireNonNull(identifierColor, "identifierColor cannot be null"),
      Objects.requireNonNull(identifierVisible, "identifierVisible cannot be null"),
      Objects.requireNonNull(tooltipsSuppressed, "tooltipsSuppressed cannot be null"),
      this::selectInstrument,
      this::selectInterval
    );
  }

  public Region getView() {
    return viewBuilder.build();
  }

  public void setPricePoint(PricePoint point) {
    interactor.setPricePoint(point);
  }

  public void clearPricePoint() {
    interactor.clearPricePoint();
  }

  public void setInstrumentName(String instrumentName) {
    interactor.setInstrumentName(instrumentName);
  }

  public void setInstrument(String instrumentName, Optional<InstrumentLogo> logo) {
    interactor.setInstrument(instrumentName, logo);
  }

  public void cancelLogoLoad() {
    interactor.cancelLogoLoad();
  }

  public void setInstrumentLogo(InstrumentLogo logo, Image image) {
    interactor.setInstrumentLogo(logo, image);
  }

  public void setInterval(ChartInterval interval) {
    interactor.setInterval(interval);
  }

  private void selectInstrument() {
    instrumentSelectionAction.run();
  }

  private void selectInterval() {
    intervalSelectionAction.run();
  }
}
