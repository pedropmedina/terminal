package com.acteque.terminal.ui.core.field;

import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;

/** Heading for a {@link FieldSet}. */
public final class FieldLegend extends com.acteque.terminal.ui.core.Label {

  private final ObjectProperty<FieldLegendVariant> variant = new ObjectPropertyBase<>(FieldLegendVariant.LEGEND) {
    @Override
    public void set(FieldLegendVariant value) {
      super.set(Objects.requireNonNull(value, "variant"));
    }

    @Override
    protected void invalidated() {
      applyVariant(Objects.requireNonNull(get(), "variant"));
    }

    @Override
    public Object getBean() {
      return FieldLegend.this;
    }

    @Override
    public String getName() {
      return "variant";
    }
  };

  public FieldLegend() {
    this(null, FieldLegendVariant.LEGEND);
  }

  public FieldLegend(String text) {
    this(text, FieldLegendVariant.LEGEND);
  }

  public FieldLegend(String text, FieldLegendVariant variant) {
    super(text);
    getStyleClass().add("core-field-legend");
    applyVariant(FieldLegendVariant.LEGEND);
    setVariant(variant);
  }

  public final ObjectProperty<FieldLegendVariant> variantProperty() {
    return variant;
  }

  public final FieldLegendVariant getVariant() {
    return variant.get();
  }

  public final void setVariant(FieldLegendVariant value) {
    variant.set(Objects.requireNonNull(value, "variant"));
  }

  private void applyVariant(FieldLegendVariant selectedVariant) {
    getStyleClass().removeAll(FieldLegendVariant.LEGEND.styleClass(), FieldLegendVariant.LABEL.styleClass());
    getStyleClass().add(selectedVariant.styleClass());
  }
}
