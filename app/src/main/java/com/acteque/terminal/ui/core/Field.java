package com.acteque.terminal.ui.core;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.css.StyleablePropertyFactory;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A composable form-field primitive modeled after shadcn/ui's Field family.
 *
 * <p>The related components are nested so their Java names match the postfixes of the React
 * exports: {@code Field.Label}, {@code Field.Description}, {@code Field.Error}, and so on.
 */
public final class Field extends Pane {

  private static final StyleablePropertyFactory<Field> STYLEABLES = new StyleablePropertyFactory<>(
    Pane.getClassCssMetaData()
  );
  private static final String ROOT_STYLE_CLASS = "core-field";
  private static final String VERTICAL_STYLE_CLASS = "field-orientation-vertical";
  private static final String HORIZONTAL_STYLE_CLASS = "field-orientation-horizontal";
  private static final String RESPONSIVE_STYLE_CLASS = "field-orientation-responsive";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final double RESPONSIVE_BREAKPOINT = 448.0;

  /** Layout variants corresponding to shadcn's vertical, horizontal, and responsive variants. */
  public enum Orientation {
    VERTICAL(VERTICAL_STYLE_CLASS),
    HORIZONTAL(HORIZONTAL_STYLE_CLASS),
    RESPONSIVE(RESPONSIVE_STYLE_CLASS);

    private final String styleClass;

    Orientation(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  /** Visual variants for a field legend. */
  public enum LegendVariant {
    LEGEND("field-legend-variant-legend"),
    LABEL("field-legend-variant-label");

    private final String styleClass;

    LegendVariant(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  private final StyleableProperty<Number> spacing = STYLEABLES.createStyleableNumberProperty(
    this,
    "spacing",
    "-fx-spacing",
    field -> field.spacing,
    8.0
  );

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Field.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  private final ObjectProperty<Orientation> orientation = new ObjectPropertyBase<>(Orientation.VERTICAL) {
    @Override
    public void set(Orientation value) {
      super.set(Objects.requireNonNull(value, "orientation"));
    }

    @Override
    protected void invalidated() {
      applyOrientation(Objects.requireNonNull(get(), "orientation"));
      requestLayout();
    }

    @Override
    public Object getBean() {
      return Field.this;
    }

    @Override
    public String getName() {
      return "orientation";
    }
  };

  public Field() {
    this(Orientation.VERTICAL);
  }

  public Field(Node... children) {
    this(Orientation.VERTICAL, children);
  }

  public Field(Orientation orientation, Node... children) {
    getStyleClass().add(ROOT_STYLE_CLASS);
    setAccessibleRole(AccessibleRole.PARENT);
    ((Observable) spacing).addListener(ignored -> requestLayout());
    applyOrientation(Orientation.VERTICAL);
    setOrientation(orientation);
    getChildren().addAll(children);
  }

  public final ObjectProperty<Orientation> orientationProperty() {
    return orientation;
  }

  public final Orientation getOrientation() {
    return orientation.get();
  }

  public final void setOrientation(Orientation value) {
    orientation.set(Objects.requireNonNull(value, "orientation"));
  }

  public final boolean isInvalid() {
    return invalid.get();
  }

  public final void setInvalid(boolean value) {
    invalid.set(value);
  }

  public final BooleanProperty invalidProperty() {
    return invalid;
  }

  @Override
  public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
    return STYLEABLES.getCssMetaData();
  }

  @Override
  protected double computeMinWidth(double height) {
    return computeContentWidth(height, true);
  }

  @Override
  protected double computePrefWidth(double height) {
    return computeContentWidth(height, false);
  }

  @Override
  protected double computeMinHeight(double width) {
    return computeContentHeight(width, true);
  }

  @Override
  protected double computePrefHeight(double width) {
    return computeContentHeight(width, false);
  }

  @Override
  protected void layoutChildren() {
    if (usesHorizontalLayout(getWidth())) {
      layoutHorizontally();
    } else {
      layoutVertically();
    }
  }

  private void layoutVertically() {
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double width = Math.max(0.0, getWidth() - x - snappedRightInset());
    List<Node> children = getManagedChildren();
    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      double height = boundedSize(child.prefHeight(width), child.minHeight(width), child.maxHeight(width));
      child.resizeRelocate(x, y, width, snapSizeY(height));
      y += snapSizeY(height);
      if (index < children.size() - 1) {
        y += snappedSpacingY();
      }
    }
  }

  private void layoutHorizontally() {
    double x = snappedLeftInset();
    double y = snappedTopInset();
    double width = Math.max(0.0, getWidth() - x - snappedRightInset());
    double height = Math.max(0.0, getHeight() - y - snappedBottomInset());
    List<Node> children = getManagedChildren();
    double available = Math.max(0.0, width - Math.max(0, children.size() - 1) * snappedSpacingX());
    double fixedWidth = children
      .stream()
      .filter(node -> !(node instanceof Content))
      .mapToDouble(node -> node.prefWidth(height))
      .sum();
    long flexibleChildren = children.stream().filter(Content.class::isInstance).count();
    double flexibleWidth = flexibleChildren == 0 ? 0.0 : Math.max(0.0, available - fixedWidth) / flexibleChildren;
    boolean alignAtTop = flexibleChildren > 0;

    for (int index = 0; index < children.size(); index++) {
      Node child = children.get(index);
      double childWidth = child instanceof Content ? flexibleWidth : child.prefWidth(height);
      childWidth = boundedSize(childWidth, child.minWidth(height), child.maxWidth(height));
      double childHeight = boundedSize(
        child.prefHeight(childWidth),
        child.minHeight(childWidth),
        child.maxHeight(childWidth)
      );
      double childY = alignAtTop ? y : y + Math.max(0.0, (height - childHeight) / 2.0);
      child.resizeRelocate(x, childY, snapSizeX(childWidth), snapSizeY(childHeight));
      x += snapSizeX(childWidth);
      if (index < children.size() - 1) {
        x += snappedSpacingX();
      }
    }
  }

  private double computeContentWidth(double height, boolean minimum) {
    boolean horizontal = getOrientation() == Orientation.HORIZONTAL;
    double content = 0.0;
    for (Node child : getManagedChildren()) {
      double childWidth = minimum ? child.minWidth(height) : child.prefWidth(height);
      content = horizontal ? content + childWidth : Math.max(content, childWidth);
    }
    if (horizontal) {
      content += Math.max(0, getManagedChildren().size() - 1) * snappedSpacingX();
    }
    return snappedLeftInset() + content + snappedRightInset();
  }

  private double computeContentHeight(double width, boolean minimum) {
    boolean horizontal = usesHorizontalLayout(width);
    double content = 0.0;
    for (Node child : getManagedChildren()) {
      double childHeight = minimum ? child.minHeight(width) : child.prefHeight(width);
      content = horizontal ? Math.max(content, childHeight) : content + childHeight;
    }
    if (!horizontal) {
      content += Math.max(0, getManagedChildren().size() - 1) * snappedSpacingY();
    }
    return snappedTopInset() + content + snappedBottomInset();
  }

  private boolean usesHorizontalLayout(double width) {
    return (
      getOrientation() == Orientation.HORIZONTAL ||
      (getOrientation() == Orientation.RESPONSIVE && width >= RESPONSIVE_BREAKPOINT)
    );
  }

  private double snappedSpacingX() {
    return snapSpaceX(spacing.getValue().doubleValue());
  }

  private double snappedSpacingY() {
    return snapSpaceY(spacing.getValue().doubleValue());
  }

  private static double boundedSize(double preferred, double minimum, double maximum) {
    return Math.min(Math.max(preferred, minimum), Math.max(minimum, maximum));
  }

  private void applyOrientation(Orientation selectedOrientation) {
    getStyleClass().removeAll(VERTICAL_STYLE_CLASS, HORIZONTAL_STYLE_CLASS, RESPONSIVE_STYLE_CLASS);
    getStyleClass().add(selectedOrientation.styleClass);
  }

  /** Semantic container for a related set of fields. */
  public static final class Set extends VBox {

    public Set(Node... children) {
      getStyleClass().add("core-field-set");
      setAccessibleRole(AccessibleRole.PARENT);
      getChildren().addAll(children);
    }
  }

  /** Heading for a {@link Set}. */
  public static final class Legend extends com.acteque.terminal.ui.core.Label {

    private final ObjectProperty<LegendVariant> variant = new ObjectPropertyBase<>(LegendVariant.LEGEND) {
      @Override
      public void set(LegendVariant value) {
        super.set(Objects.requireNonNull(value, "variant"));
      }

      @Override
      protected void invalidated() {
        applyVariant(Objects.requireNonNull(get(), "variant"));
      }

      @Override
      public Object getBean() {
        return Legend.this;
      }

      @Override
      public String getName() {
        return "variant";
      }
    };

    public Legend() {
      this(null, LegendVariant.LEGEND);
    }

    public Legend(String text) {
      this(text, LegendVariant.LEGEND);
    }

    public Legend(String text, LegendVariant variant) {
      super(text);
      getStyleClass().add("core-field-legend");
      applyVariant(LegendVariant.LEGEND);
      setVariant(variant);
    }

    public final ObjectProperty<LegendVariant> variantProperty() {
      return variant;
    }

    public final LegendVariant getVariant() {
      return variant.get();
    }

    public final void setVariant(LegendVariant value) {
      variant.set(Objects.requireNonNull(value, "variant"));
    }

    private void applyVariant(LegendVariant selectedVariant) {
      getStyleClass().removeAll(LegendVariant.LEGEND.styleClass, LegendVariant.LABEL.styleClass);
      getStyleClass().add(selectedVariant.styleClass);
    }
  }

  /** Vertical group of fields and optional separators. */
  public static final class Group extends VBox {

    public Group(Node... children) {
      getStyleClass().add("core-field-group");
      setAccessibleRole(AccessibleRole.PARENT);
      getChildren().addAll(children);
    }
  }

  /** Flexing label, description, and error content used in horizontal fields. */
  public static final class Content extends VBox {

    public Content(Node... children) {
      getStyleClass().add("core-field-content");
      getChildren().addAll(children);
    }
  }

  /** Label associated with the field's input control. */
  public static final class Label extends com.acteque.terminal.ui.core.Label {

    public Label() {
      this(null, null);
    }

    public Label(String text) {
      this(text, null);
    }

    public Label(String text, Node graphic) {
      super(text, graphic);
      getStyleClass().add("core-field-label");
      setWrapText(true);
    }
  }

  /** Non-interactive field heading used when a label association is not appropriate. */
  public static final class Title extends javafx.scene.control.Label {

    public Title() {
      this(null, null);
    }

    public Title(String text) {
      this(text, null);
    }

    public Title(String text, Node graphic) {
      super(text, graphic);
      getStyleClass().add("core-field-title");
      setWrapText(true);
    }
  }

  /** Supporting text for a field or field set. */
  public static final class Description extends javafx.scene.control.Label {

    public Description() {
      this(null);
    }

    public Description(String text) {
      super(text);
      getStyleClass().add("core-field-description");
      setWrapText(true);
    }
  }

  /** Horizontal divider with optional centered content. */
  public static final class Separator extends StackPane {

    private final com.acteque.terminal.ui.core.Separator line = new com.acteque.terminal.ui.core.Separator();
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content") {
      @Override
      protected void invalidated() {
        rebuild();
      }
    };

    public Separator() {
      this((Node) null);
    }

    public Separator(String text) {
      this(text == null ? null : new javafx.scene.control.Label(text));
    }

    public Separator(Node content) {
      getStyleClass().add("core-field-separator");
      setAlignment(Pos.CENTER);
      setContent(content);
    }

    public final ObjectProperty<Node> contentProperty() {
      return content;
    }

    public final Node getContent() {
      return content.get();
    }

    public final void setContent(Node value) {
      content.set(value);
    }

    private void rebuild() {
      getChildren().setAll(line);
      if (getContent() != null) {
        getContent().getStyleClass().add("field-separator-content");
        getChildren().add(getContent());
      }
      pseudoClassStateChanged(PseudoClass.getPseudoClass("content"), getContent() != null);
    }
  }

  /** Validation feedback that deduplicates messages and exposes them to assistive technology. */
  public static final class Error extends VBox {

    private final ObservableList<String> errors = FXCollections.observableArrayList();
    private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content") {
      @Override
      protected void invalidated() {
        rebuild();
      }
    };

    public Error() {
      this(List.<String>of());
    }

    public Error(String message) {
      this(message == null ? List.of() : List.of(message));
    }

    public Error(Collection<String> errors) {
      getStyleClass().add("core-field-error");
      setAccessibleRole(AccessibleRole.TEXT);
      this.errors.addListener((ListChangeListener<String>) change -> rebuild());
      setErrors(errors);
    }

    public final ObservableList<String> getErrors() {
      return errors;
    }

    public final void setErrors(Collection<String> values) {
      errors.setAll(Objects.requireNonNull(values, "errors"));
    }

    public final ObjectProperty<Node> contentProperty() {
      return content;
    }

    public final Node getContent() {
      return content.get();
    }

    public final void setContent(Node value) {
      content.set(value);
    }

    private void rebuild() {
      getChildren().clear();
      if (getContent() != null) {
        getChildren().add(getContent());
      } else {
        LinkedHashSet<String> uniqueMessages = new LinkedHashSet<>();
        errors
          .stream()
          .filter(Objects::nonNull)
          .filter(message -> !message.isBlank())
          .forEach(uniqueMessages::add);
        boolean multiple = uniqueMessages.size() > 1;
        uniqueMessages.forEach(message -> {
          javafx.scene.control.Label item = new javafx.scene.control.Label(multiple ? "\u2022 " + message : message);
          item.setWrapText(true);
          item.getStyleClass().add("field-error-item");
          getChildren().add(item);
        });
      }
      boolean hasContent = !getChildren().isEmpty();
      setManaged(hasContent);
      setVisible(hasContent);
      setAccessibleText(accessibleText());
    }

    private String accessibleText() {
      if (getContent() != null) {
        return Objects.requireNonNullElse(getContent().getAccessibleText(), "");
      }
      LinkedHashSet<String> messages = new LinkedHashSet<>();
      errors
        .stream()
        .filter(Objects::nonNull)
        .filter(message -> !message.isBlank())
        .forEach(messages::add);
      return String.join(". ", messages);
    }
  }
}
