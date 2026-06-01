/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;
import java.lang.reflect.Array;

/**
 * Allows immutable objects to be cached in memory in order to reduce the creation of duplicate
 * objects.
 */
public class Cache<T> {
  private final int mask;
  private final T[] data;

  public Cache(Class<T> c) {
    this(c, 8);
  }

  public Cache(Class<T> c, int logSize) {
    if (logSize > 12) logSize = 12;
    @SuppressWarnings("unchecked")
    final T[] d = (T[]) Array.newInstance(c, 1 << logSize);
    data = d;
    mask = data.length - 1;
  }

  public T get(int hashCode) {
    return data[hashCode & mask];
  }

  public T get(T value) {
    if (value == null) return null;
    int code = value.hashCode() & mask;
    final var ret = data[code];
    if (ret != null && ret.equals(value)) {
      return ret;
    } else {
      data[code] = value;
      return value;
    }
  }

  public void put(int hashCode, T value) {
    if (value != null) data[hashCode & mask] = value;
  }


  public void put(T value) {
    if (value != null) data[value.hashCode() & mask] = value;
  }
}
