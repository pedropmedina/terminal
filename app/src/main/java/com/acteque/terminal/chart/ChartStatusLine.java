package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.InstrumentLogo;
import com.acteque.terminal.ui.ChartReloadHooks;
import com.acteque.terminal.ui.RefreshableView;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import java.net.URI;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

/** Displays the chart's OHLCV status for the selected price point. */
final class ChartStatusLine extends HBox implements RefreshableView {

  private static final double LEFT_MARGIN = 12.0;
  private static final double BOTTOM_MARGIN = 38.0;
  private String instrumentName;
  private ChartInterval interval;

  private PricePoint pricePoint;
  private Runnable instrumentClickHandler = () -> {};
  private Runnable intervalClickHandler = () -> {};
  private Button symbolSection;
  private Button intervalSection;
  private Label ohlcv;
  private InstrumentLogo logo;
  private Image logoImage;
  private final Hyperlink logoAttribution = new Hyperlink();
  private Consumer<URI> openLink = ignored -> {};

  ChartStatusLine(String instrumentName, ChartInterval interval) {
    this.instrumentName = Objects.requireNonNull(instrumentName, "instrumentName");
    this.interval = Objects.requireNonNull(interval, "interval");
    getStyleClass().add("chart-status-line");
    logoAttribution.getStyleClass().add("chart-logo-attribution");
    logoAttribution.setMinWidth(USE_PREF_SIZE);
    logoAttribution.managedProperty().bind(logoAttribution.visibleProperty());
    logoAttribution.setOnAction(ignored -> {
      if (logo != null) {
        openLink.accept(logo.attributionUri());
      }
    });

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

  void setInstrumentName(String instrumentName) {
    this.instrumentName = Objects.requireNonNull(instrumentName, "instrumentName");
    symbolSection.setText(instrumentName);
    clearInstrumentLogo();
  }

  Hyperlink logoAttribution() {
    return logoAttribution;
  }

  void onOpenLink(Consumer<URI> callback) {
    openLink = Objects.requireNonNull(callback, "callback");
  }

  void setInstrumentLogo(InstrumentLogo logo, Image image) {
    Objects.requireNonNull(logo, "logo");
    Objects.requireNonNull(image, "image");
    if (image.isError() || image.getWidth() <= 0 || image.getHeight() <= 0) {
      clearInstrumentLogo();
      return;
    }
    this.logo = logo;
    this.logoImage = image;
    refreshLogo();
  }

  private void clearInstrumentLogo() {
    logo = null;
    logoImage = null;
    refreshLogo();
  }

  private void refreshLogo() {
    StackPane slot = new StackPane();
    slot.getStyleClass().add("chart-instrument-logo");
    slot.setMouseTransparent(true);
    if (logoImage == null) {
      String name = instrumentName.strip();
      Label fallback = new Label(
        name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT)
      );
      fallback.getStyleClass().add("chart-instrument-logo-fallback");
      slot.getChildren().setAll(fallback);
    } else {
      ImageView imageView = new ImageView(logoImage);
      imageView.setPreserveRatio(true);
      imageView.fitWidthProperty().bind(slot.widthProperty());
      imageView.fitHeightProperty().bind(slot.heightProperty());
      slot.getChildren().setAll(imageView);
    }
    symbolSection.setGraphic(slot);
    logoAttribution.setText(logo == null ? "" : logo.attributionText());
    logoAttribution.setAccessibleText(logo == null ? "" : logo.attributionText() + ", opens in browser");
    logoAttribution.setVisible(logo != null);
    logoAttribution.setVisited(false);
  }

  void setInterval(ChartInterval interval) {
    this.interval = Objects.requireNonNull(interval, "interval");
    intervalSection.setText(interval.displayName());
    intervalSection.setAccessibleText("Select interval, currently " + interval.displayName());
  }

  String text(PricePoint point) {
    Objects.requireNonNull(point, "point");
    return String.format("%s  %s   %s", instrumentName, interval.displayName(), ohlcvText(point));
  }

  @Override
  public void refreshView() {
    symbolSection = new Button(instrumentName, Variant.GHOST, Size.DEFAULT);
    symbolSection.getStyleClass().add("chart-symbol-button");
    symbolSection.setAccessibleText("Select symbol or instrument");
    symbolSection.setOnAction(ignored -> instrumentClickHandler.run());
    refreshLogo();

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
