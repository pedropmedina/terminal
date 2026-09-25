package com.acteque.terminal.ui.command;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiPredicate;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;

/** Searchable command-menu root that coordinates filtering, highlighting, and invocation. */
public final class Command extends VBox {

  private final StringProperty searchText = new SimpleStringProperty(this, "searchText", "");
  private final ObjectProperty<BiPredicate<CommandItem, String>> filter = new SimpleObjectProperty<>(
    this,
    "filter",
    this::matchesValueOrKeyword
  );
  private final BooleanProperty loop = new SimpleBooleanProperty(this, "loop", false);
  private final ReadOnlyObjectWrapper<CommandItem> highlightedItem = new ReadOnlyObjectWrapper<>(
    this,
    "highlightedItem"
  );
  private final List<CommandItem> registeredItems = new ArrayList<>();
  private final List<CommandGroup> registeredGroups = new ArrayList<>();
  private final List<CommandEmpty> registeredEmptyStates = new ArrayList<>();
  private final List<CommandList> registeredLists = new ArrayList<>();
  private final List<CommandSeparator> registeredSeparators = new ArrayList<>();

  /** Creates an empty command root. */
  public Command() {
    this(new Node[0]);
  }

  /**
   * Creates a command root containing the supplied composed children.
   *
   * @param children the command input, list, or other composed content
   */
  public Command(Node... children) {
    getStyleClass().add("core-command");
    setAccessibleRole(AccessibleRole.COMBO_BOX);
    setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
    getChildren().addAll(children);

    Rectangle clip = new Rectangle();
    clip.widthProperty().bind(widthProperty());
    clip.heightProperty().bind(heightProperty());
    clip.setArcWidth(24.0);
    clip.setArcHeight(24.0);
    setClip(clip);

    searchText.addListener(ignored -> refreshItems());
    filter.addListener(ignored -> refreshItems());
    addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
  }

  /** Returns the current search text. */
  public String getSearchText() {
    return searchText.get();
  }

  /**
   * Replaces the current search text.
   *
   * @param value the new search text; {@code null} is treated as an empty string
   */
  public void setSearchText(String value) {
    searchText.set(value == null ? "" : value);
  }

  /** Returns the search-text property. */
  public StringProperty searchTextProperty() {
    return searchText;
  }

  /** Returns the item filter, or {@code null} when automatic filtering is disabled. */
  public BiPredicate<CommandItem, String> getFilter() {
    return filter.get();
  }

  /**
   * Sets the item filter; {@code null} disables automatic filtering.
   *
   * @param value the item-and-query predicate, or {@code null}
   */
  public void setFilter(BiPredicate<CommandItem, String> value) {
    filter.set(value);
  }

  /** Returns the filter property. */
  public ObjectProperty<BiPredicate<CommandItem, String>> filterProperty() {
    return filter;
  }

  /** Returns whether keyboard navigation wraps at list boundaries. */
  public boolean isLoop() {
    return loop.get();
  }

  /**
   * Sets whether keyboard navigation wraps at list boundaries.
   *
   * @param value {@code true} to wrap from the last item to the first and conversely
   */
  public void setLoop(boolean value) {
    loop.set(value);
  }

  /** Returns the loop-navigation property. */
  public BooleanProperty loopProperty() {
    return loop;
  }

  /** Returns the currently highlighted item, or {@code null} when none is available. */
  public CommandItem getHighlightedItem() {
    return highlightedItem.get();
  }

  /** Returns the read-only highlighted-item property. */
  public ReadOnlyObjectProperty<CommandItem> highlightedItemProperty() {
    return highlightedItem.getReadOnlyProperty();
  }

  /** Returns accessible command text state. */
  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (attribute == AccessibleAttribute.TEXT) {
      return getSearchText();
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  /**
   * Registers an item for filtering and navigation.
   *
   * @param item the item to register
   */
  void registerItem(CommandItem item) {
    if (!registeredItems.contains(item)) {
      registeredItems.add(item);
      refreshItems();
    }
  }

  /**
   * Removes an item from filtering and navigation.
   *
   * @param item the item to unregister
   */
  void unregisterItem(CommandItem item) {
    registeredItems.remove(item);
    if (getHighlightedItem() == item) {
      setHighlightedItem(null);
    }
    refreshItems();
  }

  /**
   * Registers a group for derived visibility.
   *
   * @param group the group to register
   */
  void registerGroup(CommandGroup group) {
    if (!registeredGroups.contains(group)) {
      registeredGroups.add(group);
      refreshItems();
    }
  }

  /**
   * Removes a group from derived visibility.
   *
   * @param group the group to unregister
   */
  void unregisterGroup(CommandGroup group) {
    registeredGroups.remove(group);
  }

  /**
   * Registers an empty-state label.
   *
   * @param emptyState the empty state to register
   */
  void registerEmptyState(CommandEmpty emptyState) {
    if (!registeredEmptyStates.contains(emptyState)) {
      registeredEmptyStates.add(emptyState);
      refreshItems();
    }
  }

  /**
   * Removes an empty-state label.
   *
   * @param emptyState the empty state to unregister
   */
  void unregisterEmptyState(CommandEmpty emptyState) {
    registeredEmptyStates.remove(emptyState);
  }

  /**
   * Registers a scrollable list for highlighted-item reveal behavior.
   *
   * @param list the list to register
   */
  void registerList(CommandList list) {
    if (!registeredLists.contains(list)) {
      registeredLists.add(list);
    }
  }

  /**
   * Removes a scrollable list.
   *
   * @param list the list to unregister
   */
  void unregisterList(CommandList list) {
    registeredLists.remove(list);
  }

  /**
   * Registers a separator whose visibility follows search state.
   *
   * @param separator the separator to register
   */
  void registerSeparator(CommandSeparator separator) {
    if (!registeredSeparators.contains(separator)) {
      registeredSeparators.add(separator);
      refreshItems();
    }
  }

  /**
   * Removes a separator.
   *
   * @param separator the separator to unregister
   */
  void unregisterSeparator(CommandSeparator separator) {
    registeredSeparators.remove(separator);
  }

  /**
   * Re-evaluates filtering after item metadata or disabled state changes.
   */
  void refreshItems() {
    String query = getSearchText().strip();
    BiPredicate<CommandItem, String> itemFilter = getFilter();
    int visibleCount = 0;
    for (CommandItem item : registeredItems) {
      boolean visible = query.isEmpty() || itemFilter == null || itemFilter.test(item, query);
      item.setFilteredVisible(visible);
      if (visible) {
        visibleCount++;
      }
    }

    registeredGroups.forEach(CommandGroup::refreshVisibility);
    boolean empty = visibleCount == 0;
    registeredEmptyStates.forEach(emptyState -> emptyState.setEmpty(empty));
    registeredSeparators.forEach(separator -> separator.setFiltering(!query.isEmpty()));

    CommandItem current = getHighlightedItem();
    if (current == null || !current.isFilteredVisible() || current.isDisabled()) {
      setHighlightedItem(firstEnabledVisibleItem());
    }
  }

  /**
   * Applies pointer-driven highlighting to an enabled visible item.
   *
   * @param item the item under the pointer
   */
  void highlight(CommandItem item) {
    if (item.isFilteredVisible() && !item.isDisabled()) {
      setHighlightedItem(item);
    }
  }

  /**
   * Implements the default stable, case-insensitive value and keyword match.
   *
   * @param item the candidate item
   * @param query the non-empty search query
   * @return {@code true} when the value or a keyword contains the query
   */
  private boolean matchesValueOrKeyword(CommandItem item, String query) {
    String normalizedQuery = query.toLowerCase(Locale.ROOT);
    if (item.getValue().toLowerCase(Locale.ROOT).contains(normalizedQuery)) {
      return true;
    }
    return item
      .getKeywords()
      .stream()
      .anyMatch(keyword -> keyword.toLowerCase(Locale.ROOT).contains(normalizedQuery));
  }

  /**
   * Handles command navigation while focus remains inside the command root.
   *
   * @param event the key event to inspect
   */
  private void handleKeyPressed(KeyEvent event) {
    KeyCode code = event.getCode();
    switch (code) {
      case DOWN -> moveHighlight(1);
      case UP -> moveHighlight(-1);
      case HOME -> highlightBoundary(true);
      case END -> highlightBoundary(false);
      case ENTER -> invokeHighlightedItem();
      default -> {
        return;
      }
    }
    event.consume();
  }

  /**
   * Moves the highlight by one enabled visible item.
   *
   * @param direction positive for forward and negative for backward
   */
  private void moveHighlight(int direction) {
    List<CommandItem> navigable = enabledVisibleItems();
    if (navigable.isEmpty()) {
      setHighlightedItem(null);
      return;
    }

    int currentIndex = navigable.indexOf(getHighlightedItem());
    int nextIndex;
    if (currentIndex < 0) {
      nextIndex = direction > 0 ? 0 : navigable.size() - 1;
    } else {
      nextIndex = currentIndex + direction;
      if (isLoop()) {
        nextIndex = Math.floorMod(nextIndex, navigable.size());
      } else {
        nextIndex = Math.max(0, Math.min(navigable.size() - 1, nextIndex));
      }
    }
    setHighlightedItem(navigable.get(nextIndex));
  }

  /**
   * Highlights the first or last enabled visible item.
   *
   * @param first {@code true} for the first item; {@code false} for the last
   */
  private void highlightBoundary(boolean first) {
    List<CommandItem> navigable = enabledVisibleItems();
    setHighlightedItem(navigable.isEmpty() ? null : first ? navigable.getFirst() : navigable.getLast());
  }

  /** Fires the highlighted item when it remains enabled and visible. */
  private void invokeHighlightedItem() {
    CommandItem item = getHighlightedItem();
    if (item != null && item.isFilteredVisible() && !item.isDisabled()) {
      item.fire();
    }
  }

  /**
   * Returns the enabled visible items in declaration order.
   *
   * @return a snapshot used by keyboard navigation
   */
  private List<CommandItem> enabledVisibleItems() {
    return registeredItems
      .stream()
      .filter(CommandItem::isFilteredVisible)
      .filter(item -> !item.isDisabled())
      .toList();
  }

  /**
   * Returns the first enabled visible item.
   *
   * @return the first navigable item, or {@code null}
   */
  private CommandItem firstEnabledVisibleItem() {
    return registeredItems
      .stream()
      .filter(CommandItem::isFilteredVisible)
      .filter(item -> !item.isDisabled())
      .findFirst()
      .orElse(null);
  }

  /**
   * Updates the sole highlighted item and reveals it in registered lists.
   *
   * @param next the next highlighted item, or {@code null}
   */
  private void setHighlightedItem(CommandItem next) {
    CommandItem previous = highlightedItem.get();
    if (previous == next) {
      return;
    }
    if (previous != null) {
      previous.setHighlighted(false);
    }
    highlightedItem.set(next);
    if (next != null) {
      next.setHighlighted(true);
      registeredLists.forEach(list -> list.reveal(next));
    }
  }
}
