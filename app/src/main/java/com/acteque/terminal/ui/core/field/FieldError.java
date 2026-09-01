package com.acteque.terminal.ui.core.field;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/** Validation feedback that deduplicates messages and exposes them to assistive technology. */
public final class FieldError extends VBox {

  private final ObservableList<String> errors = FXCollections.observableArrayList();
  private final ObjectProperty<Node> content = new SimpleObjectProperty<>(this, "content") {
    @Override
    protected void invalidated() {
      rebuild();
    }
  };

  public FieldError() {
    this(List.<String>of());
  }

  public FieldError(String message) {
    this(message == null ? List.of() : List.of(message));
  }

  public FieldError(Collection<String> errors) {
    getStyleClass().add("core-field-error");
    setAccessibleRole(AccessibleRole.TEXT);
    this.errors.addListener((ListChangeListener<String>) change -> rebuild());
    setErrors(errors);
  }

  public final ObservableList<String> getErrors() {
    return errors;
  }

  public final void setErrors(Collection<String> values) {
    errors.setAll(Objects.requireNonNull(values, "errors"));
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
    getChildren().clear();
    if (getContent() != null) {
      getChildren().add(getContent());
    } else {
      LinkedHashSet<String> uniqueMessages = uniqueMessages();
      boolean multiple = uniqueMessages.size() > 1;
      uniqueMessages.forEach(message -> {
        javafx.scene.control.Label item = new javafx.scene.control.Label(multiple ? "\u2022 " + message : message);
        item.setWrapText(true);
        item.getStyleClass().add("field-error-item");
        getChildren().add(item);
      });
    }
    boolean hasContent = !getChildren().isEmpty();
    setManaged(hasContent);
    setVisible(hasContent);
    setAccessibleText(accessibleText());
  }

  private String accessibleText() {
    if (getContent() != null) {
      return Objects.requireNonNullElse(getContent().getAccessibleText(), "");
    }
    return String.join(". ", uniqueMessages());
  }

  private LinkedHashSet<String> uniqueMessages() {
    LinkedHashSet<String> messages = new LinkedHashSet<>();
    errors
      .stream()
      .filter(Objects::nonNull)
      .filter(message -> !message.isBlank())
      .forEach(messages::add);
    return messages;
  }
}
