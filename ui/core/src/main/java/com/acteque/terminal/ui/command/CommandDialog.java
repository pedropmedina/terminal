package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.dialog.DialogContent;
import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

/** Modal command-palette wrapper built from the shared dialog primitive. */
public final class CommandDialog extends Dialog {

  private static final String DEFAULT_TITLE = "Command Palette";
  private static final String DEFAULT_DESCRIPTION = "Search for a command to run...";
  private static final double EDGE_MARGIN = 16.0;
  private static final double TOP_OFFSET_RATIO = 1.0 / 3.0;

  private final Command command;
  private final DialogContent dialogContent;
  private final StringProperty title = new SimpleStringProperty(this, "title", DEFAULT_TITLE);
  private final StringProperty description = new SimpleStringProperty(this, "description", DEFAULT_DESCRIPTION);

  /**
   * Creates a command dialog with the default accessible title and description.
   *
   * @param command the command presented by the dialog
   */
  public CommandDialog(Command command) {
    this(command, DEFAULT_TITLE, DEFAULT_DESCRIPTION);
  }

  /**
   * Creates a command dialog with explicit accessible copy.
   *
   * @param command the command presented by the dialog
   * @param title the accessible dialog title
   * @param description the accessible dialog description
   */
  public CommandDialog(Command command, String title, String description) {
    super(new DialogContent(Objects.requireNonNull(command, "command cannot be null")));
    this.command = command;
    dialogContent = getContent();

    getStyleClass().add("core-command-dialog");
    setAccessibleRole(AccessibleRole.DIALOG);
    dialogContent.getStyleClass().add("core-command-dialog-content");
    dialogContent.setShowCloseButton(false);
    dialogContent.setMaxHeight(Region.USE_PREF_SIZE);
    StackPane.setAlignment(dialogContent, Pos.TOP_CENTER);
    StackPane.setMargin(dialogContent, new Insets(0.0, EDGE_MARGIN, 0.0, EDGE_MARGIN));
    dialogContent.translateYProperty().bind(getPortal().heightProperty().multiply(TOP_OFFSET_RATIO));

    this.title.addListener(ignored -> refreshAccessibility());
    this.description.addListener(ignored -> refreshAccessibility());
    setTitle(title);
    setDescription(description);
    refreshAccessibility();
  }

  /** Returns the command presented by this dialog. */
  public Command getCommand() {
    return command;
  }

  /** Returns the shared dialog-content container. */
  public DialogContent getDialogContent() {
    return dialogContent;
  }

  /** Returns the accessible dialog title. */
  public String getTitle() {
    return title.get();
  }

  /**
   * Sets the accessible dialog title.
   *
   * @param value the non-null title
   */
  public void setTitle(String value) {
    title.set(Objects.requireNonNull(value, "title cannot be null"));
  }

  /** Returns the accessible-title property. */
  public StringProperty titleProperty() {
    return title;
  }

  /** Returns the accessible dialog description. */
  public String getDescription() {
    return description.get();
  }

  /**
   * Sets the accessible dialog description.
   *
   * @param value the non-null description
   */
  public void setDescription(String value) {
    description.set(Objects.requireNonNull(value, "description cannot be null"));
  }

  /** Returns the accessible-description property. */
  public StringProperty descriptionProperty() {
    return description;
  }

  /** Returns whether the dialog content displays its close button. */
  public boolean isShowCloseButton() {
    return dialogContent.isShowCloseButton();
  }

  /**
   * Sets whether the dialog content displays its close button.
   *
   * @param value {@code true} to display the close affordance
   */
  public void setShowCloseButton(boolean value) {
    dialogContent.setShowCloseButton(value);
  }

  /** Returns the delegated close-button property. */
  public BooleanProperty showCloseButtonProperty() {
    return dialogContent.showCloseButtonProperty();
  }

  /** Copies title and description properties to JavaFX accessibility metadata. */
  private void refreshAccessibility() {
    setAccessibleText(getTitle());
    setAccessibleHelp(getDescription());
    dialogContent.setAccessibleText(getTitle());
    dialogContent.setAccessibleHelp(getDescription());
  }
}
