package com.acteque.terminal.marketlogos;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.RejectedExecutionException;

/** Owns cancellable background logo work for one consumer; the provider can be shared. */
public final class DefaultLogoSession implements LogoSession {

  private final InstrumentLogos provider;
  private final ExecutorService executor;
  private FutureTask<Optional<byte[]>> logoTask;

  public DefaultLogoSession(InstrumentLogos provider) {
    this(provider, Executors.newVirtualThreadPerTaskExecutor());
  }

  /** The session owns the supplied executor and closes it when the session closes. */
  public DefaultLogoSession(InstrumentLogos provider, ExecutorService executor) {
    this.provider = Objects.requireNonNull(provider, "provider");
    this.executor = Objects.requireNonNull(executor, "executor");
  }

  @Override
  public Optional<InstrumentLogo> findLogo(LogoRequest request) {
    return provider.findLogo(Objects.requireNonNull(request, "request"));
  }

  /** Loads a logo independently of history, interrupting any previous logo request. */
  @Override
  public synchronized CompletionStage<Optional<byte[]>> load(InstrumentLogo logo) {
    Objects.requireNonNull(logo, "logo");
    cancel();
    CompletableFuture<Optional<byte[]>> result = new CompletableFuture<>();
    FutureTask<Optional<byte[]>> task = new FutureTask<>(() -> provider.load(logo)) {
      @Override
      protected void done() {
        synchronized (DefaultLogoSession.this) {
          if (logoTask == this) {
            logoTask = null;
          }
        }
        try {
          result.complete(get());
        } catch (CancellationException exception) {
          result.cancel(false);
        } catch (ExecutionException exception) {
          result.completeExceptionally(exception.getCause());
        } catch (InterruptedException exception) {
          Thread.currentThread().interrupt();
          result.completeExceptionally(exception);
        }
      }
    };
    logoTask = task;
    try {
      executor.execute(task);
    } catch (RejectedExecutionException exception) {
      task.cancel(false);
      throw exception;
    }
    return result;
  }

  /** Interrupts the pending logo task and cancels its returned stage; safe when none is pending. */
  @Override
  public synchronized void cancel() {
    FutureTask<Optional<byte[]>> task = logoTask;
    logoTask = null;
    if (task != null) {
      task.cancel(true);
    }
  }

  @Override
  public void close() {
    synchronized (this) {
      // Reject new submissions before cancellation callbacks can attempt another logo load.
      executor.shutdown();
      cancel();
    }
    executor.close();
  }
}
