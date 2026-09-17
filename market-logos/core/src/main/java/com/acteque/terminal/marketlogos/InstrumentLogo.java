package com.acteque.terminal.marketlogos;

import java.net.URI;
import java.util.Locale;
import java.util.Objects;

/** A remote image reference and the attribution that must accompany its display. */
public record InstrumentLogo(URI imageUri, String attributionText, URI attributionUri) {
  public InstrumentLogo {
    validatePublicHttps(imageUri, "imageUri");
    validatePublicHttps(attributionUri, "attributionUri");
    attributionText = Objects.requireNonNull(attributionText, "attributionText cannot be null").strip();
    if (attributionText.isEmpty()) {
      throw new IllegalArgumentException("attributionText must not be blank");
    }
  }

  // Syntactic validation only: no DNS/network access. Loaders must enforce their own endpoint allowlist.
  private static void validatePublicHttps(URI uri, String field) {
    Objects.requireNonNull(uri, field + " cannot be null");
    String host = uri.getHost();
    if (
      !"https".equalsIgnoreCase(uri.getScheme()) ||
      host == null ||
      uri.getRawUserInfo() != null ||
      uri.getRawFragment() != null ||
      (uri.getPort() != -1 && uri.getPort() != 443)
    ) {
      throw new IllegalArgumentException(field + " must be a public HTTPS URI without credentials or fragment");
    }
    host = host.toLowerCase(Locale.ROOT);
    if (
      !host.matches("[a-z0-9-]+(?:\\.[a-z0-9-]+)*\\.[a-z][a-z0-9-]*") ||
      host.endsWith(".localhost") ||
      host.endsWith(".local") ||
      host.endsWith(".internal") ||
      host.endsWith(".lan") ||
      host.endsWith(".home") ||
      host.endsWith(".test") ||
      host.endsWith(".invalid") ||
      host.endsWith(".example")
    ) {
      throw new IllegalArgumentException(field + " must use a public DNS hostname, not a local host or IP literal");
    }
  }
}
