package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart status line. */
final class ChartStatusLineViewBuilder implements Builder<Region>, ReloadTarget {

  private final ChartStatusLineModel model;
  private final HBox root = new HBox();

  /**
   * Creates a passive status-line view for price and volume metadata.
   *
   * @param model the {@link ChartStatusLineModel} supplying displayed state
   */
  ChartStatusLineViewBuilder(ChartStatusLineModel model) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    root.getStyleClass().add("chart-status-line");
    root.setPickOnBounds(false);
    refreshView();
    ReloadHooks.register(this);
  }

  /**
   * Returns the assembled status-line root.
   *
   * @return the {@link Region} containing the status line
   */
  @Override
  public Region build() {
    return root;
  }

  /** Recreates the metadata after a hot reload while retaining model state. */
  @Override
  public void refreshView() {
    root.getChildren().setAll(createOhlcv());
  }

  /**
   * Creates a label bound to the selected price point's OHLCV values.
   *
   * @return the bound {@link Label} for price and volume metadata
   */
  private Label createOhlcv() {
    Label ohlcv = new Label();
    ohlcv.getStyleClass().add("chart-status-line-label");
    ohlcv.setAccessibleText("Open, high, low, close, and volume");
    ohlcv.setMouseTransparent(true);
    ohlcv
      .textProperty()
      .bind(Bindings.createStringBinding(() -> ohlcvText(model.getPricePoint()), model.pricePointProperty()));
    return ohlcv;
  }

  /**
   * Formats a price point for the status line, or returns an empty string when absent.
   *
   * @param point the {@link PricePoint} to format, or {@code null} when none is selected
   * @return the formatted {@link String} of OHLCV values, or an empty string
   */
  private static String ohlcvText(PricePoint point) {
    if (point == null) {
      return "";
    }
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
