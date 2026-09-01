package com.acteque.terminal.ui.core.field;

/** Layout variants for a field. */
public enum FieldOrientation {
  VERTICAL("field-orientation-vertical"),
  HORIZONTAL("field-orientation-horizontal"),
  RESPONSIVE("field-orientation-responsive");

  private final String styleClass;

  FieldOrientation(String styleClass) {
    this.styleClass = styleClass;
  }

  String styleClass() {
    return styleClass;
  }
}
