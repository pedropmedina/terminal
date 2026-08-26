package com.acteque.terminal.marketdata.provider.tiingo;

import com.acteque.terminal.marketdata.MarketDataException;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.Objects;

/** Shared authenticated HTTP execution for Tiingo endpoint modules. */
public final class TiingoRequestExecutor {

  private final String apiKey;
  private final TiingoHttpTransport transport;

  TiingoRequestExecutor(String apiKey, TiingoHttpTransport transport) {
    this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  public String getJson(URI uri) {
    return get(uri, "application/json", true).bodyAsString();
  }

  public byte[] getBytes(URI uri, String accept, boolean authenticated) {
    return get(uri, accept, authenticated).body();
  }

  private TiingoHttpTransport.Response get(URI uri, String accept, boolean authenticated) {
    TiingoHttpTransport.Response response;
    try {
      Map<String, String> headers = authenticated
        ? Map.of("Accept", accept, "Authorization", "Token " + apiKey)
        : Map.of("Accept", accept);
      response = transport.get(uri, headers);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Tiingo request was interrupted", exception);
    } catch (IOException exception) {
      throw new MarketDataException(MarketDataException.Code.NETWORK, "Unable to reach Tiingo", exception);
    }

    ensureSuccess(response);
    return response;
  }

  private static void ensureSuccess(TiingoHttpTransport.Response response) {
    int status = response.statusCode();
    if (status >= 200 && status < 300) {
      return;
    }

    MarketDataException.Code code = switch (status) {
      case 401, 403 -> MarketDataException.Code.AUTHENTICATION;
      case 404 -> MarketDataException.Code.NOT_FOUND;
      case 429 -> MarketDataException.Code.RATE_LIMITED;
      default -> MarketDataException.Code.PROVIDER_ERROR;
    };
    throw new MarketDataException(code, "Tiingo request failed with HTTP status " + status);
  }
}
