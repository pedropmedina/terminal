package com.acteque.terminal;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Renders the chart's crosshair and its price and date badges. */
final class PriceChartCrosshair {

  private static final double BADGE_HEIGHT = 24.0;
  private static final double DATE_BADGE_WIDTH = 120.0;
  private static final double PRICE_TEXT_OFFSET = 10.0;
  private static final double DATE_TEXT_OFFSET = 10.0;

  private final ChartInterval interval;

  PriceChartCrosshair(ChartInterval interval) {
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
    LocalDate date
  ) {
    Objects.requireNonNull(graphics, "graphics");
    Objects.requireNonNull(date, "date");

    graphics.save();
    graphics.setStroke(Color.rgb(120, 126, 136));
    graphics.setLineWidth(1.0);
    graphics.setLineDashes(4.0, 4.0);
    graphics.strokeLine(x, chartTop, x, chartBottom);
    graphics.strokeLine(chartLeft, y, chartRight, y);
    graphics.restore();

    Color badgeColor = Color.rgb(232, 234, 237);
    Color badgeTextColor = Color.rgb(40, 44, 52);

    double priceBadgeTop = y - BADGE_HEIGHT / 2.0;
    graphics.setFill(badgeColor);
    graphics.fillRect(chartRight, priceBadgeTop, canvasWidth - chartRight, BADGE_HEIGHT);
    graphics.setFill(badgeTextColor);
    graphics.setFont(Font.font("System", 12));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(priceText(price), chartRight + PRICE_TEXT_OFFSET, y);

    double dateBadgeLeft = Math.max(chartLeft, Math.min(chartRight - DATE_BADGE_WIDTH, x - DATE_BADGE_WIDTH / 2.0));
    graphics.setFill(badgeColor);
    graphics.fillRect(dateBadgeLeft, chartBottom, DATE_BADGE_WIDTH, BADGE_HEIGHT);
    graphics.setFill(badgeTextColor);
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
