package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstrumentTest {

  @Test
  void preservesProviderSymbolAndNormalizesDescriptiveFields() {
    Instrument details = new Instrument(
      " provider:Abc ",
      Optional.of(" Example Inc. "),
      Optional.of("   "),
      Optional.empty()
    );

    assertEquals("provider:Abc", details.symbol());
    assertEquals(Optional.of("Example Inc."), details.name());
    assertEquals(Optional.empty(), details.exchange());
    assertEquals(Optional.empty(), details.description());
  }

  @Test
  void allowsUnavailableDescriptiveFields() {
    Instrument details = new Instrument("ABC", Optional.empty(), Optional.empty(), Optional.empty());

    assertEquals(Optional.empty(), details.name());
    assertEquals(Optional.empty(), details.exchange());
    assertEquals(Optional.empty(), details.description());
  }

  @Test
  void rejectsMissingSymbolAndNullOptionalContainers() {
    assertThrows(IllegalArgumentException.class, () ->
      new Instrument(" ", Optional.empty(), Optional.empty(), Optional.empty())
    );
    assertThrows(NullPointerException.class, () ->
      new Instrument(null, Optional.empty(), Optional.empty(), Optional.empty())
    );
    assertThrows(NullPointerException.class, () -> new Instrument("ABC", null, Optional.empty(), Optional.empty()));
    assertThrows(NullPointerException.class, () -> new Instrument("ABC", Optional.empty(), null, Optional.empty()));
    assertThrows(NullPointerException.class, () -> new Instrument("ABC", Optional.empty(), Optional.empty(), null));
  }
}
