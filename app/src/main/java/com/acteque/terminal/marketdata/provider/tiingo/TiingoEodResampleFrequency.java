package com.acteque.terminal.marketdata.provider.tiingo;

/** Frequencies accepted by Tiingo's end-of-day historical prices endpoint. */
public enum TiingoEodResampleFrequency {
  DAILY("daily"),
  WEEKLY("weekly"),
  MONTHLY("monthly"),
  ANNUALLY("annually");

  private final String apiValue;

  TiingoEodResampleFrequency(String apiValue) {
    this.apiValue = apiValue;
  }

  String apiValue() {
    return apiValue;
  }
}
