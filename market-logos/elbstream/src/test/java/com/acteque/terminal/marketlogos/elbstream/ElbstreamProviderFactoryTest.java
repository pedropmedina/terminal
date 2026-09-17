package com.acteque.terminal.marketlogos.elbstream;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketlogos.LogoRequest;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ElbstreamProviderFactoryTest {

  @Test
  void createsIndependentProvidersWithoutNetworkRequests() {
    var factory = new ElbstreamProviderFactory();
    try (var first = factory.create(key -> null); var second = factory.create(key -> null)) {
      assertNotSame(first, second);
      assertEquals(factory.provider(), first.provider());
      assertTrue(first.findLogo(new LogoRequest("IBM", Optional.empty())).isPresent());
      first.close();
      first.close();
      assertTrue(second.findLogo(new LogoRequest("AAPL", Optional.empty())).isPresent());
    }
  }

  @Test
  void closesOwnedTransportButDoesNotCloseBorrowedHttpClient() {
    int[] closes = { 0 };
    var provider = new ElbstreamInstrumentLogos(
      new ElbstreamHttpTransport() {
        public Response get(java.net.URI uri) {
          throw new AssertionError("No network expected");
        }

        public void close() {
          closes[0]++;
        }
      }
    );
    provider.close();
    provider.close();
    assertEquals(1, closes[0]);
    try (var client = HttpClient.newHttpClient()) {
      new ElbstreamInstrumentLogos(ElbstreamHttpTransport.using(client, Duration.ofSeconds(1))).close();
      assertFalse(client.isTerminated());
    }
  }
}
