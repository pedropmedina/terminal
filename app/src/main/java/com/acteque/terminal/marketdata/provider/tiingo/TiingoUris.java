package com.acteque.terminal.marketdata.provider.tiingo;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

public final class TiingoUris {

  private TiingoUris() {}

  public static String ticker(String ticker) {
    return encode(requireText(ticker, "ticker").toUpperCase(Locale.ROOT));
  }

  public static String queryValue(String value) {
    return encode(requireText(value, "value"));
  }

  private static String encode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
  }

  private static String requireText(String value, String name) {
    if (value == null || value.isBlank()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return value.strip();
  }
}
