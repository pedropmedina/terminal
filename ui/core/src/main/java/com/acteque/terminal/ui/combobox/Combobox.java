package com.acteque.terminal.ui.combobox;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.NodeOrientation;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.TextInputControl;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.util.Duration;
import javafx.util.StringConverter;

/** Shared state and popup behavior for the composable combobox component family. */
public final class Combobox<T> {

  private static final double SCREEN_PADDING = 8.0;
  private static final Duration CLOSE_DURATION = Duration.millis(100.0);

  private final Popup popup = new Popup();
  private final ListProperty<T> items = new SimpleListProperty<>(this, "items", FXCollections.observableArrayList());
  private final ObservableList<T> selectedItems = FXCollections.observableArrayList();
  private final ObjectProperty<T> value = new SimpleObjectProperty<>(this, "value");
  private final StringProperty inputValue = new SimpleStringProperty(this, "inputValue", "");
  private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
  private final BooleanProperty multiple = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      applySelectionMode();
      refreshSelection();
    }

    @Override
    public Object getBean() {
      return Combobox.this;
    }

    @Override
    public String getName() {
      return "multiple";
    }
  };
  private final ObjectProperty<StringConverter<T>> converter = new ObjectPropertyBase<>(defaultConverter()) {
    @Override
    public void set(StringConverter<T> next) {
      super.set(Objects.requireNonNull(next, "converter cannot be null"));
    }

    @Override
    protected void invalidated() {
      refreshItems();
    }

    @Override
    public Object getBean() {
      return Combobox.this;
    }

    @Override
    public String getName() {
      return "converter";
    }
  };
  private final ObjectProperty<BiPredicate<T, String>> filter = new SimpleObjectProperty<>(
    this,
    "filter",
    this::containsIgnoreCase
  );
  private final ObjectProperty<BiPredicate<T, T>> itemEquality = new ObjectPropertyBase<>(Objects::equals) {
    @Override
    public void set(BiPredicate<T, T> next) {
      super.set(Objects.requireNonNull(next, "itemEquality cannot be null"));
    }

    @Override
    protected void invalidated() {
      refreshSelection();
    }

    @Override
    public Object getBean() {
      return Combobox.this;
    }

    @Override
    public String getName() {
      return "itemEquality";
    }
  };
  private final ObjectProperty<Predicate<T>> itemDisabled = new ObjectPropertyBase<>(ignored -> false) {
    @Override
    public void set(Predicate<T> next) {
      super.set(Objects.requireNonNull(next, "itemDisabled cannot be null"));
    }

    @Override
    protected void invalidated() {
      refreshItems();
    }

    @Override
    public Object getBean() {
      return Combobox.this;
    }

    @Override
    public String getName() {
      return "itemDisabled";
    }
  };
  private final IntegerProperty limit = new SimpleIntegerProperty(this, "limit", -1) {
    @Override
    public void set(int next) {
      if (next < -1) {
        throw new IllegalArgumentException("limit must be -1 or non-negative");
      }
      super.set(next);
    }
  };
  private final List<ComboboxItem<T>> registeredItems = new ArrayList<>();
  private final List<ComboboxGroup<T>> registeredGroups = new ArrayList<>();
  private final List<TextInputControl> editors = new ArrayList<>();
  private final List<Node> anchors = new ArrayList<>();
  private final EventHandler<MouseEvent> ownerMouseHandler = this::ownerMousePressed;
  private final EventHandler<ScrollEvent> ownerScrollHandler = ignored -> hide();
  private final EventHandler<KeyEvent> ownerKeyHandler = this::ownerKeyPressed;
  private final PauseTransition closeDelay = new PauseTransition(CLOSE_DURATION);
  private ComboboxContent<T> content;
  private ComboboxItem<T> highlightedItem;
  private Window owner;
  private boolean synchronizingSelection;
  private boolean synchronizingEditors;
  private boolean synchronizingPopup;

  /** Creates an empty combobox root. */
  public Combobox() {
    this(FXCollections.observableArrayList());
  }

  /**
   * Creates a combobox root backed by the supplied items.
   *
   * @param items the observable source items
   */
  public Combobox(ObservableList<T> items) {
    popup.setAutoFix(false);
    popup.setAutoHide(false);
    popup.setHideOnEscape(false);
    popup.setOnHidden(ignored -> popupHidden());
    closeDelay.setOnFinished(ignored -> finishHide());
    this.items.addListener((InvalidationListener) ignored -> refreshItems());
    selectedItems.addListener((ListChangeListener<T>) ignored -> selectedItemsChanged());
    value.addListener(ignored -> valueChanged());
    inputValue.addListener(ignored -> {
      updateEditorText();
      refreshItems();
    });
    filter.addListener(ignored -> refreshItems());
    limit.addListener(ignored -> refreshItems());
    open.addListener((ignored, wasOpen, isOpen) -> applyOpenState(isOpen));
    setItems(items);
  }

  /** Returns the source items property. */
  public ListProperty<T> itemsProperty() {
    return items;
  }

  /** Returns the observable source items. */
  public ObservableList<T> getItems() {
    return items.get();
  }

  /** Replaces the observable source items. */
  public void setItems(ObservableList<T> next) {
    items.set(Objects.requireNonNull(next, "items cannot be null"));
  }

  /** Returns the single selected value property. */
  public ObjectProperty<T> valueProperty() {
    return value;
  }

  /** Returns the single selected value. */
  public T getValue() {
    return value.get();
  }

  /** Sets the single selected value. */
  public void setValue(T next) {
    value.set(next);
  }

  /** Returns the mutable selected-items list used by multiple mode. */
  public ObservableList<T> getSelectedItems() {
    return selectedItems;
  }

  /** Returns whether multiple values may be selected. */
  public boolean isMultiple() {
    return multiple.get();
  }

  /** Sets whether multiple values may be selected. */
  public void setMultiple(boolean next) {
    multiple.set(next);
  }

  /** Returns the multiple-selection property. */
  public BooleanProperty multipleProperty() {
    return multiple;
  }

  /** Returns the current input text. */
  public String getInputValue() {
    return inputValue.get();
  }

  /** Sets the current input text. */
  public void setInputValue(String next) {
    inputValue.set(next == null ? "" : next);
  }

  /** Returns the input-text property. */
  public StringProperty inputValueProperty() {
    return inputValue;
  }

  /** Returns whether the popup is requested open. */
  public boolean isOpen() {
    return open.get();
  }

  /** Sets whether the popup is requested open. */
  public void setOpen(boolean next) {
    open.set(next);
  }

  /** Returns the popup-open property. */
  public BooleanProperty openProperty() {
    return open;
  }

  /** Returns the item string converter. */
  public StringConverter<T> getConverter() {
    return converter.get();
  }

  /** Sets the item string converter. */
  public void setConverter(StringConverter<T> next) {
    converter.set(Objects.requireNonNull(next, "converter cannot be null"));
  }

  /** Returns the converter property. */
  public ObjectProperty<StringConverter<T>> converterProperty() {
    return converter;
  }

  /** Returns the item filter, or {@code null} when filtering is caller-controlled. */
  public BiPredicate<T, String> getFilter() {
    return filter.get();
  }

  /** Sets the item filter; {@code null} disables internal filtering. */
  public void setFilter(BiPredicate<T, String> next) {
    filter.set(next);
  }

  /** Returns the filter property. */
  public ObjectProperty<BiPredicate<T, String>> filterProperty() {
    return filter;
  }

  /** Returns the item equality predicate. */
  public BiPredicate<T, T> getItemEquality() {
    return itemEquality.get();
  }

  /** Sets the item equality predicate. */
  public void setItemEquality(BiPredicate<T, T> next) {
    itemEquality.set(Objects.requireNonNull(next, "itemEquality cannot be null"));
  }

  /** Returns the item equality property. */
  public ObjectProperty<BiPredicate<T, T>> itemEqualityProperty() {
    return itemEquality;
  }

  /** Returns the predicate used to disable values. */
  public Predicate<T> getItemDisabled() {
    return itemDisabled.get();
  }

  /** Sets the predicate used to disable values. */
  public void setItemDisabled(Predicate<T> next) {
    itemDisabled.set(Objects.requireNonNull(next, "itemDisabled cannot be null"));
  }

  /** Returns the disabled-item predicate property. */
  public ObjectProperty<Predicate<T>> itemDisabledProperty() {
    return itemDisabled;
  }

  /** Returns the result limit, where -1 means unlimited. */
  public int getLimit() {
    return limit.get();
  }

  /** Sets the result limit, where -1 means unlimited. */
  public void setLimit(int next) {
    limit.set(next);
  }

  /** Returns the result-limit property. */
  public IntegerProperty limitProperty() {
    return limit;
  }

  /** Opens the popup when the registered content and anchor are attached to a showing window. */
  public void show() {
    setOpen(true);
  }

  /** Closes the popup. */
  public void hide() {
    setOpen(false);
  }

  /** Toggles the popup. */
  public void toggle() {
    setOpen(!isOpen());
  }

  /** Clears the selection and query. */
  public void clear() {
    synchronizingSelection = true;
    try {
      value.set(null);
      selectedItems.clear();
    } finally {
      synchronizingSelection = false;
    }
    setInputValue("");
    refreshSelection();
  }

  /** Returns the underlying popup for window-level integration and tests. */
  public Popup getPopup() {
    return popup;
  }

  /** Installs the popup content owned by this root. */
  void setContent(ComboboxContent<T> next) {
    if (content == next) {
      return;
    }
    if (content != null) {
      popup.getContent().remove(content);
    }
    content = next;
    popup.getContent().clear();
    if (next != null) {
      popup.getContent().add(next);
    }
    refreshItems();
    if (isOpen()) {
      showPopup();
    }
  }

  /** Recomputes popup geometry when a positioning property changes. */
  void reposition() {
    if (popup.isShowing()) {
      showPopup();
    }
  }

  /** Registers an item for filtering, selection, and keyboard navigation. */
  void registerItem(ComboboxItem<T> item) {
    if (!registeredItems.contains(item)) {
      registeredItems.add(item);
      refreshItems();
    }
  }

  /** Removes an item from filtering, selection, and keyboard navigation. */
  void unregisterItem(ComboboxItem<T> item) {
    registeredItems.remove(item);
    if (highlightedItem == item) {
      setHighlightedItem(null);
    }
    refreshItems();
  }

  /** Registers a group whose visibility follows its filtered items. */
  void registerGroup(ComboboxGroup<T> group) {
    if (!registeredGroups.contains(group)) {
      registeredGroups.add(group);
    }
  }

  /** Registers an editor and its default popup anchor. */
  void registerEditor(TextInputControl editor, Node anchor) {
    Objects.requireNonNull(editor, "editor cannot be null");
    Objects.requireNonNull(anchor, "anchor cannot be null");
    if (!editors.contains(editor)) {
      editors.add(editor);
      editor.setText(getInputValue());
      editor.textProperty().addListener((ignored, previous, next) -> editorTextChanged(editor, next));
      editor.addEventFilter(KeyEvent.KEY_PRESSED, this::editorKeyPressed);
      editor.addEventHandler(MouseEvent.MOUSE_PRESSED, ignored -> {
        if (!editor.isDisabled()) {
          show();
        }
      });
    }
    if (!anchors.contains(anchor)) {
      anchors.add(anchor);
    }
  }

  /** Applies user selection semantics to an item value. */
  void select(T item) {
    if (item == null || getItemDisabled().test(item)) {
      return;
    }
    if (isMultiple()) {
      int selectedIndex = indexOfSelected(item);
      if (selectedIndex >= 0) {
        selectedItems.remove(selectedIndex);
      } else {
        selectedItems.add(item);
      }
      setInputValue("");
      show();
    } else {
      setValue(item);
      setInputValue(labelFor(item));
      hide();
    }
  }

  /** Makes an enabled visible item the current keyboard highlight. */
  void highlight(ComboboxItem<T> item) {
    if (item != null && item.isVisible() && !item.isDisabled()) {
      setHighlightedItem(item);
    }
  }

  /** Returns whether an item matches the current single or multiple selection. */
  boolean isSelected(T item) {
    if (isMultiple()) {
      return indexOfSelected(item) >= 0;
    }
    return getValue() != null && getItemEquality().test(getValue(), item);
  }

  /** Returns the non-null display label produced by the converter. */
  String labelFor(T item) {
    String label = getConverter().toString(item);
    return label == null ? "" : label;
  }

  /** Removes an item from the current selection. */
  void removeSelected(T item) {
    int index = indexOfSelected(item);
    if (index >= 0) {
      selectedItems.remove(index);
    } else if (!isMultiple() && getValue() != null && getItemEquality().test(getValue(), item)) {
      clear();
    }
  }

  /** Normalizes selection when switching between single and multiple modes. */
  private void applySelectionMode() {
    synchronizingSelection = true;
    try {
      if (isMultiple()) {
        if (value.get() != null && indexOfSelected(value.get()) < 0) {
          selectedItems.add(value.get());
        }
        value.set(null);
      } else {
        T retained = selectedItems.isEmpty() ? value.get() : selectedItems.getFirst();
        selectedItems.setAll(retained == null ? List.of() : List.of(retained));
        value.set(retained);
        setInputValue(retained == null ? "" : labelFor(retained));
      }
    } finally {
      synchronizingSelection = false;
    }
  }

  /** Mirrors a single-value change into selected items and editor text. */
  private void valueChanged() {
    if (synchronizingSelection || isMultiple()) {
      return;
    }
    synchronizingSelection = true;
    try {
      selectedItems.setAll(value.get() == null ? List.of() : List.of(value.get()));
    } finally {
      synchronizingSelection = false;
    }
    setInputValue(value.get() == null ? "" : labelFor(value.get()));
    refreshSelection();
  }

  /** Normalizes externally changed selected items and refreshes item state. */
  private void selectedItemsChanged() {
    if (synchronizingSelection) {
      return;
    }
    synchronizingSelection = true;
    try {
      if (!isMultiple()) {
        T retained = selectedItems.isEmpty() ? null : selectedItems.getFirst();
        if (selectedItems.size() > 1) {
          selectedItems.clear();
          if (retained != null) {
            selectedItems.add(retained);
          }
        }
        value.set(retained);
      }
    } finally {
      synchronizingSelection = false;
    }
    refreshSelection();
  }

  /** Returns the equality-aware selected index, or -1 when absent. */
  private int indexOfSelected(T item) {
    for (int index = 0; index < selectedItems.size(); index++) {
      if (getItemEquality().test(selectedItems.get(index), item)) {
        return index;
      }
    }
    return -1;
  }

  /** Reapplies filtering, limits, disabled state, groups, and empty state. */
  private void refreshItems() {
    int remaining = getLimit();
    String query = getInputValue();
    for (ComboboxItem<T> item : List.copyOf(registeredItems)) {
      boolean matches = getFilter() == null || getFilter().test(item.getItem(), query);
      boolean withinLimit = remaining < 0 || remaining > 0;
      boolean shown = matches && withinLimit;
      item.setFilteredVisible(shown);
      item.setItemDisabled(getItemDisabled().test(item.getItem()));
      if (shown && remaining > 0) {
        remaining--;
      }
      item.refreshText();
    }
    registeredGroups.forEach(group -> group.refreshVisibility());
    if (highlightedItem != null && (!highlightedItem.isVisible() || highlightedItem.isDisabled())) {
      setHighlightedItem(null);
    }
    if (content != null) {
      content.setEmpty(visibleItems().isEmpty());
    }
  }

  /** Refreshes selected presentation on registered item nodes. */
  private void refreshSelection() {
    registeredItems.forEach(ComboboxItem::refreshSelection);
  }

  /** Implements the default case-insensitive substring filter. */
  private boolean containsIgnoreCase(T item, String query) {
    if (query == null || query.isBlank()) {
      return true;
    }
    return labelFor(item).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT));
  }

  /** Publishes user-entered text and opens the popup. */
  private void editorTextChanged(TextInputControl editor, String next) {
    if (synchronizingEditors || editor.isDisabled()) {
      return;
    }
    setInputValue(next);
    show();
  }

  /** Mirrors root input text into every registered editor without recursion. */
  private void updateEditorText() {
    synchronizingEditors = true;
    try {
      for (TextInputControl editor : editors) {
        if (!Objects.equals(editor.getText(), getInputValue())) {
          editor.setText(getInputValue());
        }
      }
    } finally {
      synchronizingEditors = false;
    }
  }

  /** Handles combobox navigation and selection keys from an editor. */
  private void editorKeyPressed(KeyEvent event) {
    switch (event.getCode()) {
      case DOWN -> {
        show();
        moveHighlight(1);
        event.consume();
      }
      case UP -> {
        show();
        moveHighlight(-1);
        event.consume();
      }
      case HOME -> {
        highlightBoundary(true);
        event.consume();
      }
      case END -> {
        highlightBoundary(false);
        event.consume();
      }
      case ENTER -> {
        if (highlightedItem != null) {
          select(highlightedItem.getItem());
          event.consume();
        }
      }
      case ESCAPE -> {
        if (isOpen()) {
          hide();
          event.consume();
        }
      }
      case TAB -> hide();
      case BACK_SPACE -> {
        if (isMultiple() && getInputValue().isEmpty() && !selectedItems.isEmpty()) {
          selectedItems.removeLast();
          event.consume();
        }
      }
      default -> {
        // Text input handles all other keys.
      }
    }
  }

  /** Moves the highlight cyclically through enabled visible items. */
  private void moveHighlight(int direction) {
    List<ComboboxItem<T>> visible = visibleEnabledItems();
    if (visible.isEmpty()) {
      setHighlightedItem(null);
      return;
    }
    int current = visible.indexOf(highlightedItem);
    int next =
      current < 0 ? (direction > 0 ? 0 : visible.size() - 1) : Math.floorMod(current + direction, visible.size());
    setHighlightedItem(visible.get(next));
  }

  /** Highlights the first or last enabled visible item. */
  private void highlightBoundary(boolean first) {
    List<ComboboxItem<T>> visible = visibleEnabledItems();
    setHighlightedItem(visible.isEmpty() ? null : visible.get(first ? 0 : visible.size() - 1));
  }

  /** Updates the sole highlighted item and its pseudo-class state. */
  private void setHighlightedItem(ComboboxItem<T> next) {
    if (highlightedItem != null) {
      highlightedItem.setHighlighted(false);
    }
    highlightedItem = next;
    if (highlightedItem != null) {
      highlightedItem.setHighlighted(true);
    }
  }

  /** Returns registered items currently included by filtering and limits. */
  private List<ComboboxItem<T>> visibleItems() {
    return registeredItems.stream().filter(Node::isVisible).toList();
  }

  /** Returns navigable registered items in registration order. */
  private List<ComboboxItem<T>> visibleEnabledItems() {
    return registeredItems
      .stream()
      .filter(Node::isVisible)
      .filter(item -> !item.isDisabled())
      .toList();
  }

  /** Applies requested popup state, including the closing transition delay. */
  private void applyOpenState(boolean requestedOpen) {
    if (synchronizingPopup) {
      return;
    }
    if (requestedOpen) {
      showPopup();
    } else if (popup.isShowing()) {
      content.setPhase(ComboboxContent.Phase.CLOSING);
      closeDelay.playFromStart();
    } else if (content != null) {
      content.setPhase(ComboboxContent.Phase.CLOSED);
    }
  }

  /** Configures, positions, and displays the popup when prerequisites exist. */
  private void showPopup() {
    Node anchor = resolvedAnchor();
    Scene scene = anchor == null ? null : anchor.getScene();
    Window nextOwner = scene == null ? null : scene.getWindow();
    if (content == null || anchor == null || nextOwner == null || !nextOwner.isShowing()) {
      return;
    }
    closeDelay.stop();
    copyStyles(scene);
    content.setPhase(ComboboxContent.Phase.OPENING);
    configureContentSize(anchor);
    content.applyCss();
    content.autosize();
    PopupPosition position = popupPosition(anchor);
    if (position == null) {
      return;
    }
    if (!popup.isShowing()) {
      owner = nextOwner;
      popup.show(owner, position.x(), position.y());
      attachOwnerFilters();
    } else {
      popup.setX(position.x());
      popup.setY(position.y());
    }
    Platform.runLater(() -> {
      if (isOpen() && popup.isShowing()) {
        content.setPhase(ComboboxContent.Phase.OPEN);
      }
    });
  }

  /** Hides the native popup after its closing transition. */
  private void finishHide() {
    if (popup.isShowing()) {
      popup.hide();
    } else {
      popupHidden();
    }
  }

  /** Cleans up filters and state after the native popup is hidden. */
  private void popupHidden() {
    closeDelay.stop();
    detachOwnerFilters();
    if (content != null) {
      content.setPhase(ComboboxContent.Phase.CLOSED);
    }
    setHighlightedItem(null);
    owner = null;
    if (isOpen()) {
      synchronizingPopup = true;
      setOpen(false);
      synchronizingPopup = false;
    }
  }

  /** Returns the explicit content anchor or first registered input anchor. */
  private Node resolvedAnchor() {
    if (content != null && content.getAnchor() != null) {
      return content.getAnchor();
    }
    return anchors.isEmpty() ? null : anchors.getFirst();
  }

  /** Applies the shadcn anchor width and minimum-width rules. */
  private void configureContentSize(Node anchor) {
    Bounds bounds = anchor.localToScreen(anchor.getBoundsInLocal());
    if (bounds == null) {
      return;
    }
    double width = Math.max(
      bounds.getWidth(),
      content.getAnchor() == null ? bounds.getWidth() + 28.0 : bounds.getWidth()
    );
    content.setMinWidth(width);
    content.setPrefWidth(width);
  }

  /** Calculates clamped popup coordinates and the resolved physical side. */
  private PopupPosition popupPosition(Node anchor) {
    Bounds anchorBounds = anchor.localToScreen(anchor.getBoundsInLocal());
    if (anchorBounds == null) {
      return null;
    }
    Rectangle2D screen = screenFor(anchorBounds).getVisualBounds();
    double width = Math.min(content.prefWidth(-1.0), screen.getWidth() - SCREEN_PADDING * 2.0);
    content.setMaxWidth(width);
    double availableHeight = screen.getHeight() - SCREEN_PADDING * 2.0;
    content.setAvailableHeight(availableHeight);
    double height = Math.min(content.prefHeight(width), availableHeight);
    ComboboxContent.Side side = physicalSide(anchor, content.getSide());
    if (!fits(side, anchorBounds, width, height, content.getSideOffset(), screen)) {
      ComboboxContent.Side opposite = opposite(side);
      if (fits(opposite, anchorBounds, width, height, content.getSideOffset(), screen)) {
        side = opposite;
      }
    }
    double x;
    double y;
    if (side == ComboboxContent.Side.TOP || side == ComboboxContent.Side.BOTTOM) {
      x = alignedStart(anchorBounds.getMinX(), anchorBounds.getWidth(), width, content.getAlign());
      x += content.getAlignOffset();
      y =
        side == ComboboxContent.Side.BOTTOM
          ? anchorBounds.getMaxY() + content.getSideOffset()
          : anchorBounds.getMinY() - height - content.getSideOffset();
    } else {
      x =
        side == ComboboxContent.Side.RIGHT
          ? anchorBounds.getMaxX() + content.getSideOffset()
          : anchorBounds.getMinX() - width - content.getSideOffset();
      y = alignedStart(anchorBounds.getMinY(), anchorBounds.getHeight(), height, content.getAlign());
      y += content.getAlignOffset();
    }
    content.setResolvedSide(side);
    return new PopupPosition(
      clamp(x, screen.getMinX() + SCREEN_PADDING, screen.getMaxX() - width - SCREEN_PADDING),
      clamp(y, screen.getMinY() + SCREEN_PADDING, screen.getMaxY() - height - SCREEN_PADDING)
    );
  }

  /** Copies owner stylesheets and theme state into the popup scene. */
  private void copyStyles(Scene scene) {
    Scene popupScene = popup.getScene();
    for (String stylesheet : scene.getStylesheets()) {
      if (!content.getStylesheets().contains(stylesheet)) {
        content.getStylesheets().add(stylesheet);
      }
      if (!popupScene.getStylesheets().contains(stylesheet)) {
        popupScene.getStylesheets().add(stylesheet);
      }
    }
    String themeClass = scene.getRoot().getStyleClass().contains("theme-dark") ? "theme-dark" : "theme-light";
    content.getStyleClass().removeAll("theme-light", "theme-dark");
    content.getStyleClass().add(themeClass);
    popupScene.getRoot().getStyleClass().removeAll("theme-light", "theme-dark");
    popupScene.getRoot().getStyleClass().add(themeClass);
  }

  /** Installs owner-window dismissal filters while the popup is visible. */
  private void attachOwnerFilters() {
    if (owner == null) {
      return;
    }
    owner.addEventFilter(MouseEvent.MOUSE_PRESSED, ownerMouseHandler);
    owner.addEventFilter(ScrollEvent.SCROLL, ownerScrollHandler);
    owner.addEventFilter(KeyEvent.KEY_PRESSED, ownerKeyHandler);
  }

  /** Removes owner-window dismissal filters. */
  private void detachOwnerFilters() {
    if (owner == null) {
      return;
    }
    owner.removeEventFilter(MouseEvent.MOUSE_PRESSED, ownerMouseHandler);
    owner.removeEventFilter(ScrollEvent.SCROLL, ownerScrollHandler);
    owner.removeEventFilter(KeyEvent.KEY_PRESSED, ownerKeyHandler);
  }

  /** Dismisses the popup for owner clicks outside its anchor. */
  private void ownerMousePressed(MouseEvent event) {
    if (!isDescendant(event.getTarget(), resolvedAnchor())) {
      hide();
    }
  }

  /** Dismisses the popup for Escape received by the owner window. */
  private void ownerKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE && isOpen()) {
      hide();
      event.consume();
    }
  }

  /** Returns whether an event target belongs to an anchor subtree. */
  private static boolean isDescendant(Object target, Node ancestor) {
    if (!(target instanceof Node node) || ancestor == null) {
      return false;
    }
    for (Node current = node; current != null; current = current.getParent()) {
      if (current == ancestor) {
        return true;
      }
    }
    return false;
  }

  /** Resolves logical inline sides using the anchor orientation. */
  private static ComboboxContent.Side physicalSide(Node anchor, ComboboxContent.Side side) {
    if (side == ComboboxContent.Side.INLINE_START) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
        ? ComboboxContent.Side.RIGHT
        : ComboboxContent.Side.LEFT;
    }
    if (side == ComboboxContent.Side.INLINE_END) {
      return anchor.getEffectiveNodeOrientation() == NodeOrientation.RIGHT_TO_LEFT
        ? ComboboxContent.Side.LEFT
        : ComboboxContent.Side.RIGHT;
    }
    return side;
  }

  /** Returns whether popup geometry fits on a requested physical side. */
  private static boolean fits(
    ComboboxContent.Side side,
    Bounds anchor,
    double width,
    double height,
    double offset,
    Rectangle2D screen
  ) {
    return switch (side) {
      case TOP -> anchor.getMinY() - height - offset >= screen.getMinY() + SCREEN_PADDING;
      case BOTTOM -> anchor.getMaxY() + height + offset <= screen.getMaxY() - SCREEN_PADDING;
      case LEFT -> anchor.getMinX() - width - offset >= screen.getMinX() + SCREEN_PADDING;
      case RIGHT -> anchor.getMaxX() + width + offset <= screen.getMaxX() - SCREEN_PADDING;
      case INLINE_START, INLINE_END -> throw new IllegalArgumentException("Expected a physical side");
    };
  }

  /** Returns the opposite physical side. */
  private static ComboboxContent.Side opposite(ComboboxContent.Side side) {
    return switch (side) {
      case TOP -> ComboboxContent.Side.BOTTOM;
      case BOTTOM -> ComboboxContent.Side.TOP;
      case LEFT -> ComboboxContent.Side.RIGHT;
      case RIGHT -> ComboboxContent.Side.LEFT;
      case INLINE_START, INLINE_END -> throw new IllegalArgumentException("Expected a physical side");
    };
  }

  /** Returns the aligned popup start coordinate. */
  private static double alignedStart(
    double anchorStart,
    double anchorSize,
    double popupSize,
    ComboboxContent.Align align
  ) {
    return switch (align) {
      case START -> anchorStart;
      case CENTER -> anchorStart + (anchorSize - popupSize) / 2.0;
      case END -> anchorStart + anchorSize - popupSize;
    };
  }

  /** Clamps a coordinate to the supplied inclusive range. */
  private static double clamp(double value, double minimum, double maximum) {
    return Math.max(minimum, Math.min(value, Math.max(minimum, maximum)));
  }

  /** Returns the screen containing the anchor bounds. */
  private static Screen screenFor(Bounds bounds) {
    List<Screen> screens = Screen.getScreensForRectangle(
      bounds.getMinX(),
      bounds.getMinY(),
      bounds.getWidth(),
      bounds.getHeight()
    );
    return screens.isEmpty() ? Screen.getPrimary() : screens.getFirst();
  }

  /** Creates the default display-only item converter. */
  private static <T> StringConverter<T> defaultConverter() {
    return new StringConverter<>() {
      @Override
      public String toString(T value) {
        return value == null ? "" : value.toString();
      }

      @Override
      public T fromString(String value) {
        throw new UnsupportedOperationException("Combobox does not create arbitrary values");
      }
    };
  }

  /** Calculated popup screen coordinates. */
  private record PopupPosition(double x, double y) {}
}
