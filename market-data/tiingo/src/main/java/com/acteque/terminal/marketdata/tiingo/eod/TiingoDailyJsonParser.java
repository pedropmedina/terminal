package com.acteque.terminal.marketdata.tiingo.eod;

import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.invalidResponse;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.optionalDate;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.optionalText;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.readList;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.readObject;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.required;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.requiredDate;
import static com.acteque.terminal.marketdata.tiingo.TiingoResponseDecoder.requiredText;

import com.acteque.terminal.marketdata.CalendarData;
import com.acteque.terminal.marketdata.Ohlcv;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

final class TiingoDailyJsonParser {

  private static final TypeReference<List<DailyPriceResponse>> DAILY_PRICES = new TypeReference<>() {};

  private TiingoDailyJsonParser() {}

  static List<CalendarData> parseBars(String symbol, String json) {
    List<DailyPriceResponse> responses = readList(json, DAILY_PRICES, "daily prices");
    try {
      List<CalendarData> bars = new ArrayList<>(responses.size());
      for (DailyPriceResponse response : responses) {
        bars.add(response.toCalendarData(symbol));
      }
      bars.sort(Comparator.comparing(CalendarData::date));
      return List.copyOf(bars);
    } catch (RuntimeException exception) {
      throw invalidResponse("Invalid Tiingo daily price", exception);
    }
  }

  static TiingoTickerMetadata parseMetadata(String json) {
    MetadataResponse response = readObject(json, MetadataResponse.class, "ticker metadata");
    try {
      return response.toMetadata();
    } catch (RuntimeException exception) {
      throw invalidResponse("Invalid Tiingo ticker metadata", exception);
    }
  }

  private record DailyPriceResponse(
    String date,
    BigDecimal open,
    BigDecimal high,
    BigDecimal low,
    BigDecimal close,
    BigDecimal volume,
    BigDecimal adjOpen,
    BigDecimal adjHigh,
    BigDecimal adjLow,
    BigDecimal adjClose,
    BigDecimal adjVolume,
    BigDecimal divCash,
    BigDecimal splitFactor
  ) {
    CalendarData toCalendarData(String symbol) {
      Ohlcv prices = new Ohlcv(
        required(open, "open"),
        required(high, "high"),
        required(low, "low"),
        required(close, "close"),
        required(volume, "volume")
      );
      Ohlcv adjustedPrices = new Ohlcv(
        required(adjOpen, "adjOpen"),
        required(adjHigh, "adjHigh"),
        required(adjLow, "adjLow"),
        required(adjClose, "adjClose"),
        required(adjVolume, "adjVolume")
      );
      return new CalendarData(
        symbol,
        requiredDate(date, "date"),
        prices,
        Optional.of(adjustedPrices),
        Optional.ofNullable(divCash),
        Optional.ofNullable(splitFactor)
      );
    }
  }

  private record MetadataResponse(
    String ticker,
    String name,
    String exchangeCode,
    String description,
    String startDate,
    String endDate
  ) {
    TiingoTickerMetadata toMetadata() {
      return new TiingoTickerMetadata(
        requiredText(ticker, "ticker"),
        requiredText(name, "name"),
        requiredText(exchangeCode, "exchangeCode"),
        optionalText(description),
        optionalDate(startDate, "startDate"),
        optionalDate(endDate, "endDate")
      );
    }
  }
}
