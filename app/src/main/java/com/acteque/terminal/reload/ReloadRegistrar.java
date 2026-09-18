package com.acteque.terminal.reload;

/** Optional registration point implemented by development-time view tooling. */
public interface ReloadRegistrar {
  void register(Object target);
}
