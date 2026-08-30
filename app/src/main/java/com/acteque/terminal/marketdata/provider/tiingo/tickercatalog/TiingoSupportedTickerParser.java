package com.acteque.terminal.marketdata.provider.tiingo.tickercatalog;

import com.acteque.terminal.marketdata.MarketDataException;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class TiingoSupportedTickerParser {

  private static final List<String> REQUIRED_COLUMNS = List.of(
    "ticker",
    "exchange",
    "assetType",
    "priceCurrency",
    "startDate",
    "endDate"
  );

  private TiingoSupportedTickerParser() {}

  static List<TiingoSupportedTicker> parse(byte[] archive) {
    try (
      ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive));
      BufferedReader reader = csvReader(zip)
    ) {
      String header = reader.readLine();
      if (header == null) {
        throw invalidResponse("Tiingo supported-ticker CSV is empty", null);
      }

      Map<String, Integer> columns = columns(header);
      List<TiingoSupportedTicker> tickers = new ArrayList<>();
      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }
        try {
          tickers.add(toTicker(line.split(",", -1), columns));
        } catch (RuntimeException exception) {
          throw invalidResponse("Invalid Tiingo supported-ticker row " + lineNumber, exception);
        }
      }
      return List.copyOf(tickers);
    } catch (MarketDataException exception) {
      throw exception;
    } catch (IOException exception) {
      throw invalidResponse("Unable to read Tiingo supported-ticker archive", exception);
    }
  }

  private static BufferedReader csvReader(ZipInputStream zip) throws IOException {
    ZipEntry entry;
    while ((entry = zip.getNextEntry()) != null) {
      if (!entry.isDirectory() && entry.getName().endsWith(".csv")) {
        return new BufferedReader(new InputStreamReader(zip, StandardCharsets.UTF_8));
      }
    }
    throw invalidResponse("Tiingo supported-ticker archive contains no CSV file", null);
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
      throw invalidResponse("Tiingo supported-ticker CSV is missing columns: " + missing, null);
    }
    return columns;
  }

  private static TiingoSupportedTicker toTicker(String[] values, Map<String, Integer> columns) {
    return new TiingoSupportedTicker(
      value(values, columns, "ticker"),
      value(values, columns, "exchange"),
      value(values, columns, "assetType"),
      value(values, columns, "priceCurrency"),
      optionalDate(values, columns, "startDate"),
      optionalDate(values, columns, "endDate")
    );
  }

  private static Optional<LocalDate> optionalDate(String[] values, Map<String, Integer> columns, String column) {
    String value = value(values, columns, column);
    if (value.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(LocalDate.parse(value));
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid " + column + ": " + value, exception);
    }
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
