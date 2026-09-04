package com.acteque.terminal.ui.core;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Objects;
import javafx.application.Platform;
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
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleAttribute;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Skin;
import javafx.scene.control.skin.ChoiceBoxSkin;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.stage.Window;

/** A single-choice dropdown styled to match the shadcn/ui Select primitive. */
public final class Select<T> extends javafx.scene.control.ChoiceBox<T> {

  private static final String ROOT_STYLE_CLASS = "core-select";
  private static final String POPUP_ROOT_STYLE_CLASS = "core-select-popup-root";
  private static final String ITEM_CHECK_STYLE_CLASS = "select-item-check";
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
    private final LucideIcon downIndicator = new LucideIcon(LucideIcons.CHEVRON_DOWN);
    private final LucideIcon upIndicator = new LucideIcon(LucideIcons.CHEVRON_UP);
    private final StackPane indicator = new StackPane(downIndicator, upIndicator);
    private final ChangeListener<Object> placeholderVisibilityListener = (ignored, previous, current) ->
      refreshPlaceholderVisibility();
    private final ChangeListener<Boolean> indicatorVisibilityListener = (ignored, previous, current) ->
      refreshIndicatorVisibility();
    private final ChangeListener<Number> selectionListener = (ignored, previous, current) -> refreshItemChecks();
    private final ChangeListener<Number> widthListener = (ignored, previous, current) -> {
      if (getSkinnable().isShowing()) {
        resizePopupToTrigger();
      }
    };
    private final EventHandler<Event> popupShownHandler = ignored -> {
      boolean configured = configurePopupForShow();
      Platform.runLater(() -> {
        if (!getSkinnable().isShowing()) {
          return;
        }
        if (configured) {
          resizeAndAlignPopup();
        } else {
          configurePopupForShow();
        }
      });
    };
    private ContextMenu popup;
    private Region popupContent;
    private Region popupSurface;
    private Region clippedPopupContent;
    private Rectangle popupClip;

    private SelectSkin(Select<T> select) {
      super(select);
      this.select = select;
      placeholder.getStyleClass().setAll("select-placeholder");
      placeholder.setMouseTransparent(true);
      placeholder.textProperty().bind(select.promptTextProperty());
      configureIndicator();
      select.valueProperty().addListener(placeholderVisibilityListener);
      select.promptTextProperty().addListener(placeholderVisibilityListener);
      select.showingProperty().addListener(indicatorVisibilityListener);
      select.getSelectionModel().selectedIndexProperty().addListener(selectionListener);
      select.widthProperty().addListener(widthListener);
      select.addEventHandler(javafx.scene.control.ChoiceBox.ON_SHOWN, popupShownHandler);
      refreshPlaceholderVisibility();
      refreshIndicatorVisibility();
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
      select.showingProperty().removeListener(indicatorVisibilityListener);
      select.getSelectionModel().selectedIndexProperty().removeListener(selectionListener);
      select.widthProperty().removeListener(widthListener);
      select.removeEventHandler(javafx.scene.control.ChoiceBox.ON_SHOWN, popupShownHandler);
      clearPopupClip();
      clearPopupWidthBindings();
      popupContent = null;
      popupSurface = null;
      popup = null;
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

    private void refreshPlaceholderVisibility() {
      String prompt = select.getPromptText();
      boolean visible = select.getValue() == null && prompt != null && !prompt.isEmpty();
      placeholder.setManaged(visible);
      placeholder.setVisible(visible);
      select.requestLayout();
    }

    private void refreshIndicatorVisibility() {
      boolean showing = select.isShowing();
      downIndicator.setVisible(!showing);
      upIndicator.setVisible(showing);
    }

    private boolean configurePopupForShow() {
      if (!findAndInitializePopup()) {
        return false;
      }

      refreshItemChecks();
      initializePopupRegions();
      resizeAndAlignPopup();
      return true;
    }

    private boolean findAndInitializePopup() {
      if (popup != null) {
        return true;
      }

      popup = Window.getWindows()
        .stream()
        .filter(ContextMenu.class::isInstance)
        .map(ContextMenu.class::cast)
        .filter(window -> window.isShowing() && window.getOwnerNode() == select)
        .findFirst()
        .orElse(null);
      if (popup == null) {
        return false;
      }

      if (!popup.getScene().getRoot().getStyleClass().contains(POPUP_ROOT_STYLE_CLASS)) {
        popup.getScene().getRoot().getStyleClass().add(POPUP_ROOT_STYLE_CLASS);
      }
      bindWidth(popup);
      return true;
    }

    private void initializePopupRegions() {
      if (popupContent == null && popup.getSkin().getNode() instanceof Region content) {
        popupContent = content;
        bindWidth(popupContent);
        configurePopupClip(popupContent);
      }

      if (popupSurface == null && popup.getStyleableNode() instanceof Region surface) {
        popupSurface = surface;
        if (popupSurface != popupContent) {
          bindWidth(popupSurface);
        }
      }
    }

    private void resizeAndAlignPopup() {
      resizePopupToTrigger();
      alignSelectedItemWithTrigger();
    }

    private void resizePopupToTrigger() {
      if (popup == null) {
        return;
      }

      double triggerWidth = select.getWidth();
      if (popupContent != null) {
        popupContent.resize(triggerWidth, popupContent.prefHeight(triggerWidth));
        popupContent.layout();
      }
      if (popupSurface != null && popupSurface != popupContent) {
        popupSurface.resize(triggerWidth, popupSurface.prefHeight(triggerWidth));
      }
    }

    private void clearPopupWidthBindings() {
      if (popup != null) {
        unbindWidth(popup);
      }
      if (popupContent != null) {
        unbindWidth(popupContent);
      }
      if (popupSurface != null && popupSurface != popupContent) {
        unbindWidth(popupSurface);
      }
    }

    private void bindWidth(ContextMenu popup) {
      popup.minWidthProperty().bind(select.widthProperty());
      popup.prefWidthProperty().bind(select.widthProperty());
      popup.maxWidthProperty().bind(select.widthProperty());
    }

    private void bindWidth(Region region) {
      region.minWidthProperty().bind(select.widthProperty());
      region.prefWidthProperty().bind(select.widthProperty());
      region.maxWidthProperty().bind(select.widthProperty());
    }

    private static void unbindWidth(ContextMenu popup) {
      popup.minWidthProperty().unbind();
      popup.prefWidthProperty().unbind();
      popup.maxWidthProperty().unbind();
    }

    private static void unbindWidth(Region region) {
      region.minWidthProperty().unbind();
      region.prefWidthProperty().unbind();
      region.maxWidthProperty().unbind();
    }

    private void alignSelectedItemWithTrigger() {
      if (select.getSelectionModel().getSelectedIndex() < 0) {
        return;
      }

      Node check = popup
        .getSkin()
        .getNode()
        .lookup("." + ITEM_CHECK_STYLE_CLASS);
      Bounds triggerBounds = select.localToScreen(select.getBoundsInLocal());
      if (
        check == null || check.getParent() == null || check.getParent().getParent() == null || triggerBounds == null
      ) {
        return;
      }

      Node selectedItem = check.getParent().getParent();
      Bounds selectedItemBounds = selectedItem.localToScreen(selectedItem.getBoundsInLocal());
      if (selectedItemBounds != null) {
        popup.setY(popup.getY() + triggerBounds.getCenterY() - selectedItemBounds.getCenterY());
      }
    }

    private void refreshItemChecks() {
      if (popup == null) {
        return;
      }

      int selectedIndex = select.getSelectionModel().getSelectedIndex();
      for (int index = 0; index < popup.getItems().size(); index++) {
        if (!(popup.getItems().get(index) instanceof RadioMenuItem item)) {
          continue;
        }
        if (index == selectedIndex) {
          if (item.getGraphic() == null) {
            LucideIcon check = new LucideIcon(LucideIcons.CHECK);
            check.getStyleClass().add(ITEM_CHECK_STYLE_CLASS);
            item.setGraphic(check);
          }
        } else {
          item.setGraphic(null);
        }
      }

      if (popup.isShowing()) {
        popup.getScene().getRoot().applyCss();
        configureItemLayout();
      }
    }

    private void configurePopupClip(Region popupContent) {
      if (clippedPopupContent != popupContent) {
        clearPopupClip();
        clippedPopupContent = popupContent;
        popupClip = new Rectangle();
        popupClip.widthProperty().bind(popupContent.widthProperty());
        popupClip.heightProperty().bind(popupContent.heightProperty());
        popupContent.setClip(popupClip);
      }

      double cornerRadius = popupContent.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius();
      popupClip.setArcWidth(cornerRadius * 2.0);
      popupClip.setArcHeight(cornerRadius * 2.0);
    }

    private void clearPopupClip() {
      if (popupClip != null) {
        popupClip.widthProperty().unbind();
        popupClip.heightProperty().unbind();
      }
      if (clippedPopupContent != null && clippedPopupContent.getClip() == popupClip) {
        clippedPopupContent.setClip(null);
      }
      clippedPopupContent = null;
      popupClip = null;
    }

    private void configureItemLayout() {
      Node popupContent = popup.getSkin().getNode();
      for (Node itemNode : popupContent.lookupAll(".radio-menu-item")) {
        if (!(itemNode instanceof Region item)) {
          continue;
        }

        Node nativeIndicator = item.lookup(".left-container");
        if (nativeIndicator != null) {
          nativeIndicator.setManaged(false);
          nativeIndicator.setVisible(false);
        }

        Node label = item.lookup(".label");
        if (label != null) {
          label.translateXProperty().unbind();
          label.translateXProperty().bind(label.layoutXProperty().negate().add(item.getInsets().getLeft()));
        }
      }

      for (Node check : popupContent.lookupAll("." + ITEM_CHECK_STYLE_CLASS)) {
        Node graphic = check.getParent();
        if (graphic == null || !(graphic.getParent() instanceof Region item)) {
          continue;
        }
        graphic.translateXProperty().unbind();
        graphic
          .translateXProperty()
          .bind(
            item
              .widthProperty()
              .subtract(graphic.layoutXProperty())
              .subtract(graphic.getLayoutBounds().getWidth())
              .subtract(item.getInsets().getRight())
          );
      }
    }

    private static boolean isOpenButton(Node node) {
      return node.getStyleClass().contains("open-button");
    }
  }
}
