package com.acteque.terminal.ui.core.inputgroup;

import com.acteque.terminal.ui.core.Input;

/** A single-line control displayed inside an {@link InputGroup}. */
public final class InputGroupInput extends Input implements InputGroupControl {

  private static final String CONTROL_STYLE_CLASS = "input-group-control";
  private static final String INPUT_STYLE_CLASS = "input-group-input";

  public InputGroupInput() {
    this("");
  }

  public InputGroupInput(String text) {
    super(text);
    getStyleClass().addAll(CONTROL_STYLE_CLASS, INPUT_STYLE_CLASS);
  }
}
