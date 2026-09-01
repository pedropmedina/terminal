package com.acteque.terminal.ui.core;

import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.Node;

/** A two-state button styled to match the shadcn/ui Toggle primitive. */
public class Toggle extends javafx.scene.control.ToggleButton {

  private static final String ROOT_STYLE_CLASS = "core-toggle";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

  /** Semantic visual treatments supported by the toggle. */
  public enum Variant {
    DEFAULT("toggle-variant-default"),
    OUTLINE("toggle-variant-outline");

    private final String styleClass;

    Variant(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  /** Standard dimensions supported by the toggle. */
  public enum Size {
    DEFAULT("toggle-size-default"),
    SM("toggle-size-sm"),
    LG("toggle-size-lg");

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
      return Toggle.this;
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
      return Toggle.this;
    }

    @Override
    public String getName() {
      return "size";
    }
  };

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Toggle.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  public Toggle() {
    this(null, null, Variant.DEFAULT, Size.DEFAULT);
  }

  public Toggle(String text) {
    this(text, null, Variant.DEFAULT, Size.DEFAULT);
  }

  public Toggle(String text, Node graphic) {
    this(text, graphic, Variant.DEFAULT, Size.DEFAULT);
  }

  public Toggle(String text, Variant variant, Size size) {
    this(text, null, variant, size);
  }

  public Toggle(String text, Node graphic, Variant variant, Size size) {
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

  /** Whether this toggle currently fails validation. */
  public final boolean isInvalid() {
    return invalid.get();
  }

  public final void setInvalid(boolean value) {
    invalid.set(value);
  }

  public final BooleanProperty invalidProperty() {
    return invalid;
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
