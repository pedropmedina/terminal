package com.acteque.terminal.chart;

import com.acteque.terminal.marketdata.InstrumentLogo;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

/** Asynchronous data port used by the chart's logo-loading interactor. */
@FunctionalInterface
public interface ChartLogoSource {
  CompletionStage<Optional<byte[]>> load(InstrumentLogo logo);

  default void cancel() {}
}
