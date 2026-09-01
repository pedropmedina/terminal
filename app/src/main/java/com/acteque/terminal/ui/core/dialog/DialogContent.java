package com.acteque.terminal.ui.core.dialog;

import com.acteque.terminal.ui.core.Button;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/** The centered dialog popup, including its optional top-right close button. */
public final class DialogContent extends VBox {

  private final Button closeButton = createCloseButton();
  private static final Insets FOOTER_MARGIN = new Insets(0.0, -16.0, -16.0, -16.0);
  private final BooleanProperty showCloseButton = new SimpleBooleanProperty(this, "showCloseButton", true);
  private Runnable closeAction = () -> {};

  public DialogContent(Node... children) {
    getStyleClass().add("core-dialog-content");
    setAccessibleRole(AccessibleRole.PARENT);
    setFocusTraversable(true);
    getChildren().addAll(children);
    getChildren().addListener(
      (javafx.collections.ListChangeListener<Node>) change -> {
        while (change.next()) {
          if (change.wasAdded()) {
            change.getAddedSubList().forEach(DialogContent::applyChildLayout);
          }
        }
      }
    );
    getChildren().forEach(DialogContent::applyChildLayout);

    closeButton.setManaged(false);
    closeButton.setOnAction(event -> closeAction.run());
    showCloseButton.addListener((ignored, wasVisible, isVisible) -> updateCloseButton(isVisible));
    updateCloseButton(true);
  }

  public final boolean isShowCloseButton() {
    return showCloseButton.get();
  }

  public final void setShowCloseButton(boolean value) {
    showCloseButton.set(value);
  }

  public final BooleanProperty showCloseButtonProperty() {
    return showCloseButton;
  }

  public final Button getCloseButton() {
    return closeButton;
  }

  void setCloseAction(Runnable action) {
    closeAction = Objects.requireNonNull(action, "action");
  }

  @Override
  protected void layoutChildren() {
    super.layoutChildren();
    if (!closeButton.isVisible()) {
      return;
    }
    double right = 8.0;
    double top = 8.0;
    double width = closeButton.prefWidth(-1.0);
    double height = closeButton.prefHeight(width);
    closeButton.resizeRelocate(getWidth() - right - width, top, width, height);
  }

  private void updateCloseButton(boolean visible) {
    closeButton.setVisible(visible);
    if (visible && !getChildren().contains(closeButton)) {
      getChildren().add(closeButton);
    } else if (!visible) {
      getChildren().remove(closeButton);
    }
  }

  private static Button createCloseButton() {
    Region firstLine = new Region();
    firstLine.getStyleClass().addAll("dialog-close-line", "dialog-close-line-first");
    Region secondLine = new Region();
    secondLine.getStyleClass().addAll("dialog-close-line", "dialog-close-line-second");
    StackPane icon = new StackPane(firstLine, secondLine);
    icon.getStyleClass().add("dialog-close-icon");
    icon.setMouseTransparent(true);
    icon.setAlignment(Pos.CENTER);

    Button button = new Button(null, icon, Button.Variant.GHOST, Button.Size.ICON_SM);
    button.getStyleClass().add("dialog-content-close");
    button.setAccessibleText("Close");
    return button;
  }

  private static void applyChildLayout(Node child) {
    if (child instanceof DialogFooter) {
      VBox.setMargin(child, FOOTER_MARGIN);
    }
  }
}
