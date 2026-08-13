package com.acteque.terminal;

import java.util.List;
import java.util.ServiceLoader;

/** Dispatches chart registration to optional classpath-provided tooling. */
final class ChartReloadHooks {

  private static final List<ChartReloadRegistrar> REGISTRARS =
    ServiceLoader.load(ChartReloadRegistrar.class)
      .stream()
      .map(ServiceLoader.Provider::get)
      .toList();

  private ChartReloadHooks() {}

  static void register(Object chart) {
    REGISTRARS.forEach(registrar -> registrar.register(chart));
  }
}
