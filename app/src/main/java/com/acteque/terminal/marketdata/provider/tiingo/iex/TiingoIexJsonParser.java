package com.acteque.terminal.marketdata.provider.tiingo.iex;

import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.invalidResponse;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.optionalInstant;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.readList;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.required;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.requiredInstant;
import static com.acteque.terminal.marketdata.provider.tiingo.TiingoResponseDecoder.requiredText;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.Ohlcv;
import com.fasterxml.jackson.core.type.TypeReference;

final class TiingoIexJsonParser {

  private static final TypeReference<List<HistoricalPriceResponse>> HISTORICAL_PRICES = new TypeReference<>() {};
  private static final TypeReference<List<LastPriceResponse>> LAST_PRICES = new TypeReference<>() {};

  private TiingoIexJsonParser() {}

  static List<IntradayBar> parsePrices(String symbol, String json) {
    List<HistoricalPriceResponse> responses = readList(json, HISTORICAL_PRICES, "IEX historical prices");
    try {
      List<IntradayBar> bars = new ArrayList<>(responses.size());
      for (HistoricalPriceResponse response : responses) {
        bars.add(response.toIntradayBar(symbol));
      }
      bars.sort(Comparator.comparing(IntradayBar::timestamp));
      return List.copyOf(bars);
    } catch (RuntimeException exception) {
      throw invalidResponse("Invalid Tiingo IEX historical price", exception);
    }
  }

  static List<TiingoIexLastPrice> parseLastPrices(String json) {
    List<LastPriceResponse> responses = readList(json, LAST_PRICES, "IEX last prices");
    try {
      return responses.stream().map(LastPriceResponse::toLastPrice).toList();
    } catch (RuntimeException exception) {
      throw invalidResponse("Invalid Tiingo IEX last price", exception);
    }
  }

  private record HistoricalPriceResponse(
    String date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume
  ) {
    IntradayBar toIntradayBar(String symbol) {
      return new IntradayBar(
        symbol,
        requiredInstant(date, "date"),
        new Ohlcv(
          required(open, "open"),
          required(high, "high"),
          required(low, "low"),
          required(close, "close"),
          required(volume, "volume")
        )
      );
    }
  }

  private record LastPriceResponse(
    String ticker,
    String timestamp,
    String quoteTimestamp,
    String lastSaleTimestamp,
    BigDecimal last,
    BigDecimal lastSize,
    BigDecimal tngoLast,
    BigDecimal prevClose,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal mid,
    BigDecimal volume,
    BigDecimal bidPrice,
    BigDecimal bidSize,
    BigDecimal askPrice,
    BigDecimal askSize
  ) {
    TiingoIexLastPrice toLastPrice() {
      return new TiingoIexLastPrice(
        requiredText(ticker, "ticker"),
        optionalInstant(timestamp, "timestamp"),
        optionalInstant(quoteTimestamp, "quoteTimestamp"),
        optionalInstant(lastSaleTimestamp, "lastSaleTimestamp"),
        Optional.ofNullable(last),
        Optional.ofNullable(lastSize),
        Optional.ofNullable(tngoLast),
        Optional.ofNullable(prevClose),
        Optional.ofNullable(open),
        Optional.ofNullable(high),
        Optional.ofNullable(low),
        Optional.ofNullable(mid),
        Optional.ofNullable(volume),
        Optional.ofNullable(bidPrice),
        Optional.ofNullable(bidSize),
        Optional.ofNullable(askPrice),
        Optional.ofNullable(askSize)
      );
    }
  }
}
