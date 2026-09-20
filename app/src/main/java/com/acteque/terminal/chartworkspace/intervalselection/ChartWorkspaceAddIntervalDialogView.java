package com.acteque.terminal.chartworkspace.intervalselection;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartInterval.Classification;
import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.ui.Button;
import com.acteque.terminal.ui.Input;
import com.acteque.terminal.ui.Select;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.dialog.DialogContent;
import com.acteque.terminal.ui.dialog.DialogDescription;
import com.acteque.terminal.ui.dialog.DialogFooter;
import com.acteque.terminal.ui.dialog.DialogHeader;
import com.acteque.terminal.ui.dialog.DialogTitle;
import com.acteque.terminal.ui.field.Field;
import com.acteque.terminal.ui.field.FieldGroup;
import com.acteque.terminal.ui.field.FieldLabel;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

/** Form dialog for creating a chart interval that lives for the current application session. */
final class ChartWorkspaceAddIntervalDialogView extends Dialog {

  private final Select<Classification> classification = new Select<>(
    FXCollections.observableArrayList(List.of(Classification.values()))
  );
  private final Input amount = new Input();
  private final Button addButton = new Button("Add");
  private Consumer<ChartInterval> intervalAddedHandler = ignored -> {};

  ChartWorkspaceAddIntervalDialogView() {
    getStyleClass().add("chart-workspace-add-interval-dialog");
    setFocusTraversable(false);

    classification.getStyleClass().add("chart-workspace-add-interval-classification");
    classification.setPromptText("Select interval type");
    classification.setAccessibleText("Interval classification");
    classification.setMaxWidth(Double.MAX_VALUE);
    classification.setConverter(
      new StringConverter<>() {
        @Override
        public String toString(Classification value) {
          return value == null ? "" : ChartIntervalText.classificationName(value);
        }

        @Override
        public Classification fromString(String value) {
          throw new UnsupportedOperationException("Interval classifications are selected, not parsed");
        }
      }
    );

    amount.getStyleClass().add("chart-workspace-add-interval-amount");
    amount.setPromptText("Enter a number");
    amount.setAccessibleText("Interval amount");
    amount.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));

    FieldLabel classificationLabel = new FieldLabel("Interval type");
    classificationLabel.setLabelFor(classification);
    FieldLabel amountLabel = new FieldLabel("Interval");
    amountLabel.setLabelFor(amount);
    FieldGroup form = new FieldGroup(new Field(classificationLabel, classification), new Field(amountLabel, amount));
    form.getStyleClass().add("chart-workspace-add-interval-form");

    Button cancelButton = new Button("Cancel", Button.Variant.OUTLINE, Button.Size.DEFAULT);
    cancelButton.getStyleClass().add("chart-workspace-add-interval-cancel");
    cancelButton.setOnAction(ignored -> close());
    addButton.getStyleClass().add("chart-workspace-add-interval-submit");
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
    card.getStyleClass().add("chart-workspace-add-interval-card");
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
    intervalAddedHandler = Objects.requireNonNull(callback, "callback cannot be null");
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
