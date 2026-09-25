package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Arrays;
import java.util.Objects;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Invokable command option with highlighted, disabled, and checked presentation states. */
public final class CommandItem extends Button {

  private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");
  private static final PseudoClass CHECKED = PseudoClass.getPseudoClass("checked");

  private final Command command;
  private final StringProperty value = new SimpleStringProperty(this, "value", "");
  private final ObservableList<String> keywords = FXCollections.observableArrayList();
  private final BooleanProperty checked = new SimpleBooleanProperty(this, "checked", false);
  private final HBox content = new HBox();
  private final Region spacer = new Region();
  private final StackPane indicator = new StackPane(new LucideIcon(LucideIcons.CHECK));
  private boolean filteredVisible = true;
  private boolean shortcutPresent;

  /**
   * Creates a text command item whose value and label are identical.
   *
   * @param command the owning command
   * @param value the stable searchable value and visible label
   */
  public CommandItem(Command command, String value) {
    this(command, value, new Label(Objects.requireNonNull(value, "value cannot be null")));
  }

  /**
   * Creates a command item with custom content.
   *
   * @param command the owning command
   * @param value the stable searchable value
   * @param children the visible item content, optionally including a {@link CommandShortcut}
   */
  public CommandItem(Command command, String value, Node... children) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    Objects.requireNonNull(children, "children cannot be null");
    Arrays.stream(children).forEach(child -> Objects.requireNonNull(child, "item child cannot be null"));

    getStyleClass().add("core-command-item");
    setAccessibleRole(AccessibleRole.MENU_ITEM);
    setFocusTraversable(false);
    setMaxWidth(Double.MAX_VALUE);
    setAlignment(Pos.CENTER_LEFT);
    setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

    content.getStyleClass().add("command-item-content");
    content.setAlignment(Pos.CENTER_LEFT);
    content.setMaxWidth(Double.MAX_VALUE);
    content
      .prefWidthProperty()
      .bind(
        Bindings.createDoubleBinding(
          () -> Math.max(0.0, getWidth() - snappedLeftInset() - snappedRightInset()),
          widthProperty(),
          insetsProperty()
        )
      );
    indicator.getStyleClass().add("command-item-indicator");
    composeContent(children);
    setGraphic(content);

    this.value.addListener(ignored -> this.command.refreshItems());
    keywords.addListener((InvalidationListener) ignored -> this.command.refreshItems());
    checked.addListener(ignored -> refreshCheckedState());
    disabledProperty().addListener(ignored -> this.command.refreshItems());
    setOnMouseEntered(ignored -> this.command.highlight(this));
    setValue(value);
    refreshCheckedState();
    this.command.registerItem(this);
  }

  /** Returns the stable searchable value. */
  public String getValue() {
    return value.get();
  }

  /**
   * Replaces the stable searchable value.
   *
   * @param next the non-null searchable value
   */
  public void setValue(String next) {
    value.set(Objects.requireNonNull(next, "value cannot be null"));
    setAccessibleText(next);
  }

  /** Returns the searchable-value property. */
  public StringProperty valueProperty() {
    return value;
  }

  /** Returns the mutable list of additional searchable keywords. */
  public ObservableList<String> getKeywords() {
    return keywords;
  }

  /** Returns whether the item displays its checked indicator. */
  public boolean isChecked() {
    return checked.get();
  }

  /**
   * Sets whether the item displays its checked indicator.
   *
   * @param next {@code true} to reveal the check icon
   */
  public void setChecked(boolean next) {
    checked.set(next);
  }

  /** Returns the checked-state property. */
  public BooleanProperty checkedProperty() {
    return checked;
  }

  /** Returns the item content container. */
  public HBox getItemContent() {
    return content;
  }

  /** Removes this item from its command's interaction registry. */
  public void dispose() {
    command.unregisterItem(this);
  }

  /** Returns accessible selected and text state. */
  @Override
  public Object queryAccessibleAttribute(AccessibleAttribute attribute, Object... parameters) {
    if (attribute == AccessibleAttribute.SELECTED) {
      return command.getHighlightedItem() == this;
    }
    if (attribute == AccessibleAttribute.TEXT) {
      return getValue();
    }
    return super.queryAccessibleAttribute(attribute, parameters);
  }

  /**
   * Applies filtering without removing the item from its command registry.
   *
   * @param value {@code true} when the item matches the current query
   */
  void setFilteredVisible(boolean value) {
    filteredVisible = value;
    setVisible(value);
    setManaged(value);
  }

  /** Returns whether the item currently matches filtering. */
  boolean isFilteredVisible() {
    return filteredVisible;
  }

  /**
   * Applies the active keyboard or pointer highlight.
   *
   * @param value {@code true} when this is the active item
   */
  void setHighlighted(boolean value) {
    pseudoClassStateChanged(SELECTED, value);
    notifyAccessibleAttributeChanged(AccessibleAttribute.SELECTED);
  }

  /**
   * Builds the item row and gives shortcuts the trailing position.
   *
   * @param children the caller-provided content nodes
   */
  private void composeContent(Node[] children) {
    int shortcutIndex = -1;
    for (int index = 0; index < children.length; index++) {
      if (children[index] instanceof CommandShortcut) {
        shortcutIndex = index;
        break;
      }
    }

    HBox.setHgrow(spacer, Priority.ALWAYS);
    if (shortcutIndex < 0) {
      content.getChildren().addAll(children);
      content.getChildren().addAll(spacer, indicator);
      return;
    }

    content.getChildren().addAll(Arrays.copyOfRange(children, 0, shortcutIndex));
    content.getChildren().add(spacer);
    content.getChildren().addAll(Arrays.copyOfRange(children, shortcutIndex, children.length));
    shortcutPresent = true;
    content.getChildren().add(indicator);
  }

  /** Applies checked styling and removes unused trailing layout space. */
  private void refreshCheckedState() {
    pseudoClassStateChanged(CHECKED, isChecked());
    boolean showIndicator = isChecked() && !shortcutPresent;
    indicator.setManaged(showIndicator);
    indicator.setVisible(showIndicator);
    indicator.setOpacity(showIndicator ? 1.0 : 0.0);

    boolean showSpacer = showIndicator || shortcutPresent;
    spacer.setManaged(showSpacer);
    spacer.setVisible(showSpacer);
  }
}
