package com.acteque.terminal.marketdata.provider.tiingo;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import com.acteque.terminal.marketdata.MarketDataException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;

/** Decodes and validates Tiingo responses for the provider adapters. */
public final class TiingoResponseDecoder {

  private static final JsonMapper MAPPER = JsonMapper.builder()
    .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    .disable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
    .build();

  private TiingoResponseDecoder() {}

  public static <T> List<T> readList(String json, TypeReference<List<T>> type, String responseName) {
    try {
      List<T> responses = MAPPER.readValue(json, type);
      if (responses == null) {
        throw invalidResponse("Tiingo " + responseName + " response is not a JSON array", null);
      }
      return responses;
    } catch (JsonProcessingException exception) {
      throw invalidResponse("Unable to parse Tiingo " + responseName, exception);
    }
  }

  public static <T> T readObject(String json, Class<T> type, String responseName) {
    try {
      T response = MAPPER.readValue(json, type);
      if (response == null) {
        throw invalidResponse("Tiingo " + responseName + " response is not a JSON object", null);
      }
      return response;
    } catch (JsonProcessingException exception) {
      throw invalidResponse("Unable to parse Tiingo " + responseName, exception);
    }
  }

  public static <T> T required(T value, String field) {
    if (value == null) {
      throw new IllegalArgumentException(field + " is missing");
    }
    return value;
  }

  public static String requiredText(String value, String field) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(field + " is missing");
    }
    return value;
  }

  public static Optional<String> optionalText(String value) {
    return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
  }

  public static Instant requiredInstant(String value, String field) {
    return optionalInstant(value, field).orElseThrow(() -> new IllegalArgumentException(field + " is missing"));
  }

  public static Optional<Instant> optionalInstant(String value, String field) {
    if (value == null) {
      return Optional.empty();
    }
    if (value.isBlank()) {
      throw new IllegalArgumentException(field + " is not a timestamp");
    }
    try {
      return Optional.of(Instant.parse(value));
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid " + field + ": " + value, exception);
    }
  }

  public static LocalDate requiredDate(String value, String field) {
    String date = requiredText(value, field);
    try {
      return LocalDate.parse(date.length() > 10 ? date.substring(0, 10) : date);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid " + field + ": " + date, exception);
    }
  }

  public static Optional<LocalDate> optionalDate(String value, String field) {
    Optional<String> date = optionalText(value);
    try {
      return date.map(LocalDate::parse);
    } catch (DateTimeParseException exception) {
      throw new IllegalArgumentException("Invalid " + field + ": " + date.orElseThrow(), exception);
    }
  }

  public static MarketDataException invalidResponse(String message, Throwable cause) {
    return new MarketDataException(MarketDataException.Code.INVALID_RESPONSE, message, cause);
  }
}
