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
import java.util.function.Predicate;
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
  private final Predicate<ChartInterval> availability;
  private Consumer<ChartInterval> intervalAddedHandler = ignored -> {};

  /**
   * Creates and composes the custom-interval entry dialog.
   *
   * @param availability the interval support check owned by the feature model
   */
  ChartWorkspaceAddIntervalDialogView(Predicate<ChartInterval> availability) {
    this.availability = Objects.requireNonNull(availability, "availability cannot be null");

    configureDialog();
    configureClassification();
    configureAmount();
    connectComponents();
    composeDialog();
    refreshAddButton();
  }

  /** Clears previous input and opens the dialog for a new custom interval. */
  void openForEntry() {
    classification.setValue(null);
    amount.clear();
    show();
  }

  /**
   * Registers the action invoked after a valid custom interval is submitted.
   *
   * @param callback the custom-interval callback
   */
  void onIntervalAdded(Consumer<ChartInterval> callback) {
    intervalAddedHandler = Objects.requireNonNull(callback, "callback cannot be null");
  }

  /** Re-evaluates the current entry after the active chart's availability changes. */
  void refreshAvailability() {
    refreshAddButton();
  }

  /** Configures the nested dialog's stable behavior and styling. */
  private void configureDialog() {
    getStyleClass().add("chart-workspace-add-interval-dialog");
    setFocusTraversable(false);
  }

  /** Configures interval-classification choices and their display conversion. */
  private void configureClassification() {
    classification.getStyleClass().add("chart-workspace-add-interval-classification");
    classification.setPromptText("Select interval type");
    classification.setAccessibleText("Interval classification");
    classification.setMaxWidth(Double.MAX_VALUE);
    classification.setConverter(
      new StringConverter<>() {
        /**
         * Returns the human-readable classification name.
         *
         * @param value the classification to display
         * @return the display name, or an empty string for no selection
         */
        @Override
        public String toString(Classification value) {
          return value == null ? "" : ChartIntervalText.classificationName(value);
        }

        /**
         * Rejects text parsing because classifications are selected from fixed choices.
         *
         * @param value the unsupported entered text
         * @return no value because this method always throws
         * @throws UnsupportedOperationException always
         */
        @Override
        public Classification fromString(String value) {
          throw new UnsupportedOperationException("Interval classifications are selected, not parsed");
        }
      }
    );
  }

  /** Configures numeric-only interval amount entry. */
  private void configureAmount() {
    amount.getStyleClass().add("chart-workspace-add-interval-amount");
    amount.setPromptText("Enter a number");
    amount.setAccessibleText("Interval amount");
    amount.setTextFormatter(new TextFormatter<>(change -> change.getControlNewText().matches("\\d*") ? change : null));
  }

  /** Connects entry changes and submission controls to dialog intents. */
  private void connectComponents() {
    addButton.getStyleClass().add("chart-workspace-add-interval-submit");
    addButton.setOnAction(ignored -> addInterval());
    amount.setOnAction(ignored -> {
      if (!addButton.isDisabled()) {
        addInterval();
      }
    });
    classification.valueProperty().addListener(ignored -> refreshAddButton());
    amount.textProperty().addListener(ignored -> refreshAddButton());
  }

  /** Composes field labels, controls, and actions into the dialog card. */
  private void composeDialog() {
    FieldLabel classificationLabel = new FieldLabel("Interval type");
    classificationLabel.setLabelFor(classification);
    FieldLabel amountLabel = new FieldLabel("Interval");
    amountLabel.setLabelFor(amount);
    FieldGroup form = new FieldGroup(new Field(classificationLabel, classification), new Field(amountLabel, amount));
    form.getStyleClass().add("chart-workspace-add-interval-form");

    Button cancelButton = new Button("Cancel", Button.Variant.OUTLINE, Button.Size.DEFAULT);
    cancelButton.getStyleClass().add("chart-workspace-add-interval-cancel");
    cancelButton.setOnAction(ignored -> close());

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

  /** Enables submission only when the current entry describes an available positive interval. */
  private void refreshAddButton() {
    Classification type = classification.getValue();
    int value = parseAmount();
    boolean available = type != null && value > 0 && availability.test(ChartInterval.of(value, type));
    addButton.setDisable(!available);
  }

  /**
   * Parses the entered interval amount without surfacing invalid input exceptions.
   *
   * @return the positive or zero entered amount, or zero for absent or out-of-range input
   */
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

  /** Submits a valid available interval and closes the entry dialog. */
  private void addInterval() {
    int intervalAmount = parseAmount();
    Classification selectedClassification = classification.getValue();
    if (
      intervalAmount <= 0 ||
      selectedClassification == null ||
      !availability.test(ChartInterval.of(intervalAmount, selectedClassification))
    ) {
      return;
    }
    intervalAddedHandler.accept(ChartInterval.of(intervalAmount, selectedClassification));
    close();
  }
}
