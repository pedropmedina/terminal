package com.acteque.terminal.chart;

import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import java.util.Locale;
import java.util.Objects;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Displays the chart's OHLCV status for the selected price point. */
final class ChartStatusLine extends HBox implements RefreshableView {

  private static final double LEFT_MARGIN = 12.0;
  private static final double BOTTOM_MARGIN = 38.0;
  private String stockSymbol;
  private ChartInterval interval;

  private PricePoint pricePoint;
  private Runnable instrumentClickHandler = () -> {};
  private Runnable intervalClickHandler = () -> {};
  private Button symbolSection;
  private Button intervalSection;
  private Label ohlcv;

  ChartStatusLine(String stockSymbol, ChartInterval interval) {
    this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol");
    this.interval = Objects.requireNonNull(interval, "interval");
    getStyleClass().add("chart-status-line");

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

  void onIntervalClick(Runnable callback) {
    intervalClickHandler = Objects.requireNonNull(callback, "callback");
  }

  void setStockSymbol(String stockSymbol) {
    this.stockSymbol = Objects.requireNonNull(stockSymbol, "stockSymbol");
    symbolSection.setText(stockSymbol);
  }

  void setInterval(ChartInterval interval) {
    this.interval = Objects.requireNonNull(interval, "interval");
    intervalSection.setText(interval.displayName());
    intervalSection.setAccessibleText("Select interval, currently " + interval.displayName());
  }

  String text(PricePoint point) {
    Objects.requireNonNull(point, "point");
    return String.format("%s  %s   %s", stockSymbol, interval.displayName(), ohlcvText(point));
  }

  @Override
  public void refreshView() {
    symbolSection = new Button(stockSymbol, Variant.GHOST, Size.DEFAULT);
    symbolSection.getStyleClass().add("chart-symbol-button");
    symbolSection.setAccessibleText("Select symbol or instrument");
    symbolSection.setOnAction(ignored -> instrumentClickHandler.run());

    intervalSection = new Button(interval.displayName(), Variant.GHOST, Size.DEFAULT);
    intervalSection.getStyleClass().add("chart-interval-button");
    intervalSection.setAccessibleText("Select interval, currently " + interval.displayName());
    intervalSection.setOnAction(ignored -> intervalClickHandler.run());

    ohlcv = new Label(pricePoint == null ? "" : ohlcvText(pricePoint));
    ohlcv.getStyleClass().add("chart-status-label");
    ohlcv.setAccessibleText("Open, high, low, close, and volume");
    ohlcv.setMouseTransparent(true);

    setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    setPickOnBounds(false);
    StackPane.setAlignment(this, Pos.BOTTOM_LEFT);
    StackPane.setMargin(this, new Insets(0.0, 0.0, BOTTOM_MARGIN, LEFT_MARGIN));
    getChildren().setAll(symbolSection, intervalSection, ohlcv);
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
