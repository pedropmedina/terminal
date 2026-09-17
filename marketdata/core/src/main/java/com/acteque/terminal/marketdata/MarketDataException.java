package com.acteque.terminal.marketdata;

/** Normalizes provider and transport failures for callers of market-data clients. */
public final class MarketDataException extends RuntimeException {

  public enum Code {
    AUTHENTICATION,
    NOT_FOUND,
    RATE_LIMITED,
    NETWORK,
    PROVIDER_ERROR,
    INVALID_RESPONSE,
  }

  private final Code code;

  public MarketDataException(Code code, String message) {
    super(message);
    this.code = code;
  }

  public MarketDataException(Code code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
