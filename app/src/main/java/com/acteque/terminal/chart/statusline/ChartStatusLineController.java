package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketdata.InstrumentLogo;
import java.util.Objects;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.image.Image;
import javafx.scene.layout.Region;

/** Composes and exposes the chart status line's MVCI feature. */
public final class ChartStatusLineController {

  private final ChartStatusLineInteractor interactor;
  private final ChartStatusLineViewBuilder viewBuilder;
  private final Runnable instrumentSelectionAction;
  private final Runnable intervalSelectionAction;

  public ChartStatusLineController(
    String instrumentName,
    ChartInterval interval,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentSelectionAction,
    Runnable intervalSelectionAction
  ) {
    this.instrumentSelectionAction = Objects.requireNonNull(instrumentSelectionAction, "instrumentSelectionAction");
    this.intervalSelectionAction = Objects.requireNonNull(intervalSelectionAction, "intervalSelectionAction");
    ChartStatusLineModel model = new ChartStatusLineModel();
    interactor = new ChartStatusLineInteractor(model);
    interactor.initialize(instrumentName, interval);
    viewBuilder = new ChartStatusLineViewBuilder(
      model,
      Objects.requireNonNull(tooltipsSuppressed, "tooltipsSuppressed"),
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
