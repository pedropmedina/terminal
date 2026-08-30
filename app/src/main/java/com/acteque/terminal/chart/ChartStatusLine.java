package com.acteque.terminal.chart;

import java.util.Locale;
import java.util.Objects;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Displays the chart's OHLCV status for the selected price point. */
final class ChartStatusLine extends HBox implements RefreshableView {

  private static final double LEFT_MARGIN = 12.0;
  private static final double BOTTOM_MARGIN = 38.0;
  private final String stockSymbol;
  private final ChartInterval interval;

  private PricePoint pricePoint;
  private Runnable instrumentClickHandler = () -> {};
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

  String text(PricePoint point) {
    Objects.requireNonNull(point, "point");
    return String.format("%s  %s   %s", stockSymbol, interval.displayName(), ohlcvText(point));
  }

  @Override
  public void refreshView() {
    Button symbolSection = new Button(stockSymbol);
    symbolSection.getStyleClass().addAll("link-button", "chart-symbol-button");
    symbolSection.setAccessibleText("Select symbol or instrument");
    symbolSection.setOnAction(ignored -> instrumentClickHandler.run());

    Label intervalSection = new Label(interval.displayName());
    intervalSection.getStyleClass().add("chart-status-label");
    intervalSection.setAccessibleText("Interval " + interval.displayName());
    intervalSection.setMouseTransparent(true);

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
