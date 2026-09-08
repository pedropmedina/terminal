package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.InstrumentLogo;
import java.io.ByteArrayInputStream;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javafx.scene.image.Image;

/** Loads optional chart branding without delaying prices or retaining an image cache. */
public final class ChartLogoLoader {

  private static final System.Logger LOGGER = System.getLogger(ChartLogoLoader.class.getName());
  private final Function<InstrumentLogo, CompletionStage<Optional<byte[]>>> download;
  private final Executor uiExecutor;
  private long generation;

  public ChartLogoLoader(Function<InstrumentLogo, CompletionStage<Optional<byte[]>>> download, Executor uiExecutor) {
    this.download = Objects.requireNonNull(download, "download");
    this.uiExecutor = Objects.requireNonNull(uiExecutor, "uiExecutor");
  }

  /** Called on the UI thread, including when a new price load starts or the chart closes. */
  public void cancel() {
    ++generation;
  }

  /** Called on the UI thread after the chart has switched instruments and cleared its old logo. */
  public void load(Optional<InstrumentLogo> logo, BiConsumer<InstrumentLogo, Image> onLoaded) {
    Objects.requireNonNull(logo, "logo");
    Objects.requireNonNull(onLoaded, "onLoaded");
    long requestedGeneration = ++generation;
    logo.ifPresent(reference ->
      download
        .apply(reference)
        .thenApply(bytes ->
          bytes.map(data -> {
            // Bound decoded dimensions too; these are decode limits, not the CSS display size.
            Image image = new Image(new ByteArrayInputStream(data), 64, 64, true, true);
            if (image.isError()) {
              throw new IllegalArgumentException("Logo image could not be decoded", image.getException());
            }
            return image;
          })
        )
        .whenComplete((image, failure) ->
          uiExecutor.execute(() -> {
            // Check after dispatch: a newer selection may have happened while this task was queued.
            if (requestedGeneration != generation) {
              return;
            }
            if (failure != null) {
              LOGGER.log(System.Logger.Level.WARNING, "Could not load instrument logo; keeping fallback icon", failure);
            } else {
              image.ifPresent(value -> onLoaded.accept(reference, value));
            }
          })
        )
    );
  }
}
