package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.HistoricalMarketDataContract;
import com.acteque.terminal.marketdata.MarketDataClient;
import java.io.IOException;
import java.net.URI;

class TiingoHistoricalContractTest extends HistoricalMarketDataContract {

  @Override
  protected MarketDataClient createClient(Fixture fixture) {
    return new TiingoMarketDataClient("test-token", URI.create("https://example.test"), (uri, headers) -> {
      if (fixture == Fixture.NETWORK_FAILURE) throw new IOException("offline");
      String body = fixture == Fixture.EMPTY ? "[]" : "[" + bar("2024-01-03") + "," + bar("2024-01-02") + "]";
      return new TiingoHttpTransport.Response(200, body);
    });
  }

  private static String bar(String date) {
    return """
    {"date":"%sT00:00:00.000Z","open":10,"high":11,"low":9,"close":10.12345678901234567890,
     "volume":100,"adjOpen":10,"adjHigh":11,"adjLow":9,"adjClose":10.12345678901234567890,"adjVolume":100}
    """.formatted(date);
  }
}
