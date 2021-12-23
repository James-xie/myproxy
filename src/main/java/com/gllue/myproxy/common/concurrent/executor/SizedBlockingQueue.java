package com.gllue.myproxy.common.concurrent.executor;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nonnull;

/**
 * A size based queue wrapping another blocking queue to provide (somewhat relaxed) capacity checks.
 * Mainly makes sense to use with blocking queues that are unbounded to provide the ability to do
 * capacity verification.
 */
public class SizedBlockingQueue<E> extends AbstractQueue<E> implements BlockingQueue<E> {

  private final BlockingQueue<E> queue;
  private final int capacity;

  private final AtomicInteger size = new AtomicInteger();

  public SizedBlockingQueue(BlockingQueue<E> queue, int capacity) {
    assert capacity >= 0;
    this.queue = queue;
    this.capacity = capacity;
  }

  @Override
  public boolean add(@Nonnull E e) {
    return super.add(e);
  }

  @Override
  public int size() {
    return size.get();
  }

  public int capacity() {
    return this.capacity;
  }

  @Override
  @Nonnull
  public Iterator<E> iterator() {
    final Iterator<E> it = queue.iterator();
    return new Iterator<E>() {
      E current;

      @Override
      public boolean hasNext() {
        return it.hasNext();
      }

      @Override
      public E next() {
        current = it.next();
        return current;
      }

      @Override
      public void remove() {
        // note, we can't call #remove on the iterator because we need to know
        // if it was removed or not
        if (queue.remove(current)) {
          size.decrementAndGet();
        }
      }
    };
  }

  @Override
  public E peek() {
    return queue.peek();
  }

  @Override
  public E poll() {
    E e = queue.poll();
    if (e != null) {
      size.decrementAndGet();
    }
    return e;
  }

  @Override
  public E poll(long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    E e = queue.poll(timeout, unit);
    if (e != null) {
      size.decrementAndGet();
    }
    return e;
  }

  @Override
  public boolean remove(Object o) {
    boolean v = queue.remove(o);
    if (v) {
      size.decrementAndGet();
    }
    return v;
  }

  /** Forces adding an element to the queue, without doing size checks. */
  public void forcePut(E e) throws InterruptedException {
    size.incrementAndGet();
    try {
      queue.put(e);
    } catch (InterruptedException ie) {
      size.decrementAndGet();
      throw ie;
    }
  }

  @Override
  public boolean offer(@Nonnull E e) {
    while (true) {
      final int current = size.get();
      if (current >= capacity()) {
        return false;
      }
      if (size.compareAndSet(current, 1 + current)) {
        break;
      }
    }
    boolean offered = queue.offer(e);
    if (!offered) {
      size.decrementAndGet();
    }
    return offered;
  }

  @Override
  public boolean offer(E e, long timeout, @Nonnull TimeUnit unit) throws InterruptedException {
    // note, not used in ThreadPoolExecutor
    throw new IllegalStateException("offer with timeout not allowed on sized queue");
  }

  @Override
  public void put(@Nonnull E e) throws InterruptedException {
    // note, not used in ThreadPoolExecutor
    throw new IllegalStateException("put not allowed on sized queue");
  }

  @Override
  @Nonnull
  public E take() throws InterruptedException {
    E e = queue.take();
    size.decrementAndGet();
    return e;
  }

  @Override
  public int remainingCapacity() {
    return capacity - size.get();
  }

  @Override
  public int drainTo(@Nonnull Collection<? super E> c) {
    int v = queue.drainTo(c);
    size.addAndGet(-v);
    return v;
  }

  @Override
  public int drainTo(@Nonnull Collection<? super E> c, int maxElements) {
    int v = queue.drainTo(c, maxElements);
    size.addAndGet(-v);
    return v;
  }

  @Override
  @Nonnull
  public Object[] toArray() {
    return queue.toArray();
  }

  @Override
  @Nonnull
  public <T> T[] toArray(@Nonnull T[] a) {
    return (T[]) queue.toArray(a);
  }

  @Override
  public boolean contains(Object o) {
    return queue.contains(o);
  }

  @Override
  public boolean containsAll(@Nonnull Collection<?> c) {
    return queue.containsAll(c);
  }
}