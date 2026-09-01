package com.acteque.terminal.ui.core;

import javafx.beans.InvalidationListener;
import javafx.css.PseudoClass;
import javafx.scene.Node;

/** A compact label styled for form controls and other design-system content. */
public class Label extends javafx.scene.control.Label {

  private static final String ROOT_STYLE_CLASS = "core-label";
  private static final PseudoClass LABEL_FOR_DISABLED_PSEUDO_CLASS = PseudoClass.getPseudoClass("label-for-disabled");

  private final InvalidationListener labelForDisabledListener = ignored -> refreshLabelForDisabledState();
  private Node observedLabelFor;

  public Label() {
    this(null, null);
  }

  public Label(String text) {
    this(text, null);
  }

  public Label(String text, Node graphic) {
    super(text, graphic);
    getStyleClass().add(ROOT_STYLE_CLASS);
    labelForProperty().addListener((observable, previous, current) -> observeLabelFor(current));
    observeLabelFor(getLabelFor());
  }

  private void observeLabelFor(Node labelFor) {
    if (observedLabelFor != null) {
      observedLabelFor.disabledProperty().removeListener(labelForDisabledListener);
    }
    observedLabelFor = labelFor;
    if (observedLabelFor != null) {
      observedLabelFor.disabledProperty().addListener(labelForDisabledListener);
    }
    refreshLabelForDisabledState();
  }

  private void refreshLabelForDisabledState() {
    pseudoClassStateChanged(LABEL_FOR_DISABLED_PSEUDO_CLASS, observedLabelFor != null && observedLabelFor.isDisabled());
  }
}
