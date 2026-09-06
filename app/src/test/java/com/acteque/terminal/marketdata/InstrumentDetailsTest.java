package com.acteque.terminal.marketdata;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;
import org.junit.jupiter.api.Test;

class InstrumentDetailsTest {

  @Test
  void preservesProviderSymbolAndNormalizesDescriptiveFields() {
    InstrumentDetails details = new InstrumentDetails(
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
    InstrumentDetails details = new InstrumentDetails("ABC", Optional.empty(), Optional.empty(), Optional.empty());

    assertEquals(Optional.empty(), details.name());
    assertEquals(Optional.empty(), details.exchange());
    assertEquals(Optional.empty(), details.description());
  }

  @Test
  void rejectsMissingSymbolAndNullOptionalContainers() {
    assertThrows(IllegalArgumentException.class, () ->
      new InstrumentDetails(" ", Optional.empty(), Optional.empty(), Optional.empty())
    );
    assertThrows(NullPointerException.class, () ->
      new InstrumentDetails(null, Optional.empty(), Optional.empty(), Optional.empty())
    );
    assertThrows(NullPointerException.class, () ->
      new InstrumentDetails("ABC", null, Optional.empty(), Optional.empty())
    );
    assertThrows(NullPointerException.class, () ->
      new InstrumentDetails("ABC", Optional.empty(), null, Optional.empty())
    );
    assertThrows(NullPointerException.class, () ->
      new InstrumentDetails("ABC", Optional.empty(), Optional.empty(), null)
    );
  }
}
