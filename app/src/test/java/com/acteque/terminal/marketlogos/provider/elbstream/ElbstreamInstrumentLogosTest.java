package com.acteque.terminal.marketlogos.provider.elbstream;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoRequest;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ElbstreamInstrumentLogosTest {

  private static final byte[] PNG = { (byte) 137, 80, 78, 71, 13, 10, 26, 10, 0 };

  private static LogoRequest details(String symbol) {
    return new LogoRequest(symbol, Optional.of("not-used"));
  }

  private static InstrumentLogo logo(ElbstreamInstrumentLogos logos) {
    return logos.findLogo(details("IBM")).orElseThrow();
  }

  @Test
  void resolvesWithoutNetworkAndEncodesSymbolsWithoutGuessingOrStrippingPunctuation() {
    ElbstreamInstrumentLogos logos = new ElbstreamInstrumentLogos(uri -> {
      fail("reference lookup must not use network");
      return null;
    });
    assertEquals(
      "https://api.elbstream.com/logos/symbol/BRK.B?format=png&size=64",
      logos.findLogo(details("BRK.B")).orElseThrow().imageUri().toString()
    );
    assertEquals(
      "https://api.elbstream.com/logos/symbol/a%2Fb%20%3F%23%25%3A%C3%A9?format=png&size=64",
      logos.findLogo(details("a/b ?#%:é")).orElseThrow().imageUri().toString()
    );
    assertEquals("Logos by Elbstream", logo(logos).attributionText());
    assertEquals(URI.create("https://elbstream.com"), logo(logos).attributionUri());
    assertTrue(logos.findLogo(details("..")).isEmpty());
  }

  @Test
  void loadsPngWithoutCachingAndReturnsEmptyFor404() {
    AtomicInteger requests = new AtomicInteger();
    ElbstreamInstrumentLogos logos = new ElbstreamInstrumentLogos(uri -> {
      assertEquals(URI.create("https://api.elbstream.com/logos/symbol/IBM?format=png&size=64"), uri);
      requests.incrementAndGet();
      return new ElbstreamHttpTransport.Response(200, "IMAGE/PNG; charset=binary", PNG.clone());
    });
    assertArrayEquals(PNG, logos.load(logo(logos)).orElseThrow());
    assertArrayEquals(PNG, logos.load(logo(logos)).orElseThrow());
    assertEquals(2, requests.get());
    ElbstreamInstrumentLogos missing = new ElbstreamInstrumentLogos(uri ->
      new ElbstreamHttpTransport.Response(404, "text/html", new byte[0])
    );
    assertTrue(missing.load(logo(missing)).isEmpty());
  }

  @Test
  void normalizesStatusesAndRejectsNonPngOrOversizedBodies() {
    for (int status : new int[] { 301, 401, 403, 429, 500, 204 }) {
      ElbstreamInstrumentLogos logos = new ElbstreamInstrumentLogos(uri ->
        new ElbstreamHttpTransport.Response(status, "image/png", PNG)
      );
      LogoException.Code expected = switch (status) {
        case 401, 403 -> LogoException.Code.AUTHENTICATION;
        case 429 -> LogoException.Code.RATE_LIMITED;
        default -> LogoException.Code.PROVIDER_ERROR;
      };
      assertEquals(expected, assertThrows(LogoException.class, () -> logos.load(logo(logos))).code());
    }
    for (ElbstreamHttpTransport.Response response : new ElbstreamHttpTransport.Response[] {
      new ElbstreamHttpTransport.Response(200, "text/html", PNG),
      new ElbstreamHttpTransport.Response(200, null, PNG),
      new ElbstreamHttpTransport.Response(200, "image/png", new byte[0]),
      new ElbstreamHttpTransport.Response(200, "image/png", new byte[8]),
      new ElbstreamHttpTransport.Response(200, "image/png", new byte[ElbstreamInstrumentLogos.MAX_BYTES + 1]),
    }) {
      ElbstreamInstrumentLogos logos = new ElbstreamInstrumentLogos(uri -> response);
      assertEquals(
        LogoException.Code.INVALID_RESPONSE,
        assertThrows(LogoException.class, () -> logos.load(logo(logos))).code()
      );
    }
  }

  @Test
  void normalizesNetworkAndRestoresInterruptFlag() {
    ElbstreamInstrumentLogos broken = new ElbstreamInstrumentLogos(uri -> {
      throw new IOException("offline");
    });
    assertEquals(LogoException.Code.NETWORK, assertThrows(LogoException.class, () -> broken.load(logo(broken))).code());
    ElbstreamInstrumentLogos interrupted = new ElbstreamInstrumentLogos(uri -> {
      throw new InterruptedException("cancelled");
    });
    try {
      assertEquals(
        LogoException.Code.NETWORK,
        assertThrows(LogoException.class, () -> interrupted.load(logo(interrupted))).code()
      );
      assertTrue(Thread.currentThread().isInterrupted());
    } finally {
      Thread.interrupted();
    }
  }

  @Test
  void rejectsArbitraryReferencesBeforeTransport() {
    ElbstreamInstrumentLogos logos = new ElbstreamInstrumentLogos(uri -> {
      fail("unsafe reference reached transport");
      return null;
    });
    for (String uri : new String[] {
      "https://example.com/logos/symbol/IBM?format=png&size=64",
      "https://api.elbstream.com.evil.com/logos/symbol/IBM?format=png&size=64",
      "https://api.elbstream.com/other/IBM?format=png&size=64",
      "https://api.elbstream.com/logos/symbol/IBM?format=svg&size=64",
      "https://api.elbstream.com/logos/symbol/IBM?format=png&size=64&url=https://example.com",
      "https://api.elbstream.com/logos/symbol/../IBM?format=png&size=64",
      "https://api.elbstream.com/logos/symbol/%2E%2E?format=png&size=64",
      "https://api.elbstream.com/logos/symbol/?format=png&size=64",
    }) {
      InstrumentLogo reference = new InstrumentLogo(URI.create(uri), "Logos", URI.create("https://elbstream.com"));
      assertThrows(IllegalArgumentException.class, () -> logos.load(reference), uri);
    }
  }
}
