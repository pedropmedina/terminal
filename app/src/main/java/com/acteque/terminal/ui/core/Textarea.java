package com.acteque.terminal.ui.core;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.css.PseudoClass;

/** A multiline text input styled to match the shadcn/ui Textarea primitive. */
public class Textarea extends javafx.scene.control.TextArea {

  private static final String ROOT_STYLE_CLASS = "core-textarea";
  private static final PseudoClass INVALID_PSEUDO_CLASS = PseudoClass.getPseudoClass("invalid");
  private static final int DEFAULT_PREFERRED_ROW_COUNT = 2;

  private final BooleanProperty invalid = new BooleanPropertyBase(false) {
    @Override
    protected void invalidated() {
      pseudoClassStateChanged(INVALID_PSEUDO_CLASS, get());
    }

    @Override
    public Object getBean() {
      return Textarea.this;
    }

    @Override
    public String getName() {
      return "invalid";
    }
  };

  public Textarea() {
    this("");
  }

  public Textarea(String text) {
    super(text);
    getStyleClass().add(ROOT_STYLE_CLASS);
    setPrefRowCount(DEFAULT_PREFERRED_ROW_COUNT);
    setWrapText(true);
  }

  /** Whether this textarea currently fails validation. */
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
