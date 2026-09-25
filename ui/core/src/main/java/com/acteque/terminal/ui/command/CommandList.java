package com.acteque.terminal.ui.command;

import java.util.Objects;
import javafx.geometry.Bounds;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

/** Scrollable list container for command groups, items, separators, and empty state. */
public final class CommandList extends ScrollPane {

  private static final double SCROLL_PADDING = 4.0;

  private final Command command;
  private final VBox entries = new VBox();

  /**
   * Creates a command list containing the supplied entries.
   *
   * @param command the owning command
   * @param children the groups, items, separators, or empty-state nodes
   */
  public CommandList(Command command, Node... children) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    getStyleClass().add("core-command-list");
    entries.getStyleClass().add("command-list-entries");
    entries.getChildren().addAll(children);
    setContent(entries);
    setFitToWidth(true);
    setHbarPolicy(ScrollBarPolicy.NEVER);
    setVbarPolicy(ScrollBarPolicy.NEVER);
    setAccessibleRole(AccessibleRole.LIST_VIEW);
    setFocusTraversable(false);
    this.command.registerList(this);
  }

  /** Returns the mutable list of rendered entry nodes. */
  public javafx.collections.ObservableList<Node> getEntries() {
    return entries.getChildren();
  }

  /** Stops participating in highlighted-item reveal behavior. */
  public void dispose() {
    command.unregisterList(this);
  }

  /**
   * Scrolls just enough to keep the highlighted item within the padded viewport.
   *
   * @param item the highlighted item
   */
  void reveal(CommandItem item) {
    if (item.getScene() == null || entries.getScene() == null) {
      return;
    }
    Bounds sceneBounds = item.localToScene(item.getBoundsInLocal());
    Bounds itemBounds = entries.sceneToLocal(sceneBounds);
    double contentHeight = entries.getBoundsInLocal().getHeight();
    double viewportHeight = getViewportBounds().getHeight();
    double scrollRange = contentHeight - viewportHeight;
    if (scrollRange <= 0.0) {
      return;
    }

    double viewportTop = getVvalue() * scrollRange;
    double targetTop = viewportTop;
    if (itemBounds.getMinY() < viewportTop + SCROLL_PADDING) {
      targetTop = itemBounds.getMinY() - SCROLL_PADDING;
    } else if (itemBounds.getMaxY() > viewportTop + viewportHeight - SCROLL_PADDING) {
      targetTop = itemBounds.getMaxY() - viewportHeight + SCROLL_PADDING;
    }
    setVvalue(clamp(targetTop / scrollRange));
  }

  /**
   * Constrains a normalized scroll value to the supported range.
   *
   * @param value the candidate normalized value
   * @return a value between zero and one
   */
  private static double clamp(double value) {
    return Math.max(0.0, Math.min(1.0, value));
  }
}
