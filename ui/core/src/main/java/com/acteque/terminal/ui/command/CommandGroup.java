package com.acteque.terminal.ui.command;

import java.util.Objects;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/** Labelled group of related command items. */
public final class CommandGroup extends VBox {

  private final Command command;
  private final Label heading = new Label();
  private final VBox entries = new VBox();

  /**
   * Creates a command group.
   *
   * @param command the owning command
   * @param headingText the group heading
   * @param children the grouped command entries
   */
  public CommandGroup(Command command, String headingText, Node... children) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    getStyleClass().add("core-command-group");
    setAccessibleRole(AccessibleRole.PARENT);
    heading.getStyleClass().add("command-group-heading");
    heading.setText(Objects.requireNonNull(headingText, "headingText cannot be null"));
    entries.getStyleClass().add("command-group-entries");
    entries.getChildren().addAll(children);
    getChildren().addAll(heading, entries);
    this.command.registerGroup(this);
    refreshVisibility();
  }

  /** Returns the group heading label. */
  public Label getHeading() {
    return heading;
  }

  /** Returns the mutable list of grouped entry nodes. */
  public javafx.collections.ObservableList<Node> getEntries() {
    return entries.getChildren();
  }

  /** Removes this group from derived visibility updates. */
  public void dispose() {
    command.unregisterGroup(this);
  }

  /** Shows the group only while it contains a filtered-visible item. */
  void refreshVisibility() {
    boolean hasVisibleItem = containsVisibleItem(entries);
    setVisible(hasVisibleItem);
    setManaged(hasVisibleItem);
  }

  /**
   * Searches a composed subtree for a visible command item.
   *
   * @param parent the subtree root
   * @return {@code true} when the subtree contains a matching item
   */
  private static boolean containsVisibleItem(Parent parent) {
    for (Node child : parent.getChildrenUnmodifiable()) {
      if (child instanceof CommandItem item && item.isFilteredVisible()) {
        return true;
      }
      if (child instanceof Parent nested && containsVisibleItem(nested)) {
        return true;
      }
    }
    return false;
  }
}
