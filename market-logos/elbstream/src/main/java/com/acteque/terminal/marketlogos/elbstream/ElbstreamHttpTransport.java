package com.acteque.terminal.marketlogos.elbstream;

import com.acteque.terminal.marketlogos.LogoException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@FunctionalInterface
interface ElbstreamHttpTransport extends AutoCloseable {
  @Override
  default void close() {}

  Response get(URI uri) throws IOException, InterruptedException;

  record Response(int statusCode, String contentType, byte[] body) {}

  static ElbstreamHttpTransport create() {
    HttpClient client = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(10))
      .followRedirects(HttpClient.Redirect.NEVER)
      .build();
    ElbstreamHttpTransport delegate = using(client, Duration.ofSeconds(20));
    return new ElbstreamHttpTransport() {
      public Response get(URI uri) throws IOException, InterruptedException {
        return delegate.get(uri);
      }

      public void close() {
        client.close();
      }
    };
  }

  static ElbstreamHttpTransport using(HttpClient client, Duration timeout) {
    return uri -> {
      HttpRequest request = HttpRequest.newBuilder(uri).timeout(timeout).header("Accept", "image/png").GET().build();
      CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request, info -> new BoundedBodySubscriber());
      try {
        // Unlike a headers-only timeout, this deadline includes the entire streamed body.
        HttpResponse<byte[]> response = pending.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        return new Response(
          response.statusCode(),
          response.headers().firstValue("Content-Type").orElse(null),
          response.body()
        );
      } catch (TimeoutException exception) {
        throw new HttpTimeoutException("Elbstream whole-body deadline exceeded");
      } catch (ExecutionException exception) {
        Throwable cause = exception.getCause();
        // HttpClient wraps subscriber failures in IOException; preserve our payload error code.
        for (Throwable nested = cause; nested != null; nested = nested.getCause()) {
          if (nested instanceof LogoException failure) {
            throw failure;
          }
        }
        if (cause instanceof IOException failure) {
          throw failure;
        }
        throw new IOException("Elbstream HTTP transfer failed", cause);
      } finally {
        if (!pending.isDone()) {
          pending.cancel(true);
        }
      }
    };
  }

  /** Caps allocation while streaming, before a response body can grow beyond the limit. */
  final class BoundedBodySubscriber implements HttpResponse.BodySubscriber<byte[]> {

    private final CompletableFuture<byte[]> body = new CompletableFuture<>();
    private final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    private Flow.Subscription subscription;

    @Override
    public CompletionStage<byte[]> getBody() {
      return body;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
      this.subscription = subscription;
      subscription.request(1);
    }

    @Override
    public void onNext(List<ByteBuffer> buffers) {
      if (body.isDone()) {
        return;
      }
      for (ByteBuffer buffer : buffers) {
        if (buffer.remaining() > ElbstreamInstrumentLogos.MAX_BYTES - bytes.size()) {
          body.completeExceptionally(
            new LogoException(LogoException.Code.INVALID_RESPONSE, "Elbstream logo exceeds the 1 MiB payload limit")
          );
          subscription.cancel();
          return;
        }
        byte[] chunk = new byte[buffer.remaining()];
        buffer.get(chunk);
        bytes.writeBytes(chunk);
      }
      subscription.request(1);
    }

    @Override
    public void onError(Throwable throwable) {
      body.completeExceptionally(throwable);
    }

    @Override
    public void onComplete() {
      body.complete(bytes.toByteArray());
    }
  }
}
