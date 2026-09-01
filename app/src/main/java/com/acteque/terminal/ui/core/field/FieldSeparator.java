package com.acteque.terminal.ui.core.field;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/** Horizontal divider with optional centered content. */
public final class FieldSeparator extends StackPane {

  private static final PseudoClass CONTENT_PSEUDO_CLASS = PseudoClass.getPseudoClass("content");

  private final com.acteque.terminal.ui.core.Separator line = new com.acteque.terminal.ui.core.Separator();
  private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content") {
    @Override
    protected void invalidated() {
      rebuild();
    }
  };

  public FieldSeparator() {
    this((Node) null);
  }

  public FieldSeparator(String text) {
    this(text == null ? null : new javafx.scene.control.Label(text));
  }

  public FieldSeparator(Node content) {
    getStyleClass().add("core-field-separator");
    setAlignment(Pos.CENTER);
    setContent(content);
  }

  public final ObjectProperty<Node> contentProperty() {
    return content;
  }

  public final Node getContent() {
    return content.get();
  }

  public final void setContent(Node value) {
    content.set(value);
  }

  private void rebuild() {
    getChildren().setAll(line);
    if (getContent() != null) {
      getContent().getStyleClass().add("field-separator-content");
      getChildren().add(getContent());
    }
    pseudoClassStateChanged(CONTENT_PSEUDO_CLASS, getContent() != null);
  }
}
