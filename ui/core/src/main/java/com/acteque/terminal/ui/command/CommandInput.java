package com.acteque.terminal.ui.command;

import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.inputgroup.InputGroup;
import com.acteque.terminal.ui.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.inputgroup.InputGroupInput;
import java.util.Objects;
import javafx.beans.value.ChangeListener;
import javafx.scene.AccessibleRole;
import javafx.scene.layout.VBox;

/** Search input wrapper for a {@link Command}. */
public final class CommandInput extends VBox {

  private final Command command;
  private final InputGroupInput editor = new InputGroupInput();
  private final LucideIcon searchIcon = new LucideIcon(LucideIcons.SEARCH);
  private final InputGroup inputGroup;
  private final ChangeListener<String> editorListener = (ignored, previous, next) -> updateCommand(next);
  private final ChangeListener<String> commandListener = (ignored, previous, next) -> updateEditor(next);
  private boolean synchronizing;

  /**
   * Creates an input connected to the supplied command root.
   *
   * @param command the owning command
   */
  public CommandInput(Command command) {
    this.command = Objects.requireNonNull(command, "command cannot be null");
    InputGroupAddon addon = new InputGroupAddon(InputGroupAlignment.INLINE_START, searchIcon);
    inputGroup = new InputGroup(addon, editor);

    getStyleClass().add("core-command-input-wrapper");
    setAccessibleRole(AccessibleRole.PARENT);
    inputGroup.getStyleClass().add("core-command-input-group");
    editor.getStyleClass().add("core-command-input");
    searchIcon.getStyleClass().add("command-input-search-icon");
    searchIcon.setMouseTransparent(true);
    inputGroup.disableProperty().bind(disabledProperty());
    getChildren().add(inputGroup);

    editor.textProperty().addListener(editorListener);
    command.searchTextProperty().addListener(commandListener);
    editor.setText(command.getSearchText());
  }

  /** Returns the editable text field. */
  public InputGroupInput getEditor() {
    return editor;
  }

  /** Returns the composed input group. */
  public InputGroup getInputGroup() {
    return inputGroup;
  }

  /** Returns the leading search icon. */
  public LucideIcon getSearchIcon() {
    return searchIcon;
  }

  /** Stops synchronizing the editor with the command root. */
  public void dispose() {
    editor.textProperty().removeListener(editorListener);
    command.searchTextProperty().removeListener(commandListener);
    inputGroup.disableProperty().unbind();
  }

  /**
   * Copies editor changes into command search state without recursion.
   *
   * @param value the editor text
   */
  private void updateCommand(String value) {
    if (synchronizing) {
      return;
    }
    synchronizing = true;
    try {
      command.setSearchText(value);
    } finally {
      synchronizing = false;
    }
  }

  /**
   * Copies programmatic command search changes into the editor without recursion.
   *
   * @param value the command search text
   */
  private void updateEditor(String value) {
    if (synchronizing || Objects.equals(editor.getText(), value)) {
      return;
    }
    synchronizing = true;
    try {
      editor.setText(value);
    } finally {
      synchronizing = false;
    }
  }
}
