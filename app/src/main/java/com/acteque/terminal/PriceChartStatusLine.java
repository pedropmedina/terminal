package com.acteque.terminal;

import java.util.Locale;
import java.util.Objects;
import javafx.geometry.VPos;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/** Renders the chart's OHLCV status line for a selected price point. */
final class PriceChartStatusLine {

  private static final double LEFT_PADDING = 12.0;
  private static final double BOTTOM_OFFSET = 14.0;

  private final String stockSymbol;
  private final ChartInterval interval;

  PriceChartStatusLine(String stockSymbol, ChartInterval interval) {
    this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol");
    this.interval = Objects.requireNonNull(interval, "interval");
  }

  void draw(GraphicsContext graphics, double chartLeft, double chartBottom, PricePoint point) {
    Objects.requireNonNull(graphics, "graphics");
    Objects.requireNonNull(point, "point");

    graphics.setFill(Color.rgb(28, 32, 38));
    graphics.setFont(Font.font("System", 14));
    graphics.setTextAlign(TextAlignment.LEFT);
    graphics.setTextBaseline(VPos.CENTER);
    graphics.fillText(text(point), chartLeft + LEFT_PADDING, chartBottom - BOTTOM_OFFSET);
  }

  String text(PricePoint point) {
    Objects.requireNonNull(point, "point");
    return String.format(
      Locale.US,
      "%s  %s   O%,.2f  H%,.2f  L%,.2f  C%,.2f  Vol%,.2f M",
      stockSymbol,
      interval.displayName(),
      point.open(),
      point.high(),
      point.low(),
      point.close(),
      point.volume() / 1_000_000.0
    );
  }
}
