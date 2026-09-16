package com.acteque.terminal.marketdata.provider.tiingo;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

class TiingoProviderFactoryTest {

  @Test
  void validatesConfigurationAndExposesCapabilitiesWithoutNetworkAccess() {
    var factory = new TiingoProviderFactory();
    assertThrows(IllegalArgumentException.class, () -> factory.create(key -> null));
    assertThrows(IllegalArgumentException.class, () -> factory.create(key -> " "));
    try (var client = factory.create(Map.of("TIINGO_API_KEY", "test-token")::get)) {
      assertEquals(factory.provider(), client.provider());
      assertTrue(client.historicalBars().isPresent());
      assertTrue(client.discovery().isPresent());
      assertTrue(client.catalog().isPresent());
      assertSame(client.catalog().orElseThrow(), client.catalog().orElseThrow());
      client.close();
    }
  }
}
