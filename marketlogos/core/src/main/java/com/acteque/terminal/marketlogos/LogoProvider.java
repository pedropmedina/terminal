package com.acteque.terminal.marketlogos;

/** Shared logo provider, owned by the application rather than individual sessions. */
public interface LogoProvider extends InstrumentLogos, AutoCloseable {
  String provider();

  /** Close after all consumer sessions. Must be idempotent. */
  @Override
  default void close() {}
}
