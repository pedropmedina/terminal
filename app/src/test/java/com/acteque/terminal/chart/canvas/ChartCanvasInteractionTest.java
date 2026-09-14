package com.acteque.terminal.chart.canvas;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.test.FxTestSupport;
import java.time.LocalDate;
import java.util.List;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import org.junit.jupiter.api.Test;

class ChartCanvasInteractionTest {

  @Test
  void ignoresMouseInteractionWhenTheSelectedInstrumentHasNoPriceHistory() {
    FxTestSupport.runAndWait(() -> {
      ChartCanvasModel model = new ChartCanvasModel();
      ChartCanvasInteractor interactor = new ChartCanvasInteractor(model);
      interactor.initialize(
        List.of(new PricePoint(LocalDate.of(2026, 8, 24), 100, 101, 99, 100, 1_000)),
        ChartInterval.DAILY
      );
      javafx.scene.canvas.Canvas canvas = new ChartCanvasViewBuilder(model, interactor).build();
      canvas.setWidth(800);
      canvas.setHeight(500);
      interactor.setInstrumentPricePoints(List.of());

      assertDoesNotThrow(() -> canvas.getOnMouseMoved().handle(mouseEvent(MouseEvent.MOUSE_MOVED)));
      assertDoesNotThrow(() -> canvas.getOnMousePressed().handle(mouseEvent(MouseEvent.MOUSE_PRESSED)));
      assertDoesNotThrow(() -> canvas.getOnMouseDragged().handle(mouseEvent(MouseEvent.MOUSE_DRAGGED)));
    });
  }

  private static MouseEvent mouseEvent(javafx.event.EventType<MouseEvent> eventType) {
    return new MouseEvent(
      eventType,
      100,
      100,
      100,
      100,
      MouseButton.PRIMARY,
      1,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      false,
      null
    );
  }
}
