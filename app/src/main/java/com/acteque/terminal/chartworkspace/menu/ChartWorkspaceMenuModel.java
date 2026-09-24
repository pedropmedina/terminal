package com.acteque.terminal.chartworkspace.menu;

import com.acteque.terminal.chart.Chart;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartType;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ReadOnlyListProperty;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

/** Observable state shared by the workspace menu's MVCI components. */
final class ChartWorkspaceMenuModel {

  /** Identifies an action that can be presented by the workspace menu. */
  enum Item {
    INSTRUMENT("Symbol or instrument"),
    INTERVAL("Interval"),
    CHART_TYPE("Chart type"),
    SPLIT("Split chart"),
    CLOSE("Close chart");

    private final String description;

    /**
     * Creates a menu-item identifier with assistive text.
     *
     * @param description the action description exposed to assistive technologies
     */
    Item(String description) {
      this.description = description;
    }

    /**
     * Returns the action description exposed to assistive technologies.
     *
     * @return the accessible action description
     */
    String description() {
      return description;
    }
  }

  private final ReadOnlyListWrapper<Item> items = new ReadOnlyListWrapper<>(
    this,
    "items",
    FXCollections.observableArrayList()
  );
  private final ReadOnlyObjectWrapper<Image> instrumentLogoImage = new ReadOnlyObjectWrapper<>(
    this,
    "instrumentLogoImage"
  );
  private final ReadOnlyStringWrapper instrumentSymbol = new ReadOnlyStringWrapper(this, "instrumentSymbol", "");
  private final ReadOnlyObjectWrapper<ChartInterval> interval = new ReadOnlyObjectWrapper<>(this, "interval");
  private final ReadOnlyObjectWrapper<ChartType> chartType = new ReadOnlyObjectWrapper<>(
    this,
    "chartType",
    ChartType.LINE
  );
  private final ReadOnlyObjectWrapper<Color> identifierColor = new ReadOnlyObjectWrapper<>(
    this,
    "identifierColor",
    Color.TRANSPARENT
  );

  /**
   * Returns the menu items in display order.
   *
   * @return an immutable snapshot of the displayed items
   */
  List<Item> getItems() {
    return List.copyOf(items);
  }

  /**
   * Returns the observable menu-item list.
   *
   * @return the read-only item-list property
   */
  ReadOnlyListProperty<Item> itemsProperty() {
    return items.getReadOnlyProperty();
  }

  /**
   * Replaces the menu items in display order.
   *
   * @param values the menu items to display
   */
  void setItems(List<Item> values) {
    items.setAll(Objects.requireNonNull(values, "values cannot be null"));
  }

  /**
   * Returns the active chart's instrument symbol.
   *
   * @return the active instrument symbol
   */
  String getInstrumentSymbol() {
    return instrumentSymbol.get();
  }

  /**
   * Returns the observable active instrument symbol.
   *
   * @return the read-only instrument-symbol property
   */
  ReadOnlyStringProperty instrumentSymbolProperty() {
    return instrumentSymbol.getReadOnlyProperty();
  }

  /**
   * Returns the active chart's loaded instrument logo.
   *
   * @return the logo image, or null when no logo is loaded
   */
  Image getInstrumentLogoImage() {
    return instrumentLogoImage.get();
  }

  /**
   * Returns the observable active instrument logo.
   *
   * @return the read-only instrument-logo property
   */
  ReadOnlyObjectProperty<Image> instrumentLogoImageProperty() {
    return instrumentLogoImage.getReadOnlyProperty();
  }

  /**
   * Returns the active chart's interval.
   *
   * @return the active chart interval
   */
  ChartInterval getInterval() {
    return interval.get();
  }

  /**
   * Returns the observable active chart interval.
   *
   * @return the read-only interval property
   */
  ReadOnlyObjectProperty<ChartInterval> intervalProperty() {
    return interval.getReadOnlyProperty();
  }

  /**
   * Returns the active chart's presentation type.
   *
   * @return the active chart type
   */
  ChartType getChartType() {
    return chartType.get();
  }

  /**
   * Returns the observable active chart type.
   *
   * @return the read-only chart-type property
   */
  ReadOnlyObjectProperty<ChartType> chartTypeProperty() {
    return chartType.getReadOnlyProperty();
  }

  /**
   * Returns the active chart's workspace identifier color.
   *
   * @return the chart identifier color
   */
  Color getIdentifierColor() {
    return identifierColor.get();
  }

  /**
   * Returns the observable active-chart identifier color.
   *
   * @return the read-only identifier-color property
   */
  ReadOnlyObjectProperty<Color> identifierColorProperty() {
    return identifierColor.getReadOnlyProperty();
  }

  /**
   * Binds chart-derived menu state to a new active chart.
   *
   * @param chart the chart whose state the menu should represent
   */
  void bind(Chart chart) {
    Chart activeChart = Objects.requireNonNull(chart, "chart cannot be null");
    unbind();
    instrumentLogoImage.bind(activeChart.instrumentLogoImageProperty());
    instrumentSymbol.bind(activeChart.symbolProperty());
    interval.bind(activeChart.intervalProperty());
    chartType.bind(activeChart.chartTypeProperty());
    identifierColor.bind(activeChart.identifierColorProperty());
  }

  /** Releases every property binding to the active chart. */
  void unbind() {
    instrumentLogoImage.unbind();
    instrumentSymbol.unbind();
    interval.unbind();
    chartType.unbind();
    identifierColor.unbind();
  }
}
