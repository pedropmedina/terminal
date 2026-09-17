package com.acteque.terminal.ui.inputgroup;

import com.acteque.terminal.ui.Textarea;

/** A multiline control displayed inside an {@link InputGroup}. */
public final class InputGroupTextarea extends Textarea implements InputGroupControl {

  private static final String CONTROL_STYLE_CLASS = "input-group-control";
  private static final String TEXTAREA_STYLE_CLASS = "input-group-textarea";

  public InputGroupTextarea() {
    this("");
  }

  public InputGroupTextarea(String text) {
    super(text);
    getStyleClass().addAll(CONTROL_STYLE_CLASS, TEXTAREA_STYLE_CLASS);
  }
}
