package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Input;
import com.acteque.terminal.ui.core.Toggle;
import com.acteque.terminal.ui.core.Tooltip;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroup;
import com.acteque.terminal.ui.core.togglegroup.ToggleGroupItem;
import org.junit.jupiter.api.Test;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

class ChartIntervalSelectionDialogTest {

  @Test
  void displaysCategorizedIntervalButtonsAndHighlightsTheCurrentInterval() {
    FxTestSupport.runAndWait(() -> {
      ChartIntervalSelectionDialog dialog = createDialog();
      dialog.show();

      assertEquals(27, dialog.lookupAll(".chart-interval-button").size());
      ToggleGroupItem daily = button(dialog, "1D");
      Input input = (Input) dialog.lookup(".chart-interval-search-field");
      assertTrue(daily.isSelected());
      assertFalse(button(dialog, "5M").isSelected());
      assertEquals(Toggle.Variant.OUTLINE, daily.getVariant());
      assertTrue(daily.getProperties().values().stream().anyMatch(Tooltip.class::isInstance));
      assertEquals("System", daily.getFont().getFamily());
      assertEquals(13.0, daily.getFont().getSize());
      assertEquals(8.0, input.getBorder().getStrokes().getFirst().getRadii().getTopLeftHorizontalRadius());
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
  void closesAndReportsTheSelectedInterval() {
    FxTestSupport.runAndWait(() -> {
      SimpleBooleanProperty open = new SimpleBooleanProperty(true);
      ChartIntervalSelectionDialog dialog = new ChartIntervalSelectionDialog(ChartInterval.DAILY, open);
      new Scene(new StackPane(dialog), 800.0, 600.0);
      AtomicReference<ChartInterval> selected = new AtomicReference<>();
      dialog.onIntervalSelected(selected::set);
      dialog.onRequestClose(() -> open.set(false));

      button(dialog, "4H").fire();

      assertEquals(ChartInterval.FOUR_HOURS, selected.get());
      assertFalse(dialog.isOpen());
      assertFalse(open.get());
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
}
