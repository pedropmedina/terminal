package com.acteque.terminal.ui;

import java.util.List;
import java.util.ServiceLoader;

/** Dispatches refreshable-view registration to optional classpath-provided tooling. */
public final class ChartReloadHooks {

  private static final List<ChartReloadRegistrar> REGISTRARS = ServiceLoader.load(ChartReloadRegistrar.class)
    .stream()
    .map(ServiceLoader.Provider::get)
    .toList();

  private ChartReloadHooks() {}

  public static void register(Object view) {
    REGISTRARS.forEach(registrar -> registrar.register(view));
  }
}
