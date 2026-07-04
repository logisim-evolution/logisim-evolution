/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.NoSuchElementException;

public class CircularArray<E> {
  private E[] values;
  private int size = 0;
  private int frontIndex = 0;
  private int backIndex;

  @SuppressWarnings("unchecked")
  public CircularArray(int initialCapacity) {
    super();
    if (initialCapacity <= 0) throw new IllegalArgumentException("Initial Capacity must be positive");
    values = (E[]) new Object[initialCapacity];
    backIndex = values.length - 1;
  }

  public CircularArray() {
    this(16);
  }

  /** Maps user's logical index into physical index in values array. */
  private int mapIndex(int logicalIndex) {
    if (logicalIndex < 0 || logicalIndex >= size) {
      throw new IndexOutOfBoundsException("Index: " + logicalIndex + ", Size: " + size);
    }
    return (logicalIndex + frontIndex)  % values.length;
  }

  /** returns (index + amount) mod values.length, regular math mod, not hardware remainder. */
  private int wrapIndexPlusAmount(int index, int amount) {
    int rem = (index + amount) % values.length;
    return rem < 0 ? rem + values.length : rem;
  }

  public void ensureCapacity(int minCapacity) {
    if (minCapacity <= values.length) return;
    int newSize = Math.max(minCapacity, values.length * 2);
    @SuppressWarnings("unchecked")
    E[] newValues = (E[]) new Object[newSize];
    if (size > 0) {
      if (frontIndex <= backIndex) {
        System.arraycopy(values, frontIndex, newValues, 0, size);
      } else {
        System.arraycopy(values, frontIndex, newValues, 0, values.length - frontIndex);
        System.arraycopy(values, 0, newValues, values.length - frontIndex, backIndex + 1);
      }
    }
    values = newValues;
    frontIndex = 0;
    backIndex = wrapIndexPlusAmount(size, -1);
  }

  public int capacity() {
    return values.length;
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  @SuppressWarnings("unchecked")
  public void clear() {
    values = (E[]) new Object[16];
    size = 0;
    frontIndex = 0;
    backIndex = values.length - 1;
  }

  public E get(int index) {
    return values[mapIndex(index)];
  }

  public void set(int index, E value) {
    values[mapIndex(index)] = value;
  }

  public void addFirst(E value) {
    ensureCapacity(size + 1);
    frontIndex = wrapIndexPlusAmount(frontIndex, -1);
    size++;
    values[frontIndex] = value;
  }

  public void addLast(E value) {
    ensureCapacity(size + 1);
    backIndex = wrapIndexPlusAmount(backIndex, 1);
    size++;
    values[backIndex] = value;
  }

  public E removeFirst() {
    if (size <= 0) throw new NoSuchElementException("Empty array");
    E ret = values[frontIndex];
    values[frontIndex] = null;
    frontIndex = wrapIndexPlusAmount(frontIndex, 1);
    size--;
    return ret;
  }

  public E removeLast() {
    if (size <= 0) throw new NoSuchElementException("Empty array");
    E ret = values[backIndex];
    values[backIndex] = null;
    backIndex = wrapIndexPlusAmount(backIndex, -1);
    size--;
    return ret;
  }
}
