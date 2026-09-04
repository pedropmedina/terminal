package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.Select;
import com.acteque.terminal.ui.core.Toggle;
import com.acteque.terminal.ui.core.Tooltip;
import com.acteque.terminal.ui.core.inputgroup.InputGroup;
import com.acteque.terminal.ui.core.inputgroup.InputGroupAddon;
import com.acteque.terminal.ui.core.inputgroup.InputGroupAlignment;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroupItem;
import com.acteque.terminal.ui.icons.LucideIcon;
import com.acteque.terminal.ui.icons.LucideIcons;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;

class ChartIntervalSelectionDialogTest {

  @Test
  void displaysCategorizedIntervalButtonsAndHighlightsTheCurrentInterval() {
    FxTestSupport.runAndWait(() -> {
      ChartIntervalSelectionDialog dialog = createDialog();
      dialog.show();

      Button addInterval = (Button) dialog.lookup(".chart-interval-add-button");
      LucideIcon addIntervalIcon = assertInstanceOf(LucideIcon.class, addInterval.getGraphic());
      assertEquals("Add interval", addInterval.getText());
      assertSame(LucideIcons.PLUS, addIntervalIcon.getGlyph());
      assertEquals(27, dialog.lookupAll(".chart-interval-button").size());
      ToggleGroupItem daily = button(dialog, "1D");
      Input input = (Input) dialog.lookup(".chart-interval-search-field");
      InputGroup searchGroup = assertInstanceOf(InputGroup.class, input.getParent());
      InputGroupAddon addIntervalAddon = assertInstanceOf(InputGroupAddon.class, addInterval.getParent());
      assertEquals(InputGroupAlignment.INLINE_END, addIntervalAddon.getAlignmentPosition());
      assertSame(searchGroup, addIntervalAddon.getParent());
      assertTrue(daily.isSelected());
      assertFalse(button(dialog, "5M").isSelected());
      assertEquals(Toggle.Variant.OUTLINE, daily.getVariant());
      assertTrue(daily.getProperties().values().stream().anyMatch(Tooltip.class::isInstance));
      assertEquals("System", daily.getFont().getFamily());
      assertEquals(13.0, daily.getFont().getSize());
      assertEquals(8.0, searchGroup.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(6, dialog.lookupAll(".chart-interval-toggle-group").size());
      assertTrue(dialog.lookupAll(".chart-interval-toggle-group").stream().allMatch(ToggleGroup.class::isInstance));
      assertEquals(5, dialog.lookupAll(".chart-interval-category-title").size());
      assertEquals(
        "Ticks",
        dialog
          .lookupAll(".chart-interval-category-title")
          .stream()
          .map(Label.class::cast)
          .findFirst()
          .orElseThrow()
          .getText()
      );
      assertEquals(
        Set.of(
          "1S",
          "5S",
          "10S",
          "15S",
          "30S",
          "45S",
          "1T",
          "10T",
          "100T",
          "1000T",
          "1M",
          "2M",
          "5M",
          "10M",
          "15M",
          "30M",
          "45M",
          "1H",
          "2H",
          "3H",
          "4H",
          "1D",
          "1W",
          "1Mo",
          "3Mo",
          "6Mo",
          "12Mo"
        ),
        dialog
          .lookupAll(".chart-interval-button")
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
      ChartIntervalSelectionDialog dialog = createDialog();
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
      ChartIntervalSelectionDialog dialog = createDialog();
      dialog.show();
      Input field = (Input) dialog.lookup(".chart-interval-search-field");

      field.setText("hour");
      assertEquals(4, dialog.lookupAll(".chart-interval-button").size());
      assertTrue(
        dialog.lookup(".chart-interval-no-matches") == null || !dialog.lookup(".chart-interval-no-matches").isVisible()
      );

      field.setText("1m");
      assertEquals(1, dialog.lookupAll(".chart-interval-button").size());
      assertEquals(
        "1M",
        dialog
          .lookupAll(".chart-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .findFirst()
          .orElseThrow()
          .getText()
      );

      field.setText("unsupported");
      Node noMatches = dialog.lookup(".chart-interval-no-matches");
      assertTrue(noMatches.isVisible());
      assertFalse(dialog.lookup(".chart-interval-categories").isVisible());
    });
  }

  @Test
  void pressingEnterClosesAndReportsTheSelectedInterval() {
    FxTestSupport.runAndWait(() -> {
      SimpleBooleanProperty open = new SimpleBooleanProperty(true);
      ChartIntervalSelectionDialog dialog = new ChartIntervalSelectionDialog(ChartInterval.DAILY, open);
      new Scene(new StackPane(dialog), 800.0, 600.0);
      AtomicReference<ChartInterval> selected = new AtomicReference<>();
      dialog.onIntervalSelected(selected::set);
      dialog.onRequestClose(() -> open.set(false));

      ToggleGroupItem previous = button(dialog, "1D");
      ToggleGroupItem latest = button(dialog, "4H");
      pressEnter(latest);

      assertEquals(ChartInterval.FOUR_HOURS, selected.get());
      assertFalse(previous.isSelected());
      assertTrue(latest.isSelected());
      assertEquals(
        1,
        dialog
          .lookupAll(".chart-interval-button")
          .stream()
          .map(ToggleGroupItem.class::cast)
          .filter(ToggleGroupItem::isSelected)
          .count()
      );
      assertFalse(dialog.isOpen());
      assertFalse(open.get());
    });
  }

  @Test
  void tabsThroughVisibleIntervalsInVisualOrderAndWrapsWithinTheDialog() {
    AtomicReference<ChartIntervalSelectionDialog> dialogReference = new AtomicReference<>();
    AtomicReference<Input> inputReference = new AtomicReference<>();
    AtomicReference<Stage> stageReference = new AtomicReference<>();
    FxTestSupport.runAndWait(() -> {
      ChartIntervalSelectionDialog dialog = createDialog();
      Stage stage = new Stage();
      stage.setScene(dialog.getScene());
      Platform.setImplicitExit(false);
      dialog.show();
      stage.show();
      stage.requestFocus();
      dialogReference.set(dialog);
      inputReference.set((Input) dialog.lookup(".chart-interval-search-field"));
      stageReference.set(stage);
    });

    try {
      FxTestSupport.runAndWait(() -> inputReference.get().requestFocus());
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        pressTab(input, false);
        assertSame(dialogReference.get().lookup(".chart-interval-add-button"), input.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        Node addButton = dialogReference.get().lookup(".chart-interval-add-button");
        pressTab(addButton, false);
        assertSame(button(dialogReference.get(), "1T"), addButton.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        ToggleGroupItem first = button(dialogReference.get(), "1T");
        pressTab(first, false);
        assertSame(button(dialogReference.get(), "10T"), first.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        ToggleGroupItem second = button(dialogReference.get(), "10T");
        pressTab(second, true);
        assertSame(button(dialogReference.get(), "1T"), second.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        input.setText("hour");
        input.requestFocus();
        pressTab(input, false);
        Node addButton = dialogReference.get().lookup(".chart-interval-add-button");
        assertSame(addButton, input.getScene().getFocusOwner());
        pressTab(addButton, false);
        assertSame(button(dialogReference.get(), "1H"), input.getScene().getFocusOwner());

        input.requestFocus();
        pressTab(input, true);
        assertSame(button(dialogReference.get(), "4H"), input.getScene().getFocusOwner());

        ToggleGroupItem last = button(dialogReference.get(), "4H");
        pressTab(last, false);
        assertSame(input, last.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        Input input = inputReference.get();
        input.setText("1d");
        input.requestFocus();
        pressTab(input, false);
        pressTab(dialogReference.get().lookup(".chart-interval-add-button"), false);
        ToggleGroupItem selected = button(dialogReference.get(), "1D");

        assertSame(selected, input.getScene().getFocusOwner());
      });
      FxTestSupport.runAndWait(() -> {
        ToggleGroupItem selected = button(dialogReference.get(), "1D");
        selected.getScene().getRoot().applyCss();

        assertEquals(2, selected.getBorder().getStrokes().size());
      });
    } finally {
      FxTestSupport.runAndWait(() -> stageReference.get().close());
      FxTestSupport.runAndWait(() -> {});
    }
  }

  @Test
  void addsANumericCustomIntervalToTheInMemoryChoices() {
    FxTestSupport.runAndWait(() -> {
      ChartIntervalSelectionDialog dialog = createDialog();
      AtomicReference<ChartInterval> selected = new AtomicReference<>();
      dialog.onIntervalSelected(selected::set);
      dialog.show();

      ((Button) dialog.lookup(".chart-interval-add-button")).fire();
      Node addDialog = dialog.lookup(".chart-add-interval-dialog");
      assertTrue(addDialog.isVisible());
      assertTrue(dialog.getContent().isDisabled());

      @SuppressWarnings("unchecked")
      Select<ChartInterval.Classification> classification = (Select<ChartInterval.Classification>) dialog.lookup(
        ".chart-add-interval-classification"
      );
      Input amount = (Input) dialog.lookup(".chart-add-interval-amount");
      Button submit = (Button) dialog.lookup(".chart-add-interval-submit");
      assertTrue(submit.isDisabled());

      amount.setText("7hours");
      assertEquals("", amount.getText());
      classification.setValue(ChartInterval.Classification.HOURS);
      amount.setText("7");
      assertFalse(submit.isDisabled());
      submit.fire();

      assertFalse(addDialog.isVisible());
      assertFalse(dialog.getContent().isDisabled());
      assertEquals(28, dialog.lookupAll(".chart-interval-button").size());
      ToggleGroupItem custom = button(dialog, "7H");
      custom.fire();
      assertEquals("7H", selected.get().displayName());
      assertEquals("Hours", selected.get().category());
    });
  }

  @Test
  void cancellingTheAddDialogLeavesTheIntervalChoicesUnchanged() {
    FxTestSupport.runAndWait(() -> {
      ChartIntervalSelectionDialog dialog = createDialog();
      dialog.show();

      ((Button) dialog.lookup(".chart-interval-add-button")).fire();
      ((Button) dialog.lookup(".chart-add-interval-cancel")).fire();

      assertEquals(27, dialog.lookupAll(".chart-interval-button").size());
      assertFalse(dialog.lookup(".chart-add-interval-dialog").isVisible());
    });
  }

  private static ChartIntervalSelectionDialog createDialog() {
    ChartIntervalSelectionDialog dialog = new ChartIntervalSelectionDialog(
      ChartInterval.DAILY,
      new SimpleBooleanProperty(false)
    );
    StackPane root = new StackPane(dialog);
    new ThemeManager(new Scene(root, 800.0, 600.0), AppTheme.LIGHT);
    root.applyCss();
    return dialog;
  }

  private static ToggleGroupItem button(ChartIntervalSelectionDialog dialog, String text) {
    return dialog
      .lookupAll(".chart-interval-button")
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
