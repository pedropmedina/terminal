package com.acteque.terminal.chart.statusline;

import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.reload.ReloadHooks;
import com.acteque.terminal.reload.ReloadTarget;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Button.Size;
import com.acteque.terminal.ui.Button.Variant;
import com.acteque.terminal.ui.icons.LucideIcons;
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
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.util.Builder;

/** Builds the reactive JavaFX view for the chart status line. */
final class ChartStatusLineViewBuilder implements Builder<Region>, ReloadTarget {

  private static final CornerRadii IDENTIFIER_RADII = new CornerRadii(3.0);

  private final ChartStatusLineModel model;

  private final FlowPane root = new FlowPane();
  private final Region identifier = new Region();
  private final HBox selectionGroup = new HBox();
  private final HBox metadataGroup = new HBox();
  private final SVGPath selectionSeparator = new SVGPath();

  private final Tooltip instrumentTooltip;
  private final Tooltip intervalTooltip;

  private final Runnable instrumentClickHandler;
  private final Runnable intervalClickHandler;

  /**
   * Creates a status-line view with its chart identifier hidden.
   *
   * @param model the {@link ChartStatusLineModel} supplying displayed state
   * @param tooltipsSuppressed the {@link ObservableBooleanValue} that suppresses open tooltips
   * @param instrumentClickHandler the {@link Runnable} invoked when the instrument is selected
   * @param intervalClickHandler the {@link Runnable} invoked when the interval is selected
   */
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

  /**
   * Creates a status-line view driven by the supplied identifier and tooltip state.
   *
   * @param model the {@link ChartStatusLineModel} supplying displayed state
   * @param identifierColor the {@code ObservableValue<Color>} supplying the identifier color
   * @param identifierVisible the {@link ObservableBooleanValue} controlling identifier visibility
   * @param tooltipsSuppressed the {@link ObservableBooleanValue} that suppresses open tooltips
   * @param instrumentClickHandler the {@link Runnable} invoked when the instrument is selected
   * @param intervalClickHandler the {@link Runnable} invoked when the interval is selected
   */
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
    selectionGroup.getStyleClass().add("chart-status-line-selection-group");
    metadataGroup.getStyleClass().add("chart-status-line-metadata-group");
    root.getChildren().setAll(selectionGroup, metadataGroup);

    identifier.getStyleClass().add("chart-status-line-identifier");
    identifier
      .backgroundProperty()
      .bind(identifierColor.map(color -> new Background(new BackgroundFill(color, IDENTIFIER_RADII, Insets.EMPTY))));
    identifier.visibleProperty().bind(identifierVisible);
    identifier.managedProperty().bind(identifierVisible);
    identifier.setMouseTransparent(true);

    selectionSeparator.setContent(LucideIcons.DOT.pathData());
    selectionSeparator.getStyleClass().addAll("lucide-icon-path", "chart-status-line-selection-separator");
    selectionSeparator.setMouseTransparent(true);

    // Instrument tooltip
    instrumentTooltip = new Tooltip(
      new TooltipTrigger(createInstrumentSection()),
      new TooltipContent(new Label("Select a different instrument"))
    );

    // Interval tooltip
    intervalTooltip = new Tooltip(
      new TooltipTrigger(createIntervalSection()),
      new TooltipContent(new Label("Select a different interval"))
    );

    // Hide tooltips when suppressed
    tooltipsSuppressed.addListener((ignored, wasSuppressed, isSuppressed) -> {
      if (isSuppressed) {
        dismissTooltips();
      }
    });

    selectionGroup.getChildren().setAll(identifier, instrumentTooltip, selectionSeparator, intervalTooltip);
    metadataGroup.getChildren().setAll(createOhlcv());
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

  /** Recreates the controls and metadata after a hot reload while retaining model state. */
  @Override
  public void refreshView() {
    instrumentTooltip.getTrigger().setTarget(createInstrumentSection());
    intervalTooltip.getTrigger().setTarget(createIntervalSection());
    selectionGroup.getChildren().setAll(identifier, instrumentTooltip, selectionSeparator, intervalTooltip);
    metadataGroup.getChildren().setAll(createOhlcv());
  }

  /**
   * Creates an instrument selector bound to the name and logo state.
   *
   * @return the configured {@link Button} for instrument selection
   */
  private Button createInstrumentSection() {
    Button instrumentSection = new Button(null, Variant.GHOST, Size.DEFAULT);
    instrumentSection.getStyleClass().add("chart-instrument-button");
    instrumentSection.setAccessibleText("Select instrument");
    instrumentSection.textProperty().bind(model.instrumentNameProperty());
    instrumentSection
      .graphicProperty()
      .bind(
        Bindings.createObjectBinding(this::createLogoSlot, model.instrumentNameProperty(), model.logoStateProperty())
      );
    instrumentSection.setOnAction(ignored -> {
      instrumentClickHandler.run();
      dismissTooltips();
    });
    return instrumentSection;
  }

  /**
   * Creates an interval selector with bound display and accessible text.
   *
   * @return the configured {@link Button} for interval selection
   */
  private Button createIntervalSection() {
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
    return intervalSection;
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
   * Creates a logo slot with an image or a fallback initial.
   *
   * @return the {@link Node} displaying the logo or fallback
   */
  private Node createLogoSlot() {
    StackPane slot = new StackPane();
    slot.getStyleClass().add("chart-status-line-instrument-logo");
    slot.setMouseTransparent(true);

    ChartStatusLineModel.LogoState logoState = model.getLogoState();
    if (logoState == null) {
      String name = model.getInstrumentName().strip();
      Label fallback = new Label(
        name.isEmpty() ? "?" : name.substring(0, name.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT)
      );
      fallback.getStyleClass().add("chart-status-line-instrument-logo-fallback");
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

  /** Hides both tooltips and cancels any pending display. */
  private void dismissTooltips() {
    instrumentTooltip.dismiss();
    intervalTooltip.dismiss();
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
