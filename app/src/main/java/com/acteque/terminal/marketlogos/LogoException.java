package com.acteque.terminal.marketlogos;

/** Normalizes provider and transport failures for callers of instrument logo providers. */
public final class LogoException extends RuntimeException {

  public enum Code {
    AUTHENTICATION,
    RATE_LIMITED,
    NETWORK,
    PROVIDER_ERROR,
    INVALID_RESPONSE,
  }

  private final Code code;

  public LogoException(Code code, String message) {
    super(message);
    this.code = code;
  }

  public LogoException(Code code, String message, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public Code code() {
    return code;
  }
}
