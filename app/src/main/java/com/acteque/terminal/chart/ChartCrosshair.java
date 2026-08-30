package com.acteque.terminal.chart;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.text.TextAlignment;

/** Renders the chart's crosshair and its price and date badges. */
final class ChartCrosshair {

  private static final double DATE_BADGE_WIDTH = 120.0;
  private static final double PRICE_TEXT_OFFSET = 10.0;
  private static final double DATE_TEXT_OFFSET = 10.0;

  private final ChartInterval interval;

  ChartCrosshair(ChartInterval interval) {
    this.interval = Objects.requireNonNull(interval, "interval");
  }

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
    LocalDate date,
    ChartRenderStyle style
  ) {
    Objects.requireNonNull(graphics, "graphics");
    Objects.requireNonNull(date, "date");
    Objects.requireNonNull(style, "style");

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

    double dateBadgeLeft = Math.max(chartLeft, Math.min(chartRight - DATE_BADGE_WIDTH, x - DATE_BADGE_WIDTH / 2.0));
    graphics.setFill(style.badgeBackground());
    graphics.fillRect(dateBadgeLeft, chartBottom, DATE_BADGE_WIDTH, style.badgeHeight());
    graphics.setFill(style.badgeForeground());
    graphics.setTextAlign(TextAlignment.CENTER);
    graphics.setTextBaseline(VPos.TOP);
    graphics.fillText(dateText(date), dateBadgeLeft + DATE_BADGE_WIDTH / 2.0, chartBottom + DATE_TEXT_OFFSET);
  }

  String priceText(double price) {
    return String.format(Locale.US, "%.2f", price);
  }

  String dateText(LocalDate date) {
    return interval.formatCrosshair(Objects.requireNonNull(date, "date"));
  }
}
