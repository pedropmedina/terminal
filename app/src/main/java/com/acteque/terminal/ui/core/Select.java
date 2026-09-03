package com.acteque.terminal.ui.core;

<<<<<<< HEAD
=======
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
>>>>>>> 754c827 (h)
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ChoiceBoxSkin;
<<<<<<< HEAD
=======
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
>>>>>>> 754c827 (h)

/** A single-choice dropdown styled to match the shadcn/ui Select primitive. */
public final class Select<T> extends javafx.scene.control.ChoiceBox<T> {

  private static final String ROOT_STYLE_CLASS = "core-select";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass PLACEHOLDER_SHOWN_PSEUDO_CLASS = PseudoClass.getPseudoClass("placeholder-shown");

  /** Standard trigger dimensions supported by the core select. */
  public enum Size {
    DEFAULT("select-size-default"),
    SM("select-size-sm");

    private final String styleClass;

    Size(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  private String appliedSizeStyleClass;

  private final ObjectProperty<Size> size = new ObjectPropertyBase<>(Size.DEFAULT) {
    @Override
    public void set(Size value) {
      super.set(Objects.requireNonNull(value, "size"));
    }

    @Override
    protected void invalidated() {
      applySize(Objects.requireNonNull(get(), "size"));
    }

    @Override
    public Object getBean() {
      return Select.this;
    }

    @Override
    public String getName() {
      return "size";
    }
  };

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Select.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  private final StringProperty promptText = new SimpleStringProperty(this, "promptText", "");

  public Select() {
    this(FXCollections.observableArrayList());
  }

  public Select(ObservableList<T> items) {
    super(Objects.requireNonNull(items, "items"));
    getStyleClass().add(ROOT_STYLE_CLASS);
    applySize(Size.DEFAULT);
    valueProperty().addListener(ignored -> refreshPlaceholderState());
    promptText.addListener(ignored -> refreshPlaceholderState());
    refreshPlaceholderState();
  }

  public final ObjectProperty<Size> sizeProperty() {
    return size;
  }

  public final Size getSize() {
    return size.get();
  }

  public final void setSize(Size value) {
    size.set(Objects.requireNonNull(value, "size"));
  }

  /** Whether this select currently fails validation. */
  public final boolean isInvalid() {
    return invalid.get();
  }

  public final void setInvalid(boolean value) {
    invalid.set(value);
  }

  public final BooleanProperty invalidProperty() {
    return invalid;
  }

  /** Text displayed while the select has no value. */
  public final String getPromptText() {
    return promptText.get();
  }

  public final void setPromptText(String value) {
    promptText.set(value);
  }

  public final StringProperty promptTextProperty() {
    return promptText;
  }

  @Override
  protected Skin<?> createDefaultSkin() {
    return new SelectSkin<>(this);
  }

  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (attribute == AccessibleAttribute.TEXT && getValue() == null) {
      String accessibleText = getAccessibleText();
      if (accessibleText == null || accessibleText.isEmpty()) {
        String prompt = getPromptText();
        if (prompt != null && !prompt.isEmpty()) {
          return prompt;
        }
      }
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  private void applySize(Size selectedSize) {
    if (appliedSizeStyleClass != null) {
      getStyleClass().remove(appliedSizeStyleClass);
    }
    appliedSizeStyleClass = selectedSize.styleClass;
    if (!getStyleClass().contains(appliedSizeStyleClass)) {
      getStyleClass().add(appliedSizeStyleClass);
    }
  }

  private void refreshPlaceholderState() {
    String prompt = getPromptText();
    boolean placeholderShown = getValue() == null && prompt != null && !prompt.isEmpty();
    pseudoClassStateChanged(PLACEHOLDER_SHOWN_PSEUDO_CLASS, placeholderShown);
  }

  private static final class SelectSkin<T> extends ChoiceBoxSkin<T> {

    private final Select<T> select;
    private final Label placeholder = new Label();
<<<<<<< HEAD
    private final ChangeListener<Object> placeholderVisibilityListener = (ignored, previous, current) ->
      refreshPlaceholderVisibility();
=======
    private final LucideIcon downIndicator = new LucideIcon(LucideIcons.CHEVRON_DOWN);
    private final LucideIcon upIndicator = new LucideIcon(LucideIcons.CHEVRON_UP);
    private final StackPane indicator = new StackPane(downIndicator, upIndicator);
    private final ChangeListener<Object> placeholderVisibilityListener = (ignored, previous, current) ->
      refreshPlaceholderVisibility();
    private final ChangeListener<Boolean> indicatorVisibilityListener = (ignored, previous, current) ->
      refreshIndicatorVisibility();
>>>>>>> 754c827 (h)

    private SelectSkin(Select<T> select) {
      super(select);
      this.select = select;
      placeholder.getStyleClass().setAll("select-placeholder");
      placeholder.setMouseTransparent(true);
      placeholder.textProperty().bind(select.promptTextProperty());
<<<<<<< HEAD
      select.valueProperty().addListener(placeholderVisibilityListener);
      select.promptTextProperty().addListener(placeholderVisibilityListener);
      refreshPlaceholderVisibility();
=======
      configureIndicator();
      select.valueProperty().addListener(placeholderVisibilityListener);
      select.promptTextProperty().addListener(placeholderVisibilityListener);
      select.showingProperty().addListener(indicatorVisibilityListener);
      refreshPlaceholderVisibility();
      refreshIndicatorVisibility();
>>>>>>> 754c827 (h)
      getChildren().add(placeholder);
    }

    @Override
    protected void layoutChildren(double x, double y, double width, double height) {
      super.layoutChildren(x, y, width, height);
      if (!placeholder.isManaged()) {
        return;
      }

      double openButtonWidth = getChildren()
        .stream()
        .filter(SelectSkin::isOpenButton)
        .findFirst()
        .map(node -> node.prefWidth(-1.0))
        .orElse(0.0);
      placeholder.resizeRelocate(x, y, Math.max(0.0, width - openButtonWidth), height);
    }

    @Override
    public void dispose() {
      placeholder.textProperty().unbind();
      select.valueProperty().removeListener(placeholderVisibilityListener);
      select.promptTextProperty().removeListener(placeholderVisibilityListener);
<<<<<<< HEAD
      super.dispose();
    }

=======
      select.showingProperty().removeListener(indicatorVisibilityListener);
      super.dispose();
    }

    private void configureIndicator() {
      indicator.getStyleClass().setAll("select-indicator");
      indicator.setMouseTransparent(true);
      downIndicator.getStyleClass().add("select-indicator-down");
      upIndicator.getStyleClass().add("select-indicator-up");

      getChildren()
        .stream()
        .filter(SelectSkin::isOpenButton)
        .filter(Pane.class::isInstance)
        .map(Pane.class::cast)
        .findFirst()
        .ifPresent(openButton -> {
          openButton.getChildren().removeIf(node -> node.getStyleClass().contains("arrow"));
          openButton.getChildren().add(indicator);
        });
    }

>>>>>>> 754c827 (h)
    private void refreshPlaceholderVisibility() {
      String prompt = select.getPromptText();
      boolean visible = select.getValue() == null && prompt != null && !prompt.isEmpty();
      placeholder.setManaged(visible);
      placeholder.setVisible(visible);
      select.requestLayout();
    }

<<<<<<< HEAD
=======
    private void refreshIndicatorVisibility() {
      boolean showing = select.isShowing();
      downIndicator.setVisible(!showing);
      upIndicator.setVisible(showing);
    }

>>>>>>> 754c827 (h)
    private static boolean isOpenButton(Node node) {
      return node.getStyleClass().contains("open-button");
    }
  }
}
