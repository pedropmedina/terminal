package com.acteque.terminal.chartworkspace.intervalselection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.AppTheme;
import com.acteque.terminal.AppThemeManager;
import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.ChartInterval.Classification;
import com.acteque.terminal.chart.ChartIntervalHistoryMapper;
import com.acteque.terminal.chart.ChartIntervalText;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.Input;
import com.acteque.terminal.ui.Select;
import com.acteque.terminal.ui.Toggle;
import com.acteque.terminal.ui.dialog.Dialog;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import com.acteque.terminal.ui.inputgroup.InputGroup;
import com.acteque.terminal.ui.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.togglegroup.ToggleGroupItem;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class ChartWorkspaceIntervalSelectionDialogTest {

  @Test
  void disablesUnavailableIntervalsWithoutRemovingThem() {
    FxTestSupport.runAndWait(() -> {
      ChartWorkspaceIntervalSelection selection = new ChartWorkspaceIntervalSelection(ChartInterval.DAILY);
      selection.setAvailability(interval -> ChartIntervalHistoryMapper.map(interval).isPresent());
      Dialog dialog = selection.getView();
      new Scene(new StackPane(dialog), 800.0, 600.0);
      selection.show();

      assertTrue(button(dialog, "1s").isDisabled());
      assertTrue(button(dialog, "3mo").isDisabled());
      assertFalse(button(dialog, "5m").isDisabled());
      assertFalse(button(dialog, "Y").isDisabled());

      ((Button) dialog.lookup(".chart-workspace-interval-add-button")).fire();
      @SuppressWarnings("unchecked")
      Select<Classification> classification = (Select<Classification>) dialog.lookup(
        ".chart-workspace-add-interval-classification"
      );
      classification.setValue(Classification.DAYS);
      ((Input) dialog.lookup(".chart-workspace-add-interval-amount")).setText("2");
      assertTrue(((Button) dialog.lookup(".chart-workspace-add-interval-submit")).isDisabled());
    });
  }

  @Test
  void displaysCategorizedIntervalButtonsAndHighlightsTheCurrentInterval() {
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = createFeature().view();
      dialog.show();

      Button addInterval = (Button) dialog.lookup(".chart-workspace-interval-add-button");
      LucideIcon addIntervalIcon = assertInstanceOf(LucideIcon.class, addInterval.getGraphic());
      assertEquals("Add interval", addInterval.getText());
      assertSame(LucideIcons.PLUS, addIntervalIcon.getGlyph());
      assertEquals(27, dialog.lookupAll(".chart-workspace-interval-button").size());
      ToggleGroupItem daily = button(dialog, "D");
      Input input = (Input) dialog.lookup(".chart-workspace-interval-search-field");
      InputGroup searchGroup = assertInstanceOf(InputGroup.class, input.getParent());
      InputGroupAddon addIntervalAddon = assertInstanceOf(InputGroupAddon.class, addInterval.getParent());
      assertEquals(InputGroupAlignment.INLINE_END, addIntervalAddon.getAlignmentPosition());
      assertSame(searchGroup, addIntervalAddon.getParent());
      assertTrue(daily.isSelected());
      assertFalse(button(dialog, "5m").isSelected());
      assertEquals(Toggle.Variant.OUTLINE, daily.getVariant());
      assertEquals("System", daily.getFont().getFamily());
      assertEquals(13.0, daily.getFont().getSize());
      assertEquals(8.0, searchGroup.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(6, dialog.lookupAll(".chart-workspace-interval-toggle-group").size());
      assertTrue(
        dialog.lookupAll(".chart-workspace-interval-toggle-group").stream().allMatch(ToggleGroup.class::isInstance)
      );
      assertEquals(5, dialog.lookupAll(".chart-workspace-interval-category-title").size());
      assertEquals(
        "Ticks",
        dialog
          .lookupAll(".chart-workspace-interval-category-title")
          .stream()
          .map(Label.class::cast)
          .findFirst()
          .orElseThrow()
          .getText()
      );
      assertEquals(
        Set.of(
          "1s",
          "5s",
          "10s",
          "15s",
          "30s",
          "45s",
          "1t",
          "10t",
          "100t",
          "1000t",
          "1m",
          "2m",
          "5m",
          "10m",
          "15m",
          "30m",
          "45m",
          "1h",
          "2h",
          "3h",
          "4h",
          "D",
          "W",
          "M",
          "3mo",
          "6mo",
          "Y"
        ),
        dialog
          .lookupAll(".chart-workspace-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .map(ToggleGroupItem::getText)
          .collect(Collectors.toSet())
      );
    });
  }

  @Test
  void sizesTheVisibleCardToItsContent() {
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = createFeature().view();
      StackPane root = (StackPane) dialog.getParent();
      dialog.show();
      root.applyCss();
      root.layout();

      assertEquals(
        dialog.getContent().prefHeight(dialog.getContent().getWidth()),
        dialog.getContent().getHeight(),
        0.5
      );
      assertTrue(dialog.getContent().getHeight() < dialog.getHeight());
    });
  }

  @Test
  void filtersIntervalsAndShowsAnEmptyState() {
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = createFeature().view();
      dialog.show();
      Input field = (Input) dialog.lookup(".chart-workspace-interval-search-field");

      field.setText("hour");
      assertEquals(4, dialog.lookupAll(".chart-workspace-interval-button").size());
      assertTrue(
        dialog.lookup(".chart-workspace-interval-no-matches") == null ||
          !dialog.lookup(".chart-workspace-interval-no-matches").isVisible()
      );

      field.setText("1m");
      assertEquals(1, dialog.lookupAll(".chart-workspace-interval-button").size());
      assertEquals(
        "1m",
        dialog
          .lookupAll(".chart-workspace-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .findFirst()
          .orElseThrow()
          .getText()
      );

      field.setText("unsupported");
      Node noMatches = dialog.lookup(".chart-workspace-interval-no-matches");
      assertTrue(noMatches.isVisible());
      assertFalse(dialog.lookup(".chart-workspace-interval-categories").isVisible());
    });
  }

  @Test
  void pressingEnterClosesAndReportsTheSelectedInterval() {
    FxTestSupport.runAndWait(() -> {
      ChartWorkspaceIntervalSelection selection = new ChartWorkspaceIntervalSelection(ChartInterval.DAILY);
      Dialog dialog = selection.getView();
      new Scene(new StackPane(dialog), 800.0, 600.0);
      AtomicReference<ChartInterval> selected = new AtomicReference<>();
      selection.onIntervalSelected(selected::set);
      selection.show();

      ToggleGroupItem previous = button(dialog, "D");
      ToggleGroupItem latest = button(dialog, "4h");
      pressEnter(latest);

      assertEquals(ChartInterval.FOUR_HOURS, selected.get());
      assertFalse(previous.isSelected());
      assertTrue(latest.isSelected());
      assertEquals(
        1,
        dialog
          .lookupAll(".chart-workspace-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .filter(ToggleGroupItem::isSelected)
          .count()
      );
      assertFalse(dialog.isOpen());
      assertFalse(selection.openProperty().get());
    });
  }

  @Test
  void tabsThroughVisibleIntervalsInVisualOrderAndWrapsWithinTheDialog() {
    AtomicReference<Dialog> dialogReference = new AtomicReference<>();
    AtomicReference<Input> inputReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = createFeature().view();
      Stage stage = new Stage();
      stage.setScene(dialog.getScene());
      Platform.setImplicitExit(false);
      dialog.show();
      stage.show();
      stage.requestFocus();
      dialogReference.set(dialog);
      inputReference.set((Input) dialog.lookup(".chart-workspace-interval-search-field"));
      stageReference.set(stage);
    });

    try {
      FxTestSupport.runAndWait(() -> inputReference.get().requestFocus());
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        pressTab(input, false);
        assertSame(
          dialogReference.get().lookup(".chart-workspace-interval-add-button"),
          input.getScene().getFocusOwner()
        );
      });
      FxTestSupport.runAndWait(() -> {
        Node addButton = dialogReference.get().lookup(".chart-workspace-interval-add-button");
        pressTab(addButton, false);
        assertSame(button(dialogReference.get(), "1t"), addButton.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        ToggleGroupItem first = button(dialogReference.get(), "1t");
        pressTab(first, false);
        assertSame(button(dialogReference.get(), "10t"), first.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        ToggleGroupItem second = button(dialogReference.get(), "10t");
        pressTab(second, true);
        assertSame(button(dialogReference.get(), "1t"), second.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        input.setText("hour");
        input.requestFocus();
        pressTab(input, false);
        Node addButton = dialogReference.get().lookup(".chart-workspace-interval-add-button");
        assertSame(addButton, input.getScene().getFocusOwner());
        pressTab(addButton, false);
        assertSame(button(dialogReference.get(), "1h"), input.getScene().getFocusOwner());

        input.requestFocus();
        pressTab(input, true);
        assertSame(button(dialogReference.get(), "4h"), input.getScene().getFocusOwner());

        ToggleGroupItem last = button(dialogReference.get(), "4h");
        pressTab(last, false);
        assertSame(input, last.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        input.setText("D");
        input.requestFocus();
        pressTab(input, false);
        pressTab(dialogReference.get().lookup(".chart-workspace-interval-add-button"), false);
        ToggleGroupItem selected = button(dialogReference.get(), "D");

        assertSame(selected, input.getScene().getFocusOwner());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
      FxTestSupport.runAndWait(() -> {});
    }
  }

  @Test
  void addsANumericCustomIntervalToTheInMemoryChoices() {
    FxTestSupport.runAndWait(() -> {
      IntervalSelectionFeature feature = createFeature();
      Dialog dialog = feature.view();
      AtomicReference<ChartInterval> selected = new AtomicReference<>();
      feature.selection().onIntervalSelected(selected::set);
      dialog.show();

      ((Button) dialog.lookup(".chart-workspace-interval-add-button")).fire();
      Node addDialog = dialog.lookup(".chart-workspace-add-interval-dialog");
      assertTrue(addDialog.isVisible());
      assertTrue(dialog.getContent().isDisabled());

      @SuppressWarnings("unchecked")
      Select<ChartInterval.Classification> classification = (Select<ChartInterval.Classification>) dialog.lookup(
        ".chart-workspace-add-interval-classification"
      );
      Input amount = (Input) dialog.lookup(".chart-workspace-add-interval-amount");
      Button submit = (Button) dialog.lookup(".chart-workspace-add-interval-submit");
      assertTrue(submit.isDisabled());

      amount.setText("7hours");
      assertEquals("", amount.getText());
      classification.setValue(ChartInterval.Classification.HOURS);
      amount.setText("7");
      assertFalse(submit.isDisabled());
      submit.fire();

      assertFalse(addDialog.isVisible());
      assertFalse(dialog.getContent().isDisabled());
      assertEquals(28, dialog.lookupAll(".chart-workspace-interval-button").size());
      ToggleGroupItem custom = button(dialog, "7h");
      custom.fire();
      assertEquals("7h", selected.get().name());
      assertEquals("Hours", ChartIntervalText.category(selected.get()));
    });
  }

  @Test
  void cancellingTheAddDialogLeavesTheIntervalChoicesUnchanged() {
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = createFeature().view();
      dialog.show();

      ((Button) dialog.lookup(".chart-workspace-interval-add-button")).fire();
      ((Button) dialog.lookup(".chart-workspace-add-interval-cancel")).fire();

      assertEquals(27, dialog.lookupAll(".chart-workspace-interval-button").size());
      assertFalse(dialog.lookup(".chart-workspace-add-interval-dialog").isVisible());
    });
  }

  private static IntervalSelectionFeature createFeature() {
    ChartWorkspaceIntervalSelection selection = new ChartWorkspaceIntervalSelection(ChartInterval.DAILY);
    Dialog dialog = selection.getView();
    StackPane root = new StackPane(dialog);
    new AppThemeManager(new Scene(root, 800.0, 600.0), AppTheme.LIGHT);
    selection.show();
    root.applyCss();
    return new IntervalSelectionFeature(selection, dialog);
  }

  private record IntervalSelectionFeature(ChartWorkspaceIntervalSelection selection, Dialog view) {}

  private static ToggleGroupItem button(Dialog dialog, String text) {
    return dialog
      .lookupAll(".chart-workspace-interval-button")
      .stream()
      .map(ToggleGroupItem.class::cast)
      .filter(button -> text.equals(button.getText()))
      .findFirst()
      .orElseThrow();
  }

  private static void pressTab(Node target, boolean shiftDown) {
    target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.TAB, shiftDown, false, false, false));
  }

  private static void pressEnter(Node target) {
    target.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER, false, false, false, false));
  }
}
