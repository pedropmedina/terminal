package com.acteque.terminal.marketdata.provider.tiingo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.acteque.terminal.marketdata.DailyBar;
import com.acteque.terminal.marketdata.MarketDataException;
import com.acteque.terminal.marketdata.Ohlcv;

final class TiingoCsvParser {

  private static final List<String> REQUIRED_COLUMNS = List.of(
    "date",
    "open",
    "high",
    "low",
    "close",
    "volume",
    "adjOpen",
    "adjHigh",
    "adjLow",
    "adjClose",
    "adjVolume",
    "divCash",
    "splitFactor"
  );

  private TiingoCsvParser() {}

  static List<DailyBar> parse(String symbol, String csv) {
    if (csv == null || csv.isBlank()) {
      return List.of();
    }

    String[] lines = csv.strip().split("\\R");
    Map<String, Integer> columns = columns(lines[0]);
    List<DailyBar> bars = new ArrayList<>();

    for (int lineNumber = 1; lineNumber < lines.length; lineNumber++) {
      if (lines[lineNumber].isBlank()) {
        continue;
      }

      String[] values = lines[lineNumber].split(",", -1);
      try {
        bars.add(toDailyBar(symbol, values, columns));
      } catch (RuntimeException exception) {
        throw invalidResponse("Invalid Tiingo CSV row " + (lineNumber + 1), exception);
      }
    }

    bars.sort(java.util.Comparator.comparing(DailyBar::date));
    return List.copyOf(bars);
  }

  private static Map<String, Integer> columns(String header) {
    String[] names = header.split(",", -1);
    Map<String, Integer> columns = new HashMap<>();
    for (int index = 0; index < names.length; index++) {
      columns.put(names[index].strip(), index);
    }

    List<String> missing = REQUIRED_COLUMNS.stream()
      .filter(name -> !columns.containsKey(name))
      .toList();
    if (!missing.isEmpty()) {
      throw invalidResponse("Tiingo CSV response is missing columns: " + missing, null);
    }
    return columns;
  }

  private static DailyBar toDailyBar(String symbol, String[] values, Map<String, Integer> columns) {
    Ohlcv prices = new Ohlcv(
      decimal(values, columns, "open"),
      decimal(values, columns, "high"),
      decimal(values, columns, "low"),
      decimal(values, columns, "close"),
      decimal(values, columns, "volume")
    );
    Ohlcv adjustedPrices = new Ohlcv(
      decimal(values, columns, "adjOpen"),
      decimal(values, columns, "adjHigh"),
      decimal(values, columns, "adjLow"),
      decimal(values, columns, "adjClose"),
      decimal(values, columns, "adjVolume")
    );

    return new DailyBar(
      symbol,
      date(values, columns),
      prices,
      Optional.of(adjustedPrices),
      optionalDecimal(values, columns, "divCash"),
      optionalDecimal(values, columns, "splitFactor")
    );
  }

  private static LocalDate date(String[] values, Map<String, Integer> columns) {
    String value = value(values, columns, "date");
    try {
      return LocalDate.parse(value.length() > 10 ? value.substring(0, 10) : value);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid date: " + value, exception);
    }
  }

  private static BigDecimal decimal(String[] values, Map<String, Integer> columns, String column) {
    String value = value(values, columns, column);
    if (value.isBlank()) {
      throw new IllegalArgumentException(column + " is blank");
    }
    return new BigDecimal(value);
  }

  private static Optional<BigDecimal> optionalDecimal(String[] values, Map<String, Integer> columns, String column) {
    String value = value(values, columns, column);
    return value.isBlank() ? Optional.empty() : Optional.of(new BigDecimal(value));
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
