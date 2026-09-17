package com.acteque.terminal.marketlogos.elbstream;

import com.acteque.terminal.marketlogos.LogoProvider;
import com.acteque.terminal.marketlogos.LogoProviderFactory;
import java.util.Objects;
import java.util.function.Function;

public final class ElbstreamProviderFactory implements LogoProviderFactory {

  @Override
  public String provider() {
    return "elbstream";
  }

  @Override
  public LogoProvider create(Function<String, String> configuration) {
    Objects.requireNonNull(configuration, "configuration");
    return ElbstreamInstrumentLogos.create();
  }
}
