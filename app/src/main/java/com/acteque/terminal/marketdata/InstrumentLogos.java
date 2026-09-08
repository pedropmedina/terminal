package com.acteque.terminal.marketdata;

import java.util.Objects;
import java.util.Optional;

/** Optional logo references and uncached image loading, independent of price-history loading. */
public interface InstrumentLogos {
  InstrumentLogos NONE = new InstrumentLogos() {
    @Override
    public Optional<InstrumentLogo> findLogo(InstrumentDetails details) {
      Objects.requireNonNull(details, "details");
      return Optional.empty();
    }

    @Override
    public Optional<byte[]> load(InstrumentLogo logo) {
      Objects.requireNonNull(logo, "logo");
      return Optional.empty();
    }
  };

  /** Resolves a best-effort reference only; must not perform network access. */
  Optional<InstrumentLogo> findLogo(InstrumentDetails details);

  /**
   * Loads image bytes without caching. A missing image (HTTP 404) is empty;
   * transport, status, and invalid-response failures throw {@link MarketDataException}.
   * Implementations must reject references outside their supported endpoints.
   */
  Optional<byte[]> load(InstrumentLogo logo);
}
