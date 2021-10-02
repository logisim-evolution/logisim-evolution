/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

/**
 * Allows immutable objects to be cached in memory in order to reduce the creation of duplicate
 * objects.
 */
public class Cache {
  private final int mask;
  private final Object[] data;

  public Cache() {
    this(8);
  }

  public Cache(int logSize) {
    if (logSize > 12) logSize = 12;

    data = new Object[1 << logSize];
    mask = data.length - 1;
  }

  public Object get(int hashCode) {
    return data[hashCode & mask];
  }

  public Object get(Object value) {
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

  public void put(int hashCode, Object value) {
    if (value != null) data[hashCode & mask] = value;
  }
}
