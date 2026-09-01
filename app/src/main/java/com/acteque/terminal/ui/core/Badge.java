package com.acteque.terminal.ui.core;

import java.util.Objects;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.css.PseudoClass;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.layout.Region;

/** A compact status label styled to match the shadcn/ui Badge primitive. */
public class Badge extends javafx.scene.control.Label {

  private static final String ROOT_STYLE_CLASS = "core-badge";
  private static final PseudoClass INTERACTIVE_PSEUDO_CLASS = PseudoClass.getPseudoClass("interactive");
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final PseudoClass ICON_INLINE_START_PSEUDO_CLASS = PseudoClass.getPseudoClass("icon-inline-start");
  private static final PseudoClass ICON_INLINE_END_PSEUDO_CLASS = PseudoClass.getPseudoClass("icon-inline-end");

  /** Semantic visual treatments supported by the badge. */
  public enum Variant {
    DEFAULT("badge-variant-default"),
    SECONDARY("badge-variant-secondary"),
    DESTRUCTIVE("badge-variant-destructive"),
    OUTLINE("badge-variant-outline"),
    GHOST("badge-variant-ghost"),
    LINK("badge-variant-link");

    private final String styleClass;

    Variant(String styleClass) {
      this.styleClass = styleClass;
    }
  }

  private String appliedVariantStyleClass;

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
      return Badge.this;
    }

    @Override
    public String getName() {
      return "variant";
    }
  };

  private final BooleanProperty interactive = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INTERACTIVE_PSEUDO_CLASS, get());
      setFocusTraversable(get());
    }

    @Override
    public Object getBean() {
      return Badge.this;
    }

    @Override
    public String getName() {
      return "interactive";
    }
  };

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Badge.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  public Badge() {
    this(null, null, Variant.DEFAULT);
  }

  public Badge(String text) {
    this(text, null, Variant.DEFAULT);
  }

  public Badge(String text, Variant variant) {
    this(text, null, variant);
  }

  public Badge(String text, Node graphic) {
    this(text, graphic, Variant.DEFAULT);
  }

  public Badge(String text, Node graphic, Variant variant) {
    super(text, graphic);
    getStyleClass().add(ROOT_STYLE_CLASS);
    setMinWidth(Region.USE_PREF_SIZE);
    applyVariant(Variant.DEFAULT);
    setVariant(variant);
    graphicProperty().addListener(ignored -> refreshIconPosition());
    contentDisplayProperty().addListener(ignored -> refreshIconPosition());
    refreshIconPosition();
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

  /** Whether this badge represents an interactive target, analogous to rendering the React badge as a link. */
  public final boolean isInteractive() {
    return interactive.get();
  }

  public final void setInteractive(boolean value) {
    interactive.set(value);
  }

  public final BooleanProperty interactiveProperty() {
    return interactive;
  }

  /** Whether this badge currently fails validation. */
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
    if (appliedVariantStyleClass != null) {
      getStyleClass().remove(appliedVariantStyleClass);
    }
    appliedVariantStyleClass = selectedVariant.styleClass;
    if (!getStyleClass().contains(appliedVariantStyleClass)) {
      getStyleClass().add(appliedVariantStyleClass);
    }
  }

  private void refreshIconPosition() {
    ContentDisplay display = getContentDisplay();
    boolean hasGraphic = getGraphic() != null;
    pseudoClassStateChanged(ICON_INLINE_START_PSEUDO_CLASS, hasGraphic && display == ContentDisplay.LEFT);
    pseudoClassStateChanged(ICON_INLINE_END_PSEUDO_CLASS, hasGraphic && display == ContentDisplay.RIGHT);
  }
}
