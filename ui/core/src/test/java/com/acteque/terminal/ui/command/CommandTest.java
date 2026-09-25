package com.acteque.terminal.ui.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.css.PseudoClass;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

/** Behavioral and styling coverage for the composable command component family. */
class CommandTest {

  private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

  /** Verifies composition, stable filtering, derived visibility, and synchronized search state. */
  @Test
  void composesAndFiltersItemsGroupsEmptyStateAndSeparators() {
    FxTestSupport.runAndWait(() -> {
      Command command = new Command();
      CommandInput input = new CommandInput(command);
      CommandItem apple = new CommandItem(command, "Apple");
      CommandItem banana = new CommandItem(command, "Banana");
      banana.getKeywords().add("yellow fruit");
      CommandItem carrot = new CommandItem(command, "Carrot");
      CommandGroup fruit = new CommandGroup(command, "Fruit", apple, banana);
      CommandGroup vegetables = new CommandGroup(command, "Vegetables", carrot);
      CommandSeparator separator = new CommandSeparator(command);
      CommandEmpty empty = new CommandEmpty(command, "No results found.");
      CommandList list = new CommandList(command, empty, fruit, separator, vegetables);
      command.getChildren().addAll(input, list);

      assertEquals(AccessibleRole.COMBO_BOX, command.getAccessibleRole());
      assertEquals(AccessibleRole.LIST_VIEW, list.getAccessibleRole());
      assertSame(apple, command.getHighlightedItem());

      input.getEditor().setText("yellow");

      assertFalse(apple.isVisible());
      assertTrue(banana.isVisible());
      assertFalse(carrot.isVisible());
      assertTrue(fruit.isVisible());
      assertFalse(vegetables.isVisible());
      assertFalse(separator.isVisible());
      assertFalse(empty.isVisible());
      assertSame(banana, command.getHighlightedItem());

      command.setSearchText("missing");

      assertTrue(empty.isVisible());
      assertNull(command.getHighlightedItem());
      assertEquals("missing", input.getEditor().getText());
      assertEquals("missing", command.queryAccessibleAttribute(AccessibleAttribute.TEXT));

      command.setFilter(null);

      assertTrue(apple.isVisible());
      assertTrue(banana.isVisible());
      assertTrue(carrot.isVisible());
      assertFalse(empty.isVisible());

      input.dispose();
      list.dispose();
      empty.dispose();
      separator.dispose();
      fruit.dispose();
      vegetables.dispose();
      apple.dispose();
      banana.dispose();
      carrot.dispose();
    });
  }

  /** Verifies keyboard boundaries, disabled-item skipping, looping, and action dispatch. */
  @Test
  void navigatesSkipsDisabledItemsLoopsAndInvokesTheHighlightedAction() {
    FxTestSupport.runAndWait(() -> {
      Command command = new Command();
      CommandInput input = new CommandInput(command);
      CommandItem one = new CommandItem(command, "One");
      CommandItem two = new CommandItem(command, "Two");
      CommandItem three = new CommandItem(command, "Three");
      two.setDisable(true);
      AtomicInteger invocations = new AtomicInteger();
      three.setOnAction(ignored -> invocations.incrementAndGet());
      CommandList list = new CommandList(command, one, two, three);
      command.getChildren().addAll(input, list);

      input.getEditor().fireEvent(keyPressed(KeyCode.DOWN));

      assertSame(three, command.getHighlightedItem());
      assertFalse(two.getPseudoClassStates().contains(SELECTED));
      assertTrue(three.getPseudoClassStates().contains(SELECTED));

      input.getEditor().fireEvent(keyPressed(KeyCode.ENTER));

      assertEquals(1, invocations.get());
      assertEquals("", command.getSearchText());

      input.getEditor().fireEvent(keyPressed(KeyCode.DOWN));
      assertSame(three, command.getHighlightedItem());

      command.setLoop(true);
      input.getEditor().fireEvent(keyPressed(KeyCode.DOWN));
      assertSame(one, command.getHighlightedItem());
      input.getEditor().fireEvent(keyPressed(KeyCode.END));
      assertSame(three, command.getHighlightedItem());
      input.getEditor().fireEvent(keyPressed(KeyCode.HOME));
      assertSame(one, command.getHighlightedItem());

      input.dispose();
      list.dispose();
      one.dispose();
      two.dispose();
      three.dispose();
    });
  }

  /** Verifies checked indicators and shortcut-specific indicator suppression. */
  @Test
  void rendersCheckedIndicatorsAndLetsShortcutsTakeTheirPlace() {
    FxTestSupport.runAndWait(() -> {
      Command command = new Command();
      CommandItem unchecked = new CommandItem(command, "Unchecked");
      CommandItem checked = new CommandItem(command, "Checked");
      checked.setChecked(true);
      CommandShortcut shortcut = new CommandShortcut("⌘K");
      CommandItem withShortcut = new CommandItem(
        command,
        "Shortcut",
        new javafx.scene.control.Label("Shortcut"),
        shortcut
      );

      Region uncheckedSpacer = (Region) unchecked.getItemContent().getChildren().get(1);
      Region uncheckedIndicator = (Region) unchecked.getItemContent().getChildren().getLast();
      Region checkedSpacer = (Region) checked.getItemContent().getChildren().get(1);
      Region checkedIndicator = (Region) checked.getItemContent().getChildren().getLast();
      Region shortcutSpacer = (Region) withShortcut.getItemContent().getChildren().get(1);
      Region shortcutIndicator = (Region) withShortcut.getItemContent().getChildren().getLast();

      assertFalse(uncheckedSpacer.isManaged());
      assertFalse(uncheckedSpacer.isVisible());
      assertFalse(uncheckedIndicator.isManaged());
      assertFalse(uncheckedIndicator.isVisible());
      assertTrue(checkedSpacer.isManaged());
      assertTrue(checkedSpacer.isVisible());
      assertTrue(checkedIndicator.isManaged());
      assertTrue(checkedIndicator.isVisible());
      assertEquals(1.0, checkedIndicator.getOpacity());
      assertTrue(shortcutSpacer.isManaged());
      assertTrue(shortcutSpacer.isVisible());
      assertFalse(shortcutIndicator.isManaged());
      assertFalse(shortcutIndicator.isVisible());
      assertSame(shortcut, withShortcut.getItemContent().getChildren().get(2));

      unchecked.dispose();
      checked.dispose();
      withShortcut.dispose();
    });
  }

  /** Verifies the principal shadcn style mappings and command-dialog defaults. */
  @Test
  void mapsShadcnDimensionsAndDialogDefaults() {
    FxTestSupport.runAndWait(() -> {
      Command command = new Command();
      CommandInput input = new CommandInput(command);
      CommandItem item = new CommandItem(command, "Calendar");
      CommandItem search = new CommandItem(command, "Search Emoji");
      CommandItem calculator = new CommandItem(command, "Calculator");
      CommandEmpty empty = new CommandEmpty(command, "No results found.");
      CommandGroup group = new CommandGroup(command, "Suggestions", item, search, calculator);
      CommandSeparator separator = new CommandSeparator(command);
      CommandList list = new CommandList(command, empty, group, separator);
      command.getChildren().addAll(input, list);
      CommandDialog dialog = new CommandDialog(command);
      StackPane root = new StackPane(dialog);
      new ThemeManager(new Scene(root, 800.0, 600.0), AppTheme.LIGHT);

      dialog.show();
      root.applyCss();
      root.layout();

      Bounds initialDialogBounds = dialog.getDialogContent().getBoundsInParent();

      assertEquals(new Insets(4.0), command.getPadding());
      assertEquals(12.0, command.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.web("#ffffff"), background(command));
      assertEquals(32.0, input.getInputGroup().prefHeight(-1.0));
      assertEquals(Color.TRANSPARENT, background(input.getInputGroup()));
      assertEquals(new Insets(4.0, 4.0, 0.0, 4.0), input.getPadding());
      assertEquals(288.0, list.getMaxHeight());
      assertEquals(new Insets(0.0), list.getPadding());
      assertEquals(ScrollBarPolicy.NEVER, list.getVbarPolicy());
      assertEquals(new Insets(24.0, 0.0, 24.0, 0.0), empty.getPadding());
      assertEquals(Double.MAX_VALUE, empty.getMaxWidth());
      assertEquals(Pos.CENTER, empty.getAlignment());
      assertEquals(new Insets(4.0), group.getPadding());
      assertEquals(-4.0, separator.getTranslateX());
      assertEquals(new Insets(6.0, 8.0, 6.0, 8.0), item.getPadding());
      assertEquals(8.0, item.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(
        item.getWidth() - item.getInsets().getLeft() - item.getInsets().getRight(),
        item.getItemContent().getWidth(),
        0.01
      );
      assertEquals(new Insets(0.0), dialog.getDialogContent().getPadding());
      assertEquals(AccessibleRole.DIALOG, dialog.getAccessibleRole());
      assertEquals("Command Palette", dialog.getAccessibleText());
      assertEquals("Search for a command to run...", dialog.getAccessibleHelp());
      assertFalse(dialog.isShowCloseButton());
      assertEquals(root.getHeight() / 3.0, initialDialogBounds.getMinY(), 1.0);

      command.setSearchText("missing");
      root.applyCss();
      root.layout();

      Bounds filteredDialogBounds = dialog.getDialogContent().getBoundsInParent();
      assertTrue(filteredDialogBounds.getHeight() < initialDialogBounds.getHeight());
      assertEquals(initialDialogBounds.getMinY(), filteredDialogBounds.getMinY(), 0.01);

      dialog.setShowCloseButton(true);
      assertTrue(dialog.getDialogContent().getChildren().contains(dialog.getDialogContent().getCloseButton()));

      input.dispose();
      list.dispose();
      empty.dispose();
      group.dispose();
      separator.dispose();
      item.dispose();
      search.dispose();
      calculator.dispose();
    });
  }

  /**
   * Creates a plain key-pressed event for interaction tests.
   *
   * @param code the key code
   * @return the key event
   */
  private static KeyEvent keyPressed(KeyCode code) {
    return new KeyEvent(KeyEvent.KEY_PRESSED, "", "", code, false, false, false, false);
  }

  /**
   * Returns the first CSS background color for a region.
   *
   * @param region the styled region
   * @return the first background fill
   */
  private static Color background(Region region) {
    return (Color) region.getBackground().getFills().getFirst().getFill();
  }
}
