package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.tooltip.Tooltip;
import com.acteque.terminal.ui.tooltip.TooltipContent;
import com.acteque.terminal.ui.tooltip.TooltipTrigger;
import java.util.Locale;
import java.util.Objects;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart status line. */
final class ChartStatusLineViewBuilder implements Builder<Region>, ReloadTarget {

  private static final CornerRadii IDENTIFIER_RADII = new CornerRadii(3.0);

  private final ChartStatusLineModel model;
  private final Runnable instrumentClickHandler;
  private final Runnable intervalClickHandler;
  private final FlowPane root = new FlowPane();
  private final Region identifier = new Region();
  private final HBox instrument = new HBox();
  private final HBox metadata = new HBox();
  private final Label logoAttribution = new Label();
  private final Tooltip symbolTooltip;
  private final Tooltip intervalTooltip;

  ChartStatusLineViewBuilder(
    ChartStatusLineModel model,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentClickHandler,
    Runnable intervalClickHandler
  ) {
    this(
      model,
      new SimpleObjectProperty<>(Color.TRANSPARENT),
      new SimpleBooleanProperty(false),
      tooltipsSuppressed,
      instrumentClickHandler,
      intervalClickHandler
    );
  }

  ChartStatusLineViewBuilder(
    ChartStatusLineModel model,
    ObservableValue<Color> identifierColor,
    ObservableBooleanValue identifierVisible,
    ObservableBooleanValue tooltipsSuppressed,
    Runnable instrumentClickHandler,
    Runnable intervalClickHandler
  ) {
    this.model = Objects.requireNonNull(model, "model cannot be null");
    this.instrumentClickHandler = Objects.requireNonNull(
      instrumentClickHandler,
      "instrumentClickHandler cannot be null"
    );
    this.intervalClickHandler = Objects.requireNonNull(intervalClickHandler, "intervalClickHandler cannot be null");
    Objects.requireNonNull(identifierColor, "identifierColor cannot be null");
    Objects.requireNonNull(identifierVisible, "identifierVisible cannot be null");
    Objects.requireNonNull(tooltipsSuppressed, "tooltipsSuppressed cannot be null");

    root.getStyleClass().add("chart-status-line");
    root.setPickOnBounds(false);
    instrument.getStyleClass().add("chart-status-instrument");
    metadata.getStyleClass().add("chart-status-metadata");
    root.getChildren().setAll(instrument, metadata);

    identifier.getStyleClass().add("chart-identifier");
    identifier
      .backgroundProperty()
      .bind(
        Bindings.createObjectBinding(
          () -> new Background(new BackgroundFill(identifierColor.getValue(), IDENTIFIER_RADII, Insets.EMPTY)),
          identifierColor
        )
      );
    identifier.visibleProperty().bind(identifierVisible);
    identifier.managedProperty().bind(identifierVisible);
    identifier.setMouseTransparent(true);

    logoAttribution.getStyleClass().add("chart-symbol-tooltip-attribution");
    logoAttribution
      .textProperty()
      .bind(Bindings.createStringBinding(this::logoAttributionText, model.logoStateProperty()));
    logoAttribution.visibleProperty().bind(Bindings.isNotNull(model.logoStateProperty()));
    logoAttribution.managedProperty().bind(logoAttribution.visibleProperty());

    VBox symbolTooltipContent = new VBox(
      new Label("Click to select a different symbol"),
      shortcutNote("Shortcut: ⌘F, ⌘/, or ⌘P"),
      logoAttribution
    );
    symbolTooltipContent.getStyleClass().add("chart-symbol-tooltip-content");
    symbolTooltip = new Tooltip(new TooltipTrigger(new Button()), new TooltipContent(symbolTooltipContent));
    intervalTooltip = new Tooltip(
      new TooltipTrigger(new Button()),
      new TooltipContent(new VBox(new Label("Click to select a different interval"), shortcutNote("Shortcut: ⌘I")))
    );

    tooltipsSuppressed.addListener((ignored, wasSuppressed, isSuppressed) -> {
      if (isSuppressed) {
        dismissTooltips();
      }
    });

    refreshView();
    ReloadHooks.register(this);
  }

  @Override
  public Region build() {
    return root;
  }

  @Override
  public void refreshView() {
    Button symbolSection = new Button(null, Variant.GHOST, Size.DEFAULT);
    symbolSection.getStyleClass().add("chart-symbol-button");
    symbolSection.setAccessibleText("Select symbol or instrument");
    symbolSection.textProperty().bind(model.instrumentNameProperty());
    symbolSection
      .graphicProperty()
      .bind(
        Bindings.createObjectBinding(this::createLogoSlot, model.instrumentNameProperty(), model.logoStateProperty())
      );
    symbolSection.setOnAction(ignored -> {
      instrumentClickHandler.run();
      dismissTooltips();
    });
    symbolTooltip.getTrigger().setTarget(symbolSection);

    Button intervalSection = new Button(null, Variant.GHOST, Size.DEFAULT);
    intervalSection.getStyleClass().add("chart-interval-button");
    intervalSection
      .textProperty()
      .bind(
        Bindings.createStringBinding(() -> ChartIntervalText.displayName(model.getInterval()), model.intervalProperty())
      );
    intervalSection
      .accessibleTextProperty()
      .bind(
        Bindings.createStringBinding(
          () -> "Select interval, currently " + ChartIntervalText.displayName(model.getInterval()),
          model.intervalProperty()
        )
      );
    intervalSection.setOnAction(ignored -> {
      intervalClickHandler.run();
      dismissTooltips();
    });
    intervalTooltip.getTrigger().setTarget(intervalSection);

    Label ohlcv = new Label();
    ohlcv.getStyleClass().add("chart-status-label");
    ohlcv.setAccessibleText("Open, high, low, close, and volume");
    ohlcv.setMouseTransparent(true);
    ohlcv
      .textProperty()
      .bind(Bindings.createStringBinding(() -> ohlcvText(model.getPricePoint()), model.pricePointProperty()));

    instrument.getChildren().setAll(identifier, symbolTooltip, intervalTooltip);
    metadata.getChildren().setAll(ohlcv);
  }

  private Node createLogoSlot() {
    StackPane slot = new StackPane();
    slot.getStyleClass().add("chart-instrument-logo");
    slot.setMouseTransparent(true);
    ChartStatusLineModel.LogoState logoState = model.getLogoState();
    if (logoState == null) {
      String name = model.getInstrumentName().strip();
      Label fallback = new Label(
        name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT)
      );
      fallback.getStyleClass().add("chart-instrument-logo-fallback");
      slot.getChildren().setAll(fallback);
    } else {
      ImageView imageView = new ImageView(logoState.image());
      imageView.setPreserveRatio(true);
      imageView.fitWidthProperty().bind(slot.widthProperty());
      imageView.fitHeightProperty().bind(slot.heightProperty());
      slot.getChildren().setAll(imageView);
    }
    return slot;
  }

  private String logoAttributionText() {
    ChartStatusLineModel.LogoState logoState = model.getLogoState();
    return logoState == null ? "" : logoState.logo().attributionText();
  }

  private void dismissTooltips() {
    symbolTooltip.dismiss();
    intervalTooltip.dismiss();
  }

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

  private static Label shortcutNote(String text) {
    Label note = new Label(text);
    note.getStyleClass().add("chart-tooltip-shortcut");
    return note;
  }
}
