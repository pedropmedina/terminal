package com.acteque.terminal.chart;

import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;

import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

/** Displays the chart's OHLCV status for the selected price point. */
final class ChartStatusLine extends HBox implements RefreshableView {

  private static final double LEFT_MARGIN = 12.0;
  private static final double BOTTOM_MARGIN = 38.0;
  private static final double SECTION_SPACING = 12.0;
  private static final Font FONT = Font.font("System", 14.0);

  private final String stockSymbol;
  private final ChartInterval interval;

  private PricePoint pricePoint;
  private Runnable instrumentClickHandler = () -> {};
  private Text ohlcv;

  ChartStatusLine(String stockSymbol, ChartInterval interval) {
    this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol");
    this.interval = Objects.requireNonNull(interval, "interval");

    refreshView();
    ChartReloadHooks.register(this);
  }

  void setPricePoint(PricePoint point) {
    pricePoint = Objects.requireNonNull(point, "point");
    ohlcv.setText(ohlcvText(point));
  }

  void clearPricePoint() {
    pricePoint = null;
    ohlcv.setText("");
  }

  void onInstrumentClick(Runnable callback) {
    instrumentClickHandler = Objects.requireNonNull(callback, "callback");
  }

  String text(PricePoint point) {
    Objects.requireNonNull(point, "point");
    return String.format("%s  %s   %s", stockSymbol, interval.displayName(), ohlcvText(point));
  }

  @Override
  public void refreshView() {
    Text symbol = new Text(stockSymbol);
    symbol.setFill(Color.rgb(26, 115, 232));
    symbol.setFont(FONT);

    StackPane symbolSection = createSection(symbol);
    symbolSection.setCursor(Cursor.HAND);
    symbolSection.setAccessibleText("Select symbol or instrument");
    symbolSection.setOnMouseClicked(ignored -> instrumentClickHandler.run());

    Text intervalLabel = new Text(interval.displayName());
    intervalLabel.setFill(Color.rgb(28, 32, 38));
    intervalLabel.setFont(FONT);
    StackPane intervalSection = createSection(intervalLabel);
    intervalSection.setAccessibleText("Interval " + interval.displayName());
    intervalSection.setMouseTransparent(true);

    ohlcv = new Text(pricePoint == null ? "" : ohlcvText(pricePoint));
    ohlcv.setFill(Color.rgb(28, 32, 38));
    ohlcv.setFont(FONT);
    StackPane ohlcvSection = createSection(ohlcv);
    ohlcvSection.setAccessibleText("Open, high, low, close, and volume");
    ohlcvSection.setMouseTransparent(true);

    setSpacing(SECTION_SPACING);
    setAlignment(Pos.CENTER_LEFT);
    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setPickOnBounds(false);
    StackPane.setAlignment(this, Pos.BOTTOM_LEFT);
    StackPane.setMargin(this, new Insets(0.0, 0.0, BOTTOM_MARGIN, LEFT_MARGIN));
    getChildren().setAll(symbolSection, intervalSection, ohlcvSection);
  }

  private static StackPane createSection(Text content) {
    StackPane section = new StackPane(content);
    section.setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    return section;
  }

  private String ohlcvText(PricePoint point) {
    return String.format(
      Locale.US,
      "O%,.2f  H%,.2f  L%,.2f  C%,.2f  Vol%,.2f M",
      point.open(),
      point.high(),
      point.low(),
      point.close(),
      point.volume() / 1_000_000.0
    );
  }
}
