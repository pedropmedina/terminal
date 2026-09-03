package com.acteque.terminal.chart;

import com.acteque.terminal.chart.ChartInterval.Classification;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.Select;
import com.acteque.terminal.ui.core.dialog.Dialog;
import com.acteque.terminal.ui.core.dialog.DialogContent;
import com.acteque.terminal.ui.core.dialog.DialogDescription;
import com.acteque.terminal.ui.core.dialog.DialogFooter;
import com.acteque.terminal.ui.core.dialog.DialogHeader;
import com.acteque.terminal.ui.core.dialog.DialogTitle;
import com.acteque.terminal.ui.core.field.Field;
import com.acteque.terminal.ui.core.field.FieldGroup;
import com.acteque.terminal.ui.core.field.FieldLabel;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.scene.control.TextFormatter;

/** Form dialog for creating a chart interval that lives for the current application session. */
final class AddChartIntervalDialog extends Dialog {

  private final Select<Classification> classification = new Select<>(
    FXCollections.observableArrayList(List.of(Classification.values()))
  );
  private final Input amount = new Input();
  private final Button addButton = new Button("Add");
  private Consumer<ChartInterval> intervalAddedHandler = ignored -> {};

  AddChartIntervalDialog() {
    getStyleClass().add("chart-add-interval-dialog");
    setFocusTraversable(false);

    classification.getStyleClass().add("chart-add-interval-classification");
    classification.setPromptText("Select interval type");
    classification.setAccessibleText("Interval classification");
    classification.setMaxWidth(Double.MAX_VALUE);

    amount.getStyleClass().add("chart-add-interval-amount");
    amount.setPromptText("Enter a number");
    amount.setAccessibleText("Interval amount");
    amount.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));

    FieldLabel classificationLabel = new FieldLabel("Interval type");
    classificationLabel.setLabelFor(classification);
    FieldLabel amountLabel = new FieldLabel("Interval");
    amountLabel.setLabelFor(amount);
    FieldGroup form = new FieldGroup(new Field(classificationLabel, classification), new Field(amountLabel, amount));
    form.getStyleClass().add("chart-add-interval-form");

    Button cancelButton = new Button("Cancel", Button.Variant.OUTLINE, Button.Size.DEFAULT);
    cancelButton.getStyleClass().add("chart-add-interval-cancel");
    cancelButton.setOnAction(ignored -> close());
    addButton.getStyleClass().add("chart-add-interval-submit");
    addButton.setOnAction(ignored -> addInterval());
    amount.setOnAction(ignored -> {
      if (!addButton.isDisabled()) {
        addInterval();
      }
    });

    classification.valueProperty().addListener(ignored -> refreshAddButton());
    amount.textProperty().addListener(ignored -> refreshAddButton());
    refreshAddButton();

    DialogHeader header = new DialogHeader(
      new DialogTitle("Add interval"),
      new DialogDescription("Choose an interval type and enter its numeric value.")
    );
    DialogFooter footer = new DialogFooter(cancelButton, addButton);
    DialogContent card = new DialogContent(header, form, footer);
    card.getStyleClass().add("chart-add-interval-card");
    card.setShowCloseButton(false);
    card.setMaxHeight(USE_PREF_SIZE);
    setContent(card);
  }

  void openForEntry() {
    classification.setValue(null);
    amount.clear();
    show();
  }

  void onIntervalAdded(Consumer<ChartInterval> callback) {
    intervalAddedHandler = Objects.requireNonNull(callback, "callback");
  }

  private void refreshAddButton() {
    addButton.setDisable(classification.getValue() == null || parseAmount() <= 0);
  }

  private int parseAmount() {
    String text = amount.getText();
    if (text == null || text.isBlank()) {
      return 0;
    }
    try {
      return Integer.parseInt(text);
    } catch (NumberFormatException ignored) {
      return 0;
    }
  }

  private void addInterval() {
    int intervalAmount = parseAmount();
    Classification selectedClassification = classification.getValue();
    if (intervalAmount <= 0 || selectedClassification == null) {
      return;
    }
    intervalAddedHandler.accept(ChartInterval.of(intervalAmount, selectedClassification));
    close();
  }
}
