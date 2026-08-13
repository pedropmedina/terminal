package com.acteque.terminal;

/** Optional registration point implemented by development-time chart tooling. */
public interface ChartReloadRegistrar {

  void register(Object chart);
}
