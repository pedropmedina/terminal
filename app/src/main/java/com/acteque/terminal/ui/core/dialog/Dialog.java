package com.acteque.terminal.ui.core.dialog;

import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;

/** A modal dialog root with backdrop dismissal, focus containment, and focus restoration. */
public class Dialog extends StackPane {

  private static final PseudoClass OPEN_PSEUDO_CLASS = PseudoClass.getPseudoClass("open");
  private static final PseudoClass CLOSED_PSEUDO_CLASS = PseudoClass.getPseudoClass("closed");

  private final DialogPortal portal = new DialogPortal();
  private final DialogOverlay overlay = new DialogOverlay();
  private final BooleanProperty open = new SimpleBooleanProperty(this, "open", false);
  private final BooleanProperty dismissible = new SimpleBooleanProperty(this, "dismissible", true);
  private final ObjectProperty<DialogContent> content = new SimpleObjectProperty<>(this, "content") {
    @Override
    protected void invalidated() {
      replaceContent(get());
    }
  };
  private Node focusOwnerBeforeOpen;

  public Dialog() {
    this(null);
  }

  public Dialog(DialogContent content) {
    getStyleClass().add("core-dialog");
    setFocusTraversable(true);

    overlay.setOnMouseClicked(event -> {
      if (isDismissible()) {
        close();
      }
      event.consume();
    });
    portal.getChildren().add(overlay);
    getChildren().add(portal);

    addEventFilter(KeyEvent.KEY_PRESSED, this::handleKeyPressed);
    open.addListener((ignored, wasOpen, isOpen) -> applyOpenState(isOpen));
    applyOpenState(false);
    setContent(content);
  }

  public final boolean isOpen() {
    return open.get();
  }

  public final void setOpen(boolean value) {
    open.set(value);
  }

  public final BooleanProperty openProperty() {
    return open;
  }

  public final boolean isDismissible() {
    return dismissible.get();
  }

  public final void setDismissible(boolean value) {
    dismissible.set(value);
  }

  public final BooleanProperty dismissibleProperty() {
    return dismissible;
  }

  public final DialogContent getContent() {
    return content.get();
  }

  public final void setContent(DialogContent value) {
    content.set(value);
  }

  public final ObjectProperty<DialogContent> contentProperty() {
    return content;
  }

  public final DialogPortal getPortal() {
    return portal;
  }

  public final DialogOverlay getOverlay() {
    return overlay;
  }

  public final void show() {
    setOpen(true);
  }

  public final void close() {
    setOpen(false);
  }

  private void replaceContent(DialogContent nextContent) {
    portal.getChildren().removeIf(DialogContent.class::isInstance);
    if (nextContent != null) {
      nextContent.setCloseAction(this::close);
      nextContent.setOnMouseClicked(event -> event.consume());
      nextContent.pseudoClassStateChanged(OPEN_PSEUDO_CLASS, isOpen());
      nextContent.pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, !isOpen());
      StackPane.setMargin(nextContent, new Insets(16.0));
      portal.getChildren().add(nextContent);
    }
  }

  private void applyOpenState(boolean isOpen) {
    pseudoClassStateChanged(OPEN_PSEUDO_CLASS, isOpen);
    pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, !isOpen);
    portal.pseudoClassStateChanged(OPEN_PSEUDO_CLASS, isOpen);
    portal.pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, !isOpen);
    overlay.pseudoClassStateChanged(OPEN_PSEUDO_CLASS, isOpen);
    overlay.pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, !isOpen);
    if (getContent() != null) {
      getContent().pseudoClassStateChanged(OPEN_PSEUDO_CLASS, isOpen);
      getContent().pseudoClassStateChanged(CLOSED_PSEUDO_CLASS, !isOpen);
    }

    setVisible(isOpen);
    setManaged(isOpen);
    if (isOpen) {
      focusOwnerBeforeOpen = getScene() == null ? null : getScene().getFocusOwner();
      Platform.runLater(this::focusFirstControl);
    } else if (focusOwnerBeforeOpen != null) {
      Node previousFocusOwner = focusOwnerBeforeOpen;
      focusOwnerBeforeOpen = null;
      Platform.runLater(previousFocusOwner::requestFocus);
    }
  }

  private void handleKeyPressed(KeyEvent event) {
    if (event.getCode() == KeyCode.ESCAPE && isDismissible()) {
      close();
      event.consume();
      return;
    }
    if (event.getCode() != KeyCode.TAB || getContent() == null) {
      return;
    }

    List<Node> focusable = new ArrayList<>();
    collectFocusableChildren(getContent(), focusable);
    if (focusable.isEmpty()) {
      requestFocus();
      event.consume();
      return;
    }

    Node focusOwner = getScene() == null ? null : getScene().getFocusOwner();
    int current = focusable.indexOf(focusOwner);
    int next = event.isShiftDown()
      ? current <= 0
        ? focusable.size() - 1
        : current - 1
      : current < 0 || current == focusable.size() - 1
        ? 0
        : current + 1;
    focusable.get(next).requestFocus();
    event.consume();
  }

  private void focusFirstControl() {
    if (!isOpen() || getContent() == null) {
      return;
    }
    List<Node> focusable = new ArrayList<>();
    collectFocusableChildren(getContent(), focusable);
    if (focusable.isEmpty()) {
      getContent().requestFocus();
    } else {
      focusable.getFirst().requestFocus();
    }
  }

  private static void collectFocusable(Node node, List<Node> focusable) {
    if (node.isFocusTraversable() && node.isVisible() && !node.isDisabled()) {
      focusable.add(node);
    }
    if (node instanceof Parent parent) {
      parent.getChildrenUnmodifiable().forEach(child -> collectFocusable(child, focusable));
    }
  }

  private static void collectFocusableChildren(Parent parent, List<Node> focusable) {
    parent.getChildrenUnmodifiable().forEach(child -> collectFocusable(child, focusable));
  }
}
