package com.acteque.terminal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class PriceDataLoader {

  private PriceDataLoader() {}

  static List<PricePoint> loadFromResource(String resourcePath) {
    InputStream stream = PriceDataLoader.class.getResourceAsStream(resourcePath);
    if (stream == null) {
      throw new IllegalStateException("Missing CSV resource: " + resourcePath);
    }
    return load(stream, resourcePath);
  }

  /**
   * Reads OHLCV CSV data and converts each row into the close-price points needed by the chart.
   *
   * <p>The parser validates the minimum column count so bad data fails with a useful message
   * instead of producing a misleading chart.
   */
  static List<PricePoint> load(InputStream stream, String sourceName) {
    List<PricePoint> points = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      String header = reader.readLine();
      if (header == null) {
        throw new IllegalStateException("CSV resource is empty: " + sourceName);
      }

      String line;
      int lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isBlank()) {
          continue;
        }

        String[] columns = line.split(",");
        if (columns.length < 5) {
          throw new IllegalStateException("CSV row " + lineNumber + " has fewer than 5 columns: " + line);
        }

        LocalDate date = LocalDate.parse(columns[0].trim());
        double closePrice = Double.parseDouble(columns[4].trim());
        points.add(new PricePoint(date, closePrice));
      }
    } catch (IOException | RuntimeException exception) {
      throw new IllegalStateException("Unable to read stock data from " + sourceName, exception);
    }

    if (points.isEmpty()) {
      throw new IllegalStateException("CSV resource has no price rows: " + sourceName);
    }
    points.sort(Comparator.comparing(PricePoint::date));
    return List.copyOf(points);
  }
}
