package com.acteque.terminal.marketlogos.provider.elbstream;

import static org.junit.jupiter.api.Assertions.*;

import com.acteque.terminal.marketlogos.LogoException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class ElbstreamHttpTransportTest {

  @Test
  void acceptsExactlyTheLimitAcrossChunksAndCancelsBeforeOverflow() {
    var subscriber = new ElbstreamHttpTransport.BoundedBodySubscriber();
    Subscription subscription = new Subscription();
    subscriber.onSubscribe(subscription);
    subscriber.onNext(List.of(ByteBuffer.wrap(new byte[ElbstreamInstrumentLogos.MAX_BYTES - 1])));
    subscriber.onNext(List.of(ByteBuffer.wrap(new byte[1])));
    subscriber.onComplete();
    assertEquals(ElbstreamInstrumentLogos.MAX_BYTES, subscriber.getBody().toCompletableFuture().join().length);
    assertFalse(subscription.cancelled);

    var oversized = new ElbstreamHttpTransport.BoundedBodySubscriber();
    Subscription cancelled = new Subscription();
    oversized.onSubscribe(cancelled);
    oversized.onNext(
      List.of(ByteBuffer.wrap(new byte[ElbstreamInstrumentLogos.MAX_BYTES]), ByteBuffer.wrap(new byte[1]))
    );
    assertTrue(cancelled.cancelled);
    CompletionException failure = assertThrows(CompletionException.class, () ->
      oversized.getBody().toCompletableFuture().join()
    );
    assertEquals(LogoException.Code.INVALID_RESPONSE, assertInstanceOf(LogoException.class, failure.getCause()).code());
  }

  @Test
  void propagatesStreamFailures() {
    var subscriber = new ElbstreamHttpTransport.BoundedBodySubscriber();
    subscriber.onSubscribe(new Subscription());
    RuntimeException failure = new RuntimeException("broken stream");
    subscriber.onError(failure);
    assertSame(
      failure,
      assertThrows(CompletionException.class, () -> subscriber.getBody().toCompletableFuture().join()).getCause()
    );
  }

  @Test
  void wholeBodyDeadlineAppliesAfterHeadersArrive() throws Exception {
    CountDownLatch sentHeaders = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    try (
      ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build();
      var executor = Executors.newVirtualThreadPerTaskExecutor()
    ) {
      server.setSoTimeout(5000);
      var serving = executor.submit(() -> {
        try (var socket = server.accept()) {
          socket.setSoTimeout(5000);
          var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
          while (!reader.readLine().isEmpty()) {}
          socket
            .getOutputStream()
            .write(
              "HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: 100\r\n\r\n".getBytes(
                StandardCharsets.US_ASCII
              )
            );
          socket.getOutputStream().flush();
          sentHeaders.countDown();
          release.await(5, TimeUnit.SECONDS);
        }
        return null;
      });
      try {
        URI uri = new URI(
          "http",
          null,
          server.getInetAddress().getHostAddress(),
          server.getLocalPort(),
          "/logo",
          null,
          null
        );
        assertThrows(HttpTimeoutException.class, () ->
          ElbstreamHttpTransport.using(client, Duration.ofSeconds(1)).get(uri)
        );
        assertEquals(0, sentHeaders.getCount(), "The timeout must cover a stalled body, not just connection setup");
      } finally {
        release.countDown();
      }
      serving.get(5, TimeUnit.SECONDS);
    }
  }

  @Test
  void streamedOversizeIsAnInvalidResponseThroughHttpClient() throws Exception {
    try (
      ServerSocket server = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
      HttpClient client = HttpClient.newHttpClient();
      var executor = Executors.newVirtualThreadPerTaskExecutor()
    ) {
      server.setSoTimeout(5000);
      var serving = executor.submit(() -> {
        try (var socket = server.accept()) {
          socket.setSoTimeout(5000);
          var reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
          while (!reader.readLine().isEmpty()) {}
          socket
            .getOutputStream()
            .write(
              (
                "HTTP/1.1 200 OK\r\nContent-Type: image/png\r\nContent-Length: " +
                (ElbstreamInstrumentLogos.MAX_BYTES + 1) +
                "\r\n\r\n"
              ).getBytes(StandardCharsets.US_ASCII)
            );
          socket.getOutputStream().write(new byte[ElbstreamInstrumentLogos.MAX_BYTES + 1]);
          socket.getOutputStream().flush();
        }
        return null;
      });
      URI uri = new URI(
        "http",
        null,
        server.getInetAddress().getHostAddress(),
        server.getLocalPort(),
        "/logo",
        null,
        null
      );
      LogoException failure = assertThrows(LogoException.class, () ->
        ElbstreamHttpTransport.using(client, Duration.ofSeconds(3)).get(uri)
      );
      assertEquals(LogoException.Code.INVALID_RESPONSE, failure.code());
      serving.get(5, TimeUnit.SECONDS);
    }
  }

  private static class Subscription implements Flow.Subscription {

    boolean cancelled;

    public void request(long count) {}

    public void cancel() {
      cancelled = true;
    }
  }
}
