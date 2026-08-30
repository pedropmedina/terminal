package com.acteque.terminal.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChartMenuTest {

  @Test
  void usesAccessibleCoreIconButtons() {
    FxTestSupport.runAndWait(() -> {
      ChartMenu menu = new ChartMenu();
      List<String> descriptions = List.of("Symbol or instrument", "Interval", "Chart type");

      assertEquals(3, menu.getChildren().size());
      for (int index = 0; index < menu.getChildren().size(); index++) {
        Button button = assertInstanceOf(Button.class, menu.getChildren().get(index));
        assertEquals(Variant.GHOST, button.getVariant());
        assertEquals(Size.ICON, button.getSize());
        assertEquals(descriptions.get(index), button.getAccessibleText());
      }
    });
  }
}
