package com.acteque.terminal.ui.drawer;

import com.acteque.terminal.ui.behavior.KineticScroll;
import java.util.IdentityHashMap;
import java.util.Map;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Drawer popup and its scrollable-content-ready body. */
public final class DrawerContent extends BorderPane {

  private final VBox body = new VBox();
  private final DrawerSwipeHandle swipeHandle = new DrawerSwipeHandle();
  private final Map<ScrollPane, KineticScroll> kineticScrolls = new IdentityHashMap<>();
  private Node autoGrowNode;
  private boolean kineticScrollingActive;

  public DrawerContent(Node... children) {
    getStyleClass().add("core-drawer-content");
    setAccessibleRole(AccessibleRole.PARENT);
    setFocusTraversable(true);
    body.getStyleClass().add("core-drawer-body");
    body.getChildren().addAll(children);
    body.getChildren().addListener(
      (javafx.collections.ListChangeListener<Node>) change -> {
        updateFooterGrowth();
        refreshKineticScrolling();
      }
    );
    updateFooterGrowth();
    setCenter(body);
  }

  /** The content column, whose children can be changed after construction. */
  public VBox getBody() {
    return body;
  }

  public DrawerSwipeHandle getSwipeHandle() {
    return swipeHandle;
  }

  void activateKineticScrolling() {
    kineticScrollingActive = true;
    refreshKineticScrolling();
  }

  void deactivateKineticScrolling() {
    kineticScrollingActive = false;
    kineticScrolls.values().forEach(KineticScroll::close);
    kineticScrolls.clear();
  }

  void configureHandle(DrawerDirection direction, boolean visible) {
    setTop(null);
    setBottom(null);
    setLeft(null);
    setRight(null);
    if (!visible) {
      return;
    }
    switch (direction) {
      case DOWN -> setTop(swipeHandle);
      case UP -> setBottom(swipeHandle);
      case LEFT -> setRight(swipeHandle);
      case RIGHT -> setLeft(swipeHandle);
    }
    swipeHandle.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("vertical"), direction.isVertical());
    swipeHandle.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("horizontal"), !direction.isVertical());
  }

  private void updateFooterGrowth() {
    if (autoGrowNode != null && VBox.getVgrow(autoGrowNode) == Priority.ALWAYS) {
      VBox.setVgrow(autoGrowNode, null);
    }
    autoGrowNode = null;
    for (int index = 1; index < body.getChildren().size(); index++) {
      if (body.getChildren().get(index) instanceof DrawerFooter) {
        Node previous = body.getChildren().get(index - 1);
        if (VBox.getVgrow(previous) == null) {
          VBox.setVgrow(previous, Priority.ALWAYS);
          autoGrowNode = previous;
        }
        break;
      }
    }
  }

  private void refreshKineticScrolling() {
    if (!kineticScrollingActive) {
      return;
    }
    Map<ScrollPane, Boolean> current = new IdentityHashMap<>();
    collectScrollPanes(body, current);
    kineticScrolls.entrySet().removeIf(entry -> {
      if (current.containsKey(entry.getKey())) {
        return false;
      }
      entry.getValue().close();
      return true;
    });
    current
      .keySet()
      .stream()
      .filter(scrollPane -> !kineticScrolls.containsKey(scrollPane))
      .forEach(scrollPane ->
        kineticScrolls.put(
          scrollPane,
          new KineticScroll(
            scrollPane,
            requestedPixels -> scrollByPixels(scrollPane, requestedPixels),
            new ReadOnlyBooleanWrapper(scrollPane, "gliding")
          )
        )
      );
  }

  private static void collectScrollPanes(Node node, Map<ScrollPane, Boolean> target) {
    if (node instanceof ScrollPane scrollPane) {
      target.put(scrollPane, Boolean.TRUE);
    }
    if (node instanceof Parent parent) {
      parent.getChildrenUnmodifiable().forEach(child -> collectScrollPanes(child, target));
    }
  }

  private static double scrollByPixels(ScrollPane scrollPane, double requestedPixels) {
    Node content = scrollPane.getContent();
    if (content == null || requestedPixels == 0.0) {
      return 0.0;
    }

    double scrollablePixels = Math.max(
      0.0,
      content.getBoundsInLocal().getHeight() - scrollPane.getViewportBounds().getHeight()
    );
    double valueRange = scrollPane.getVmax() - scrollPane.getVmin();
    if (scrollablePixels == 0.0 || valueRange == 0.0) {
      return 0.0;
    }

    double currentPixels = ((scrollPane.getVvalue() - scrollPane.getVmin()) / valueRange) * scrollablePixels;
    double nextPixels = Math.max(0.0, Math.min(scrollablePixels, currentPixels + requestedPixels));
    scrollPane.setVvalue(scrollPane.getVmin() + (nextPixels / scrollablePixels) * valueRange);
    return nextPixels - currentPixels;
  }
}
