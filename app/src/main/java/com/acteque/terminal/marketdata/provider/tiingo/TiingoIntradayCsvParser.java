package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.IntradayBar;
import com.acteque.terminal.marketdata.MarketDataException;
import com.acteque.terminal.marketdata.Ohlcv;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class TiingoIntradayCsvParser {

  private static final List<String> REQUIRED_COLUMNS = List.of("date", "open", "high", "low", "close", "volume");

  private TiingoIntradayCsvParser() {}

  static List<IntradayBar> parse(String symbol, String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }

    String[] lines = csv.strip().split("\\R");
    Map<String, Integer> columns = columns(lines[0]);
    List<IntradayBar> bars = new ArrayList<>();

    for (int lineNumber = 1; lineNumber < lines.length; lineNumber++) {
      if (lines[lineNumber].isBlank()) {
        continue;
      }

      String[] values = lines[lineNumber].split(",", -1);
      try {
        bars.add(toIntradayBar(symbol, values, columns));
      } catch (RuntimeException exception) {
        throw invalidResponse("Invalid Tiingo intraday CSV row " + (lineNumber + 1), exception);
      }
    }

    bars.sort(java.util.Comparator.comparing(IntradayBar::timestamp));
    return List.copyOf(bars);
  }

  private static Map<String, Integer> columns(String header) {
    String[] names = header.split(",", -1);
    Map<String, Integer> columns = new HashMap<>();
    for (int index = 0; index < names.length; index++) {
      columns.put(names[index].strip(), index);
    }

    List<String> missing = REQUIRED_COLUMNS.stream().filter(name -> !columns.containsKey(name)).toList();
    if (!missing.isEmpty()) {
      throw invalidResponse("Tiingo intraday CSV response is missing columns: " + missing, null);
    }
    return columns;
  }

  private static IntradayBar toIntradayBar(String symbol, String[] values, Map<String, Integer> columns) {
    return new IntradayBar(
      symbol,
      timestamp(values, columns),
      new Ohlcv(
        decimal(values, columns, "open"),
        decimal(values, columns, "high"),
        decimal(values, columns, "low"),
        decimal(values, columns, "close"),
        decimal(values, columns, "volume")
      )
    );
  }

  private static Instant timestamp(String[] values, Map<String, Integer> columns) {
    String value = value(values, columns, "date");
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid timestamp: " + value, exception);
    }
  }

  private static BigDecimal decimal(String[] values, Map<String, Integer> columns, String column) {
    String value = value(values, columns, column);
    if (value.isBlank()) {
      throw new IllegalArgumentException(column + " is blank");
    }
    return new BigDecimal(value);
  }

  private static String value(String[] values, Map<String, Integer> columns, String column) {
    int index = columns.get(column);
    if (index >= values.length) {
      throw new IllegalArgumentException(
        "Expected column " + column + " at index " + index + " in " + Arrays.toString(values)
      );
    }
    return values[index].strip();
  }

  private static MarketDataException invalidResponse(String message, Throwable cause) {
    return new MarketDataException(MarketDataException.Code.INVALID_RESPONSE, message, cause);
  }
}
