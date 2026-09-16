package com.acteque.terminal.marketlogos;

import java.util.Objects;
import java.util.Optional;

/** Pluggable instrument logo provider. Implementations must support concurrent calls from independent sessions. */
public interface InstrumentLogos {
  InstrumentLogos NONE = new InstrumentLogos() {
    @Override
    public Optional<InstrumentLogo> findLogo(LogoRequest request) {
      Objects.requireNonNull(request, "request");
      return Optional.empty();
    }

    @Override
    public Optional<byte[]> load(InstrumentLogo logo) {
      Objects.requireNonNull(logo, "logo");
      return Optional.empty();
    }
  };

  /** Resolves a best-effort reference only; must not perform network access. */
  Optional<InstrumentLogo> findLogo(LogoRequest request);

  /**
   * Loads image bytes without caching. A missing image (HTTP 404) is empty;
   * transport, status, and invalid-response failures throw {@link LogoException}.
   * Implementations must reject references outside their supported endpoints.
   */
  Optional<byte[]> load(InstrumentLogo logo);
}
