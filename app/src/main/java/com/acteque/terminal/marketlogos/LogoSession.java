package com.acteque.terminal.marketlogos;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/** A single consumer's logo work. Sessions must not be shared between charts. */
@FunctionalInterface
public interface LogoSession extends AutoCloseable {
  LogoSession NONE = logo -> {
    Objects.requireNonNull(logo, "logo");
    return CompletableFuture.completedFuture(Optional.empty());
  };

  /** Resolves a reference locally, without network access. */
  default Optional<InstrumentLogo> findLogo(LogoRequest request) {
    Objects.requireNonNull(request, "request");
    return Optional.empty();
  }

  CompletionStage<Optional<byte[]>> load(InstrumentLogo logo);

  default void cancel() {}

  /** Releases session work; does not close a shared provider. */
  @Override
  default void close() {
    cancel();
  }
}
