package com.acteque.terminal.ui.core;

import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.scene.Node;

/** A theme-aware button with design-system variants and sizes. */
public class Button extends javafx.scene.control.Button {

  private static final String ROOT_STYLE_CLASS = "core-button";

  /** Semantic visual treatments supported by the core button. */
  public enum Variant {
    DEFAULT("button-variant-default"),
    OUTLINE("button-variant-outline"),
    SECONDARY("button-variant-secondary"),
    GHOST("button-variant-ghost"),
    DESTRUCTIVE("button-variant-destructive"),
    LINK("button-variant-link");

    private final String styleClass;

    Variant(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  /** Standard text and icon-only dimensions supported by the core button. */
  public enum Size {
    DEFAULT("button-size-default"),
    XS("button-size-xs"),
    SM("button-size-sm"),
    LG("button-size-lg"),
    ICON("button-size-icon"),
    ICON_XS("button-size-icon-xs"),
    ICON_SM("button-size-icon-sm"),
    ICON_LG("button-size-icon-lg");

    private final String styleClass;

    Size(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  private String appliedVariantStyleClass;
  private String appliedSizeStyleClass;

  private final ObjectProperty<Variant> variant = new ObjectPropertyBase<>(Variant.DEFAULT) {
    @Override
    public void set(Variant value) {
      super.set(Objects.requireNonNull(value, "variant"));
    }

    @Override
    protected void invalidated() {
      applyVariant(Objects.requireNonNull(get(), "variant"));
    }

    @Override
    public Object getBean() {
      return Button.this;
    }

    @Override
    public String getName() {
      return "variant";
    }
  };

  private final ObjectProperty<Size> size = new ObjectPropertyBase<>(Size.DEFAULT) {
    @Override
    public void set(Size value) {
      super.set(Objects.requireNonNull(value, "size"));
    }

    @Override
    protected void invalidated() {
      applySize(Objects.requireNonNull(get(), "size"));
    }

    @Override
    public Object getBean() {
      return Button.this;
    }

    @Override
    public String getName() {
      return "size";
    }
  };

  public Button() {
    this(null, null, Variant.DEFAULT, Size.DEFAULT);
  }

  public Button(String text) {
    this(text, null, Variant.DEFAULT, Size.DEFAULT);
  }

  public Button(String text, Node graphic) {
    this(text, graphic, Variant.DEFAULT, Size.DEFAULT);
  }

  public Button(String text, Variant variant, Size size) {
    this(text, null, variant, size);
  }

  public Button(String text, Node graphic, Variant variant, Size size) {
    super(text, graphic);
    getStyleClass().add(ROOT_STYLE_CLASS);
    applyVariant(Variant.DEFAULT);
    applySize(Size.DEFAULT);
    setVariant(variant);
    setSize(size);
  }

  public final ObjectProperty<Variant> variantProperty() {
    return variant;
  }

  public final Variant getVariant() {
    return variant.get();
  }

  public final void setVariant(Variant value) {
    variant.set(Objects.requireNonNull(value, "variant"));
  }

  public final ObjectProperty<Size> sizeProperty() {
    return size;
  }

  public final Size getSize() {
    return size.get();
  }

  public final void setSize(Size value) {
    size.set(Objects.requireNonNull(value, "size"));
  }

  private void applyVariant(Variant selectedVariant) {
    appliedVariantStyleClass = replaceStyleClass(appliedVariantStyleClass, selectedVariant.styleClass);
  }

  private void applySize(Size selectedSize) {
    appliedSizeStyleClass = replaceStyleClass(appliedSizeStyleClass, selectedSize.styleClass);
  }

  private String replaceStyleClass(String previousStyleClass, String nextStyleClass) {
    if (previousStyleClass != null) {
      getStyleClass().remove(previousStyleClass);
    }
    if (!getStyleClass().contains(nextStyleClass)) {
      getStyleClass().add(nextStyleClass);
    }
    return nextStyleClass;
  }
}
