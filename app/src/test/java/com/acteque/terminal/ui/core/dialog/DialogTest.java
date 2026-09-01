package com.acteque.terminal.ui.core.dialog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.AppTheme;
import com.acteque.terminal.ui.ThemeManager;
import com.acteque.terminal.ui.core.Button;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.Test;

class DialogTest {

  @Test
  void composesTheDialogComponentFamily() {
    FxTestSupport.runAndWait(() -> {
      DialogTitle title = new DialogTitle("Delete account?");
      DialogDescription description = new DialogDescription("This action cannot be undone.");
      DialogHeader header = new DialogHeader(title, description);
      DialogFooter footer = new DialogFooter(new Button("Continue"));
      DialogContent content = new DialogContent(header, footer);
      Dialog dialog = new Dialog(content);

      assertSame(content, dialog.getContent());
      assertEquals(2, dialog.getPortal().getChildren().size());
      assertSame(dialog.getOverlay(), dialog.getPortal().getChildren().getFirst());
      assertTrue(dialog.getStyleClass().contains("core-dialog"));
      assertTrue(content.getStyleClass().contains("core-dialog-content"));
      assertTrue(content.getChildren().contains(content.getCloseButton()));
      assertEquals("Close", content.getCloseButton().getAccessibleText());
    });
  }

  @Test
  void stretchesTheOverlayToEveryPortalEdge() {
    FxTestSupport.runAndWait(() -> {
      Dialog dialog = new Dialog(new DialogContent());
      StackPane root = new StackPane(dialog);
      new Scene(root, 800.0, 600.0);

      dialog.show();
      root.applyCss();
      root.layout();

      assertEquals(0.0, dialog.getOverlay().getBoundsInParent().getMinX());
      assertEquals(0.0, dialog.getOverlay().getBoundsInParent().getMinY());
      assertEquals(dialog.getPortal().getWidth(), dialog.getOverlay().getWidth());
      assertEquals(dialog.getPortal().getHeight(), dialog.getOverlay().getHeight());
    });
  }

  @Test
  void triggerEscapeAndCloseControlsManageOpenState() {
    FxTestSupport.runAndWait(() -> {
      DialogFooter footer = new DialogFooter();
      footer.setShowCloseButton(true);
      DialogContent content = new DialogContent(footer);
      Dialog dialog = new Dialog(content);
      DialogTrigger trigger = new DialogTrigger("Open", dialog);
      new Scene(new StackPane(trigger, dialog));

      trigger.fire();
      assertTrue(dialog.isOpen());
      assertTrue(dialog.isManaged());
      assertTrue(dialog.isVisible());

      dialog.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ESCAPE, false, false, false, false));
      assertFalse(dialog.isOpen());

      dialog.show();
      ((Button) footer.getCloseButton()).fire();
      assertFalse(dialog.isOpen());

      dialog.show();
      content.getCloseButton().fire();
      assertFalse(dialog.isOpen());
      assertFalse(dialog.isManaged());
      assertFalse(dialog.isVisible());
    });
  }

  @Test
  void canHideBothOptionalCloseButtons() {
    FxTestSupport.runAndWait(() -> {
      DialogContent content = new DialogContent();
      DialogFooter footer = new DialogFooter();

      content.setShowCloseButton(false);
      footer.setShowCloseButton(false);

      assertFalse(content.getChildren().contains(content.getCloseButton()));
      assertFalse(footer.getChildren().contains(footer.getCloseButton()));
    });
  }

  @Test
  void blursTheConfiguredBackdropWhileOpenAndRestoresItsEffectWhenClosed() {
    FxTestSupport.runAndWait(() -> {
      StackPane backdrop = new StackPane();
      ColorAdjust existingEffect = new ColorAdjust();
      backdrop.setEffect(existingEffect);
      Dialog dialog = new Dialog();
      dialog.setBackdrop(backdrop);

      dialog.show();

      assertTrue(backdrop.getEffect() instanceof GaussianBlur);
      GaussianBlur blur = (GaussianBlur) backdrop.getEffect();
      assertEquals(4.0, blur.getRadius());
      assertSame(existingEffect, blur.getInput());

      dialog.close();

      assertSame(existingEffect, backdrop.getEffect());
    });
  }

  @Test
  void mapsTheProvidedSpacingSizingAndColorsToJavaFxCss() {
    FxTestSupport.runAndWait(() -> {
      DialogHeader header = new DialogHeader(new DialogTitle("Title"), new DialogDescription("Description"));
      DialogFooter footer = new DialogFooter();
      DialogContent content = new DialogContent(header, footer);
      Dialog dialog = new Dialog(content);
      StackPane root = new StackPane(dialog);
      new ThemeManager(new Scene(root, 800.0, 600.0), AppTheme.LIGHT);

      dialog.show();
      root.applyCss();

      assertEquals(384.0, content.getMaxWidth());
      assertEquals(16.0, content.getSpacing());
      assertEquals(new Insets(16.0), content.getPadding());
      assertEquals(12.0, content.getBackground().getFills().getFirst().getRadii().getTopLeftHorizontalRadius());
      assertEquals(Color.web("#ffffff"), content.getBackground().getFills().getFirst().getFill());
      assertEquals(8.0, header.getSpacing());
      assertEquals(new Insets(0.0, -16.0, -16.0, -16.0), DialogContent.getMargin(footer));
      assertEquals(16.0, ((DialogTitle) header.getChildren().getFirst()).getFont().getSize());
      assertEquals(Color.web("#737373"), ((DialogDescription) header.getChildren().get(1)).getTextFill());
    });
  }
}
