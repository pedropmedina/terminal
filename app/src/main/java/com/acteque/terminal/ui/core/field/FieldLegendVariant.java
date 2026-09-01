package com.acteque.terminal.ui.core.field;

/** Visual variants for a field legend. */
public enum FieldLegendVariant {
  LEGEND("field-legend-variant-legend"),
  LABEL("field-legend-variant-label");

  private final String styleClass;

  FieldLegendVariant(String styleClass) {
    this.styleClass = styleClass;
  }

  String styleClass() {
    return styleClass;
  }
}
