package com.acteque.terminal.chart.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import com.acteque.terminal.test.FxTestSupport;
import com.acteque.terminal.ui.core.Button;
import com.acteque.terminal.ui.core.Button.Size;
import com.acteque.terminal.ui.core.Button.Variant;
import com.acteque.terminal.ui.core.buttongroup.ButtonGroup;
import java.util.List;
import javafx.scene.layout.Region;
import org.junit.jupiter.api.Test;

class ChartMenuViewBuilderTest {

  @Test
  void buildsAccessibleCoreIconButtonsFromTheModel() {
    FxTestSupport.runAndWait(() -> {
      ChartMenuModel model = new ChartMenuModel();
      ChartMenuInteractor interactor = new ChartMenuInteractor(model);
      interactor.initialize();
      Region menu = new ChartMenuViewBuilder(model, interactor::request).build();
      List<String> descriptions = List.of("Symbol or instrument", "Interval", "Chart type");
      ButtonGroup group = assertInstanceOf(ButtonGroup.class, menu.getChildrenUnmodifiable().getFirst());

      assertEquals(1, menu.getChildrenUnmodifiable().size());
      assertEquals(3, group.getChildren().size());
      for (int index = 0; index < group.getChildren().size(); index++) {
        Button button = assertInstanceOf(Button.class, group.getChildren().get(index));
        assertEquals(Variant.GHOST, button.getVariant());
        assertEquals(Size.ICON, button.getSize());
        assertEquals(descriptions.get(index), button.getAccessibleText());
      }
    });
  }
}
