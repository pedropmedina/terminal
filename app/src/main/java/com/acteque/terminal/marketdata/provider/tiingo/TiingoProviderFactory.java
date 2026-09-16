package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.MarketDataClient;
import com.acteque.terminal.marketdata.MarketDataProviderFactory;
import java.util.function.Function;

/** Tiingo-specific configuration and construction; environment access stays with the application. */
public final class TiingoProviderFactory implements MarketDataProviderFactory {

  @Override
  public String provider() {
    return "tiingo";
  }

  @Override
  public MarketDataClient create(Function<String, String> configuration) {
    String apiKey = configuration.apply("TIINGO_API_KEY");
    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalArgumentException("TIINGO_API_KEY must be configured before using Tiingo");
    }
    return new TiingoMarketDataClient(apiKey);
  }
}
