package com.acteque.terminal.marketdata.provider.tiingo;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@FunctionalInterface
interface TiingoHttpTransport {
  Response get(URI uri, Map<String, String> headers) throws IOException, InterruptedException;

  record Response(int statusCode, String body) {}

  static TiingoHttpTransport using(HttpClient httpClient) {
    return (uri, headers) -> {
      HttpRequest.Builder request = HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(30)).GET();
      headers.forEach(request::header);

      HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
      return new Response(response.statusCode(), response.body());
    };
  }
}
