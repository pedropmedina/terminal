package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.*;

import java.net.URI;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstrumentLogoTest {

  private static final URI IMAGE = URI.create("https://images.example.com/logo.png");
  private static final URI ATTRIBUTION = URI.create("https://example.com");

  @Test
  void validatesBothPublicReferencesAndAttribution() {
    InstrumentLogo logo = new InstrumentLogo(IMAGE, " Logos ", ATTRIBUTION);
    assertEquals("Logos", logo.attributionText());
    for (String invalid : new String[] {
      "http://example.com/logo",
      "/logo",
      "file:///tmp/logo",
      "https://localhost/logo",
      "https://127.0.0.1/logo",
      "https://[::1]/logo",
      "https://10.0.0.1/logo",
      "https://service.local/logo",
      "https://example.com:8080/logo",
      "https://user@example.com/logo",
      "https://example.com/logo#fragment",
      "https://2130706433/logo",
      "https://internal/logo",
    }) {
      URI uri = URI.create(invalid);
      assertThrows(IllegalArgumentException.class, () -> new InstrumentLogo(uri, "Logos", ATTRIBUTION), invalid);
      assertThrows(IllegalArgumentException.class, () -> new InstrumentLogo(IMAGE, "Logos", uri), invalid);
    }
    assertThrows(IllegalArgumentException.class, () -> new InstrumentLogo(IMAGE, " ", ATTRIBUTION));
    assertThrows(NullPointerException.class, () -> new InstrumentLogo(null, "Logos", ATTRIBUTION));
    assertThrows(NullPointerException.class, () -> new InstrumentLogo(IMAGE, null, ATTRIBUTION));
    assertThrows(NullPointerException.class, () -> new InstrumentLogo(IMAGE, "Logos", null));
  }

  @Test
  void existingDetailsConstructorDefaultsToNoLogoAndHelperPreservesMetadata() {
    InstrumentDetails original = new InstrumentDetails(
      "Ab.C",
      Optional.of("Name"),
      Optional.of("Exchange"),
      Optional.of("Description")
    );
    InstrumentLogo logo = new InstrumentLogo(IMAGE, "Logos", ATTRIBUTION);
    assertTrue(original.logo().isEmpty());
    InstrumentDetails enriched = original.withLogo(Optional.of(logo));
    assertEquals(original, enriched.withLogo(Optional.empty()));
    assertSame(logo, enriched.logo().orElseThrow());
    assertThrows(NullPointerException.class, () -> original.withLogo(null));
  }
}
