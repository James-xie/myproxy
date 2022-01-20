package com.gllue.myproxy.common.concurrent;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class WrappedFuture<T> implements ExtensibleFuture<T> {
  private final ExtensibleFuture<T> future;

  @Override
  public T getValue() {
    return future.getValue();
  }

  @Override
  public Throwable getException() {
    return future.getException();
  }

  @Override
  public boolean isSuccess() {
    return future.isSuccess();
  }

  @Override
  public void addListener(Runnable listener, Executor executor) {
    future.addListener(listener, executor);
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    return future.cancel(mayInterruptIfRunning);
  }

  @Override
  public boolean isCancelled() {
    return future.isCancelled();
  }

  @Override
  public boolean isDone() {
    return future.isDone();
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    return future.get();
  }

  @Override
  public T get(long timeout, TimeUnit unit)
      throws InterruptedException, ExecutionException, TimeoutException {
    return future.get(timeout, unit);
  }
}
