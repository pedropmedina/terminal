package com.acteque.terminal.ui.core;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;

/** A single-line text input styled to match the shadcn/ui Input primitive. */
public final class Input extends javafx.scene.control.TextField {

  private static final String ROOT_STYLE_CLASS = "core-input";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Input.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  public Input() {
    this("");
  }

  public Input(String text) {
    super(text);
    getStyleClass().add(ROOT_STYLE_CLASS);
  }

  /** Whether this input currently fails validation. */
  public final boolean isInvalid() {
    return invalid.get();
  }

  public final void setInvalid(boolean value) {
    invalid.set(value);
  }

  public final BooleanProperty invalidProperty() {
    return invalid;
  }
}
