package com.acteque.terminal.chart;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Observable state shared by the chart's MVCI components. */
final class ChartModel {

  private final ReadOnlyBooleanWrapper instrumentSearchOpen = new ReadOnlyBooleanWrapper(this, "instrumentSearchOpen");
  private final ReadOnlyBooleanWrapper intervalSelectionOpen = new ReadOnlyBooleanWrapper(
    this,
    "intervalSelectionOpen"
  );
  private final ReadOnlyBooleanWrapper identifierVisible = new ReadOnlyBooleanWrapper(this, "identifierVisible");
  private final ReadOnlyObjectWrapper<Color> identifierColor = new ReadOnlyObjectWrapper<>(
    this,
    "identifierColor",
    Color.TRANSPARENT
  );
  private final ReadOnlyObjectWrapper<Image> instrumentLogoImage = new ReadOnlyObjectWrapper<>(
    this,
    "instrumentLogoImage"
  );
  private final ReadOnlyObjectWrapper<String> symbol = new ReadOnlyObjectWrapper<>(this, "symbol");
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(this, "chartType");
  private final ReadOnlyStringWrapper loadError = new ReadOnlyStringWrapper(this, "loadError");
  private final BooleanBinding modalOpen = instrumentSearchOpen.or(intervalSelectionOpen);

  /** @return true when instrument search is open */
  boolean isInstrumentSearchOpen() {
    return instrumentSearchOpen.get();
  }

  /** @return the read-only instrument-search state */
  ReadOnlyBooleanProperty instrumentSearchOpenProperty() {
    return instrumentSearchOpen.getReadOnlyProperty();
  }

  /** @param value true to mark instrument search open */
  void setInstrumentSearchOpen(boolean value) {
    instrumentSearchOpen.set(value);
  }

  /** @return true when interval selection is open */
  boolean isIntervalSelectionOpen() {
    return intervalSelectionOpen.get();
  }

  /** @return the read-only interval-selection state */
  ReadOnlyBooleanProperty intervalSelectionOpenProperty() {
    return intervalSelectionOpen.getReadOnlyProperty();
  }

  /** @param value true to mark interval selection open */
  void setIntervalSelectionOpen(boolean value) {
    intervalSelectionOpen.set(value);
  }

  /** @return the read-only identifier visibility */
  ReadOnlyBooleanProperty identifierVisibleProperty() {
    return identifierVisible.getReadOnlyProperty();
  }

  /** @param value true to display the workspace identifier */
  void setIdentifierVisible(boolean value) {
    identifierVisible.set(value);
  }

  /** @return the read-only identifier color */
  ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return identifierColor.getReadOnlyProperty();
  }

  /** @param value the workspace identifier color */
  void setIdentifierColor(Color value) {
    identifierColor.set(value);
  }

  /**
   * Returns the decoded logo for the selected instrument.
   *
   * @return the loaded {@link Image}, or {@code null} when the symbol fallback should be used
   */
  Image getInstrumentLogoImage() {
    return instrumentLogoImage.get();
  }

  /**
   * Returns the observable decoded instrument logo.
   *
   * @return the read-only logo image property
   */
  ReadOnlyObjectProperty<Image> instrumentLogoImageProperty() {
    return instrumentLogoImage.getReadOnlyProperty();
  }

  /**
   * Publishes the decoded instrument logo.
   *
   * @param value the loaded {@link Image}, or {@code null} to use the symbol fallback
   */
  void setInstrumentLogoImage(Image value) {
    instrumentLogoImage.set(value);
  }

  /** @return true when any chart modal is open */
  boolean isModalOpen() {
    return modalOpen.get();
  }

  /** @return the combined observable modal state */
  ObservableBooleanValue modalOpenProperty() {
    return modalOpen;
  }

  /** @return the selected interval */
  ChartInterval getInterval() {
    return interval.get();
  }

  /** @return the read-only selected interval */
  ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return interval.getReadOnlyProperty();
  }

  /** @param value the selected interval */
  void setInterval(ChartInterval value) {
    interval.set(value);
  }

  /** @return the selected chart type */
  ChartType getChartType() {
    return chartType.get();
  }

  /** @return the read-only selected chart type */
  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  /** @param value the selected chart type */
  void setChartType(ChartType value) {
    chartType.set(value);
  }

  /** @return the selected instrument symbol */
  String getSymbol() {
    return symbol.get();
  }

  /** @return the read-only selected instrument symbol */
  ReadOnlyObjectProperty<String> symbolProperty() {
    return symbol.getReadOnlyProperty();
  }

  /** @param value the selected instrument symbol */
  void setSymbol(String value) {
    symbol.set(value);
  }

  /** @return the read-only load error, whose value may be {@code null} */
  ReadOnlyStringProperty loadErrorProperty() {
    return loadError.getReadOnlyProperty();
  }

  /** @param value the current load error, or {@code null} to clear it */
  void setLoadError(String value) {
    loadError.set(value);
  }
}
