package com.acteque.terminal.ui.core.inputgroup;

/** Positions supported by an input-group addon. */
public enum InputGroupAlignment {
  INLINE_START("input-group-addon-inline-start"),
  INLINE_END("input-group-addon-inline-end"),
  BLOCK_START("input-group-addon-block-start"),
  BLOCK_END("input-group-addon-block-end");

  final String styleClass;

  InputGroupAlignment(String styleClass) {
    this.styleClass = styleClass;
  }
}
