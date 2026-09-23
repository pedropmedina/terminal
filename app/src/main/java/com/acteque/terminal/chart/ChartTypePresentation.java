package com.acteque.terminal.chart;

import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Objects;

/** Labels and icons used to present chart types in chart controls. */
public final class ChartTypePresentation {

  /** Prevents utility-class instantiation. */
  private ChartTypePresentation() {}

  /**
   * Returns the human-readable chart-type name.
   *
   * @param type the chart type to present
   * @return its display name
   */
  public static String displayName(ChartType type) {
    return switch (Objects.requireNonNull(type, "type cannot be null")) {
      case LINE -> "Line";
      case LINE_WITH_MARKERS -> "Line with markers";
      case STEP_LINE -> "Step line";
      case AREA -> "Area";
      case BAR -> "Bar";
      case CANDLESTICK -> "Candlestick";
    };
  }

  /**
   * Returns a concise behavioral description of a chart type.
   *
   * @param type the chart type to describe
   * @return its description
   */
  public static String description(ChartType type) {
    return switch (Objects.requireNonNull(type, "type cannot be null")) {
      case LINE -> "Connects closing prices with a continuous line.";
      case LINE_WITH_MARKERS -> "Marks each closing price and connects the points.";
      case STEP_LINE -> "Holds each closing price until the next interval.";
      case AREA -> "Fills the space beneath the closing-price line.";
      case BAR -> "Shows each interval's open, high, low, and close as an OHLC bar.";
      case CANDLESTICK -> "Shows each interval's open, high, low, and close.";
    };
  }

  /**
   * Returns the icon used to represent a chart type.
   *
   * @param type the chart type to represent
   * @return its Lucide icon
   */
  public static LucideIcons icon(ChartType type) {
    return switch (Objects.requireNonNull(type, "type cannot be null")) {
      case LINE, STEP_LINE -> LucideIcons.CHART_LINE;
      case LINE_WITH_MARKERS -> LucideIcons.CHART_NETWORK;
      case AREA -> LucideIcons.CHART_AREA;
      case BAR -> LucideIcons.CHART_BAR;
      case CANDLESTICK -> LucideIcons.CHART_CANDLESTICK;
    };
  }
}
