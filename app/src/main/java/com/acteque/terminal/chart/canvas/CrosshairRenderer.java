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

  private static final double CALENDAR_BADGE_WIDTH = 120.0;
  private static final double INTRADAY_BADGE_WIDTH = 180.0;
  private static final double PRICE_TEXT_OFFSET = 10.0;
  private static final double DATE_TEXT_OFFSET = 10.0;

  CrosshairRenderer() {}

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
    graphics.setLineDashes(4.0, 4.0);
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
    graphics.fillText(priceText(price), chartRight + PRICE_TEXT_OFFSET, y);

    double dateBadgeWidth = dateText.contains(":") ? INTRADAY_BADGE_WIDTH : CALENDAR_BADGE_WIDTH;
    double dateBadgeLeft = Math.max(chartLeft, Math.min(chartRight - dateBadgeWidth, x - dateBadgeWidth / 2.0));
    graphics.setFill(style.badgeBackground());
    graphics.fillRect(dateBadgeLeft, chartBottom, dateBadgeWidth, style.badgeHeight());
    graphics.setFill(style.badgeForeground());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    graphics.fillText(dateText, dateBadgeLeft + dateBadgeWidth / 2.0, chartBottom + DATE_TEXT_OFFSET);
  }

  String priceText(double price) {
    return String.format(Locale.US, "%.2f", price);
  }

  String dateText(LocalDate date) {
    return ChartDateFormatter.crosshair(Objects.requireNonNull(date, "date cannot be null"));
  }

  String dateText(Instant timestamp) {
    return ChartDateFormatter.crosshair(Objects.requireNonNull(timestamp, "timestamp cannot be null"));
  }
}
