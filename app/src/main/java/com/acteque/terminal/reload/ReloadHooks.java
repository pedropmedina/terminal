package com.acteque.terminal.reload;

import java.util.List;
import java.util.ServiceLoader;

/** Dispatches reload target registration to optional classpath-provided tooling. */
public final class ReloadHooks {

  private static final List<ReloadRegistrar> REGISTRARS = ServiceLoader.load(ReloadRegistrar.class)
    .stream()
    .map(ServiceLoader.Provider::get)
    .toList();

  private ReloadHooks() {}

  public static void register(Object target) {
    REGISTRARS.forEach(registrar -> registrar.register(target));
  }
}
