package com.acteque.terminal.chart.statusline;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.acteque.terminal.chart.ChartInterval;
import com.acteque.terminal.chart.PricePoint;
import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.test.FxTestSupport;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.LocalDate;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.junit.jupiter.api.Test;

class ChartStatusLineInteractorTest {

  private static final InstrumentLogo LOGO = new InstrumentLogo(
    URI.create("https://images.example.com/ACME.png"),
    "Logos by Example",
    URI.create("https://example.com")
  );

  @Test
  void initializesAndUpdatesStatusState() {
    ChartStatusLineModel model = new ChartStatusLineModel();
    ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
    PricePoint point = new PricePoint(LocalDate.of(2026, 8, 24), 1, 2, 0.5, 1.5, 100);

    interactor.initialize("ACME", ChartInterval.DAILY);
    interactor.setPricePoint(point);
    interactor.setInterval(ChartInterval.FIVE_MINUTES);

    assertEquals("ACME", model.getInstrumentName());
    assertSame(point, model.getPricePoint());
    assertEquals(ChartInterval.FIVE_MINUTES, model.getInterval());

    interactor.clearPricePoint();
    assertNull(model.getPricePoint());
  }

  @Test
  void changingInstrumentClearsThePreviousLogo() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
      WritableImage image = new WritableImage(64, 64);
      interactor.initialize("ACME", ChartInterval.DAILY);
      interactor.setInstrumentLogo(LOGO, image);
      assertSame(image, model.getLogoState().image());

      interactor.setInstrumentName("Widget Industries");

      assertEquals("Widget Industries", model.getInstrumentName());
      assertNull(model.getLogoState());
    });
  }

  @Test
  void rejectsAnInvalidLogoImage() {
    FxTestSupport.runAndWait(() -> {
      ChartStatusLineModel model = new ChartStatusLineModel();
      ChartStatusLineInteractor interactor = new ChartStatusLineInteractor(model);
      interactor.initialize("ACME", ChartInterval.DAILY);
      interactor.setInstrumentLogo(LOGO, new WritableImage(64, 64));
      Image invalidImage = new Image(new ByteArrayInputStream(new byte[0]));

      interactor.setInstrumentLogo(LOGO, invalidImage);

      assertNull(model.getLogoState());
    });
  }
}
