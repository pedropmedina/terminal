package com.acteque.terminal;

import java.util.Objects;
import javafx.beans.value.ObservableBooleanValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderStroke;
import javafx.scene.layout.BorderStrokeStyle;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/** A transient symbol picker. Selection is intentionally not applied to market data yet. */
final class InstrumentSearchDialog extends StackPane {

  private static final double MODAL_WIDTH = 320.0;

  private final TextField symbolField = new TextField();
  private Runnable overlayClickHandler = () -> {};
  private Runnable closeRequestHandler = () -> {};

  InstrumentSearchDialog(String currentSymbol, ObservableBooleanValue open) {
    symbolField.setText(Objects.requireNonNull(currentSymbol, "currentSymbol"));
    Objects.requireNonNull(open, "open");

    visibleProperty().bind(open);
    managedProperty().bind(open);
    open.addListener((ignored, wasOpen, isOpen) -> {
      if (isOpen) {
        symbolField.selectAll();
        symbolField.requestFocus();
      }
    });
    setPickOnBounds(true);
    setAlignment(Pos.CENTER);
    setBackground(new Background(new BackgroundFill(Color.rgb(15, 18, 24, 0.42), CornerRadii.EMPTY, Insets.EMPTY)));

    Label title = new Label("Select stock symbol");
    title.setFont(Font.font("System", FontWeight.SEMI_BOLD, 18.0));

    symbolField.setPromptText("Symbol");
    symbolField.setAccessibleText("Stock symbol");

    Button cancelButton = new Button("Cancel");
    cancelButton.setOnAction(event -> closeRequestHandler.run());
    Button selectButton = new Button("Select");
    selectButton.setDefaultButton(true);
    selectButton.setOnAction(event -> closeRequestHandler.run());

    HBox actions = new HBox(8.0, cancelButton, selectButton);
    actions.setAlignment(Pos.CENTER_RIGHT);

    VBox card = new VBox(16.0, title, symbolField, actions);
    card.setPrefWidth(MODAL_WIDTH);
    card.setMaxWidth(MODAL_WIDTH);
    card.setPadding(new Insets(24.0));
    card.setBackground(new Background(new BackgroundFill(Color.WHITE, new CornerRadii(10.0), Insets.EMPTY)));
    card.setBorder(
      new Border(new BorderStroke(Color.rgb(220, 223, 229), BorderStrokeStyle.SOLID, new CornerRadii(10.0), null))
    );
    card.setOnMouseClicked(event -> event.consume());

    getChildren().setAll(card);
    setOnMouseClicked(event -> overlayClickHandler.run());
    addEventFilter(KeyEvent.KEY_PRESSED, event -> {
      if (event.getCode() == KeyCode.ESCAPE) {
        closeRequestHandler.run();
        event.consume();
      }
    });
  }

  void onOverlayClick(Runnable callback) {
    overlayClickHandler = Objects.requireNonNull(callback, "callback");
  }

  void onRequestClose(Runnable callback) {
    closeRequestHandler = Objects.requireNonNull(callback, "callback");
  }
}
