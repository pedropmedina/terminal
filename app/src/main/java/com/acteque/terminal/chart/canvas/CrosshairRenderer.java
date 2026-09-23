package com.acteque.terminal.chart.canvas;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

/** Renders the chart's crosshair and its price and date badges. */
final class CrosshairRenderer {

  /** Creates a stateless crosshair renderer. */
  CrosshairRenderer() {}

  /**
   * Draws crosshair lines and axis badges.
   *
   * @param graphics the target graphics context
   * @param chartLeft the chart area's left edge
   * @param chartTop the chart area's top edge
   * @param chartRight the chart area's right edge
   * @param chartBottom the chart area's bottom edge
   * @param canvasWidth the complete canvas width
   * @param x the crosshair x coordinate
   * @param y the crosshair y coordinate
   * @param price the price represented by {@code y}
   * @param dateText the date or time label
   * @param style the CSS-resolved drawing style
   */
  void draw(
    GraphicsContext graphics,
    double chartLeft,
    double chartTop,
    double chartRight,
    double chartBottom,
    double canvasWidth,
    double x,
    double y,
    double price,
    String dateText,
    RenderStyle style
  ) {
    Objects.requireNonNull(graphics, "graphics cannot be null");
    Objects.requireNonNull(dateText, "dateText cannot be null");
    Objects.requireNonNull(style, "style cannot be null");

    graphics.save();
    graphics.setStroke(style.crosshair());
    graphics.setLineWidth(style.gridLineWidth());
    graphics.setLineDashes(style.crosshairDashLength(), style.crosshairDashLength());
    graphics.strokeLine(x, chartTop, x, chartBottom);
    graphics.strokeLine(chartLeft, y, chartRight, y);
    graphics.restore();

    double priceBadgeTop = y - style.badgeHeight() / 2.0;
    graphics.setFill(style.badgeBackground());
    graphics.fillRect(chartRight, priceBadgeTop, canvasWidth - chartRight, style.badgeHeight());
    graphics.setFill(style.badgeForeground());
    graphics.setFont(style.badgeFont());
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(priceText(price), chartRight + style.badgeTextOffset(), y);

    double dateBadgeWidth = dateText.contains(":") ? style.intradayBadgeWidth() : style.calendarBadgeWidth();
    double dateBadgeLeft = Math.max(chartLeft, Math.min(chartRight - dateBadgeWidth, x - dateBadgeWidth / 2.0));
    graphics.setFill(style.badgeBackground());
    graphics.fillRect(dateBadgeLeft, chartBottom, dateBadgeWidth, style.badgeHeight());
    graphics.setFill(style.badgeForeground());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    graphics.fillText(dateText, dateBadgeLeft + dateBadgeWidth / 2.0, chartBottom + style.badgeTextOffset());
  }

  /**
   * @param price the price to format
   * @return a two-decimal price label
   */
  String priceText(double price) {
    return String.format(Locale.US, "%.2f", price);
  }

  /**
   * @param date the date to format
   * @return a calendar crosshair label
   */
  String dateText(LocalDate date) {
    return ChartDateFormatter.crosshair(Objects.requireNonNull(date, "date cannot be null"));
  }

  /**
   * @param timestamp the instant to format
   * @return a New York intraday crosshair label
   */
  String dateText(Instant timestamp) {
    return ChartDateFormatter.crosshair(Objects.requireNonNull(timestamp, "timestamp cannot be null"));
  }
}
