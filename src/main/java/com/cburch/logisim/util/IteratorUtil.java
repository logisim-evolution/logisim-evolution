/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

public final class IteratorUtil {

  private IteratorUtil() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  private static final class EmptyIterator<E> implements Iterator<E> {
    private EmptyIterator() {}

    @Override
    public boolean hasNext() {
      return false;
    }

    @Override
    public E next() {
      throw new NoSuchElementException();
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException("EmptyIterator.remove");
    }
  }

  private static final class IteratorUnion<E> implements Iterator<E> {
    Iterator<? extends E> cur;
    final Iterator<? extends E> next;

    private IteratorUnion(Iterator<? extends E> cur, Iterator<? extends E> next) {
      this.cur = cur;
      this.next = next;
    }

    @Override
    public boolean hasNext() {
      return cur.hasNext() || (next != null && next.hasNext());
    }

    @Override
    public E next() {
      if (!cur.hasNext()) {
        if (next == null) throw new NoSuchElementException();
        cur = next;
        if (!cur.hasNext()) throw new NoSuchElementException();
      }
      return cur.next();
    }

    @Override
    public void remove() {
      cur.remove();
    }
  }

  public static <E> Iterator<E> createJoinedIterator(
      Iterator<? extends E> i0, Iterator<? extends E> i1) {
    if (!i0.hasNext()) {
      @SuppressWarnings("unchecked")
      Iterator<E> ret = (Iterator<E>) i1;
      return ret;
    } else if (!i1.hasNext()) {
      @SuppressWarnings("unchecked")
      Iterator<E> ret = (Iterator<E>) i0;
      return ret;
    } else {
      return new IteratorUnion<>(i0, i1);
    }
  }

  public static <E> Iterator<E> emptyIterator() {
    return new EmptyIterator<>();
  }
}
