package com.acteque.terminal.marketlogos;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class LogoProviderRegistryTest {

  @Test
  void selectsFactoriesAndCreatesIndependentConfiguredInstances() {
    try (var registry = new LogoProviderRegistry(List.of(factory("first"), factory("second")))) {
      var first = (Client) registry.create("first", Map.of("account", "one")::get);
      var another = (Client) registry.create("first", Map.of("account", "two")::get);
      assertNotSame(first, another);
      assertEquals("one", first.account);
      assertEquals("two", another.account);
      assertEquals("second", registry.create("second", key -> null).provider());
      assertThrows(IllegalArgumentException.class, () -> registry.create("unknown", key -> null));
    }
  }

  @Test
  void rejectsDuplicateAndMalformedRegistrations() {
    assertThrows(IllegalArgumentException.class, () ->
      new LogoProviderRegistry(List.of(factory("same"), factory("same")))
    );
    assertThrows(IllegalArgumentException.class, () -> new LogoProviderRegistry(List.of(factory("UPPER"))));
  }

  @Test
  void closesRemainingClientsAfterACloseFailure() {
    var registry = new LogoProviderRegistry(List.of(factory("test")));
    Client first = (Client) registry.create("test", key -> null);
    Client second = (Client) registry.create("test", key -> null);
    second.failClose = true;
    assertEquals(1, assertThrows(IllegalStateException.class, registry::close).getSuppressed().length);
    assertEquals(1, first.closes);
    assertEquals(1, second.closes);
    registry.close();
    assertEquals(1, first.closes);
  }

  @Test
  void rejectsAndClosesAMismatchedClient() {
    Client mismatched = new Client("other", null);
    var factory = new LogoProviderFactory() {
      public String provider() {
        return "test";
      }

      public LogoProvider create(Function<String, String> settings) {
        return mismatched;
      }
    };
    try (var registry = new LogoProviderRegistry(List.of(factory))) {
      assertThrows(IllegalStateException.class, () -> registry.create("test", key -> null));
      assertEquals(1, mismatched.closes);
    }
  }

  private static LogoProviderFactory factory(String id) {
    return new LogoProviderFactory() {
      public String provider() {
        return id;
      }

      public LogoProvider create(Function<String, String> configuration) {
        return new Client(id, configuration.apply("account"));
      }
    };
  }

  private static final class Client implements LogoProvider {

    private final String id;
    private final String account;
    private int closes;
    private boolean failClose;

    private Client(String id, String account) {
      this.id = id;
      this.account = account;
    }

    public String provider() {
      return id;
    }

    public Optional<InstrumentLogo> findLogo(LogoRequest request) {
      return Optional.empty();
    }

    public Optional<byte[]> load(InstrumentLogo logo) {
      return Optional.empty();
    }

    public void close() {
      closes++;
      if (failClose) throw new IllegalStateException("close failed");
    }
  }
}
