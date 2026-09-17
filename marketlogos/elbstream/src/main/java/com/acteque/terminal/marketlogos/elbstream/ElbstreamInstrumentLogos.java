package com.acteque.terminal.marketlogos.elbstream;

import com.acteque.terminal.marketlogos.InstrumentLogo;
import com.acteque.terminal.marketlogos.LogoException;
import com.acteque.terminal.marketlogos.LogoProvider;
import com.acteque.terminal.marketlogos.LogoRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;

/**
 * Unauthenticated Elbstream symbol logos. Mapping is best effort: provider-scoped symbols
 * are passed unchanged, including punctuation, without exchange guessing or suffix removal.
 * A reference is not proof that a logo exists or identifies the intended instrument.
 * No image cache is maintained. Display must include the returned attribution; consumers
 * remain responsible for Elbstream's display/usage terms (including visible 12pt attribution).
 */
public final class ElbstreamInstrumentLogos implements LogoProvider {

  private static final String ENDPOINT = "https://api.elbstream.com/logos/symbol/";
  private static final String QUERY = "format=png&size=64";
  private static final byte[] PNG_SIGNATURE = { (byte) 137, 80, 78, 71, 13, 10, 26, 10 };
  static final int MAX_BYTES = 1024 * 1024;
  private final ElbstreamHttpTransport transport;
  private boolean closed;

  public static ElbstreamInstrumentLogos create() {
    return new ElbstreamInstrumentLogos(ElbstreamHttpTransport.create());
  }

  ElbstreamInstrumentLogos(ElbstreamHttpTransport transport) {
    this.transport = Objects.requireNonNull(transport, "transport");
  }

  @Override
  public String provider() {
    return "elbstream";
  }

  @Override
  public synchronized void close() {
    if (closed) return;
    closed = true;
    transport.close();
  }

  @Override
  public Optional<InstrumentLogo> findLogo(LogoRequest request) {
    String symbol = Objects.requireNonNull(request, "request").symbol();
    // Dot-only segments have path traversal semantics rather than ticker semantics.
    if (symbol.equals(".") || symbol.equals("..")) {
      return Optional.empty();
    }
    return Optional.of(
      new InstrumentLogo(
        URI.create(ENDPOINT + encodeSegment(symbol) + "?" + QUERY),
        "Logos by Elbstream",
        URI.create("https://elbstream.com")
      )
    );
  }

  @Override
  public Optional<byte[]> load(InstrumentLogo logo) {
    URI uri = Objects.requireNonNull(logo, "logo").imageUri();
    validateEndpoint(uri);
    ElbstreamHttpTransport.Response response;
    try {
      response = transport.get(uri);
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      throw new LogoException(LogoException.Code.NETWORK, "Elbstream request was interrupted", exception);
    } catch (IOException exception) {
      throw new LogoException(LogoException.Code.NETWORK, "Unable to load Elbstream logo", exception);
    }
    int status = response.statusCode();
    if (status == 404) {
      return Optional.empty();
    }
    if (status != 200) {
      LogoException.Code code = switch (status) {
        case 401, 403 -> LogoException.Code.AUTHENTICATION;
        case 429 -> LogoException.Code.RATE_LIMITED;
        default -> LogoException.Code.PROVIDER_ERROR;
      };
      throw new LogoException(code, "Elbstream request failed with HTTP status " + status);
    }
    byte[] body = response.body();
    String type = response.contentType();
    if (
      type == null ||
      !type.split(";", 2)[0].strip().equalsIgnoreCase("image/png") ||
      body == null ||
      body.length < PNG_SIGNATURE.length ||
      body.length > MAX_BYTES
    ) {
      throw invalidResponse();
    }
    for (int i = 0; i < PNG_SIGNATURE.length; i++) {
      if (body[i] != PNG_SIGNATURE[i]) {
        throw invalidResponse();
      }
    }
    // Full decoding is deliberately left to the UI; a signature does not prove the PNG is decodable.
    return Optional.of(body);
  }

  private static LogoException invalidResponse() {
    return new LogoException(LogoException.Code.INVALID_RESPONSE, "Elbstream returned an invalid PNG payload");
  }

  private static void validateEndpoint(URI uri) {
    String path = uri.getRawPath();
    String prefix = "/logos/symbol/";
    if (
      !"https".equals(uri.getScheme()) ||
      !"api.elbstream.com".equals(uri.getHost()) ||
      uri.getPort() != -1 ||
      uri.getRawUserInfo() != null ||
      uri.getRawFragment() != null ||
      !QUERY.equals(uri.getRawQuery()) ||
      path == null ||
      !path.startsWith(prefix)
    ) {
      throw new IllegalArgumentException("Logo reference is outside the Elbstream symbol endpoint");
    }
    String segment = path.substring(prefix.length());
    // Only the exact canonical encoding produced by findLogo is accepted. Encoded separators
    // stay inside one segment, and redirects are never followed by the transport.
    String decoded = uri.getPath().substring(prefix.length());
    if (decoded.isBlank() || decoded.equals(".") || decoded.equals("..") || !segment.equals(encodeSegment(decoded))) {
      throw new IllegalArgumentException("Logo reference contains a noncanonical Elbstream symbol");
    }
  }

  private static String encodeSegment(String symbol) {
    StringBuilder encoded = new StringBuilder();
    for (byte value : symbol.getBytes(StandardCharsets.UTF_8)) {
      int b = value & 0xff;
      if (
        (b >= 'a' && b <= 'z') ||
        (b >= 'A' && b <= 'Z') ||
        (b >= '0' && b <= '9') ||
        b == '-' ||
        b == '.' ||
        b == '_' ||
        b == '~'
      ) {
        encoded.append((char) b);
      } else {
        encoded
          .append('%')
          .append("0123456789ABCDEF".charAt(b >>> 4))
          .append("0123456789ABCDEF".charAt(b & 15));
      }
    }
    return encoded.toString();
  }
}
