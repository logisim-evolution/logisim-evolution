/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.util.HashMap;

public class UniquelyNamedThread extends Thread {

  private static final Object lock = new Object();
  private static final HashMap<String, Integer> lastID = new HashMap<>();

  private static String nextName(String prefix) {
    int id = 0;
    synchronized (lock) {
      final var i = lastID.get(prefix);
      if (i != null) id = i + 1;
      lastID.put(prefix, id);
    }
    return prefix + "-" + id;
  }

  // private UniquelyNamedThread() {}
  // private UniquelyNamedThread(Runnable runnable) {}

  public UniquelyNamedThread(String prefix) {
    super(nextName(prefix));
  }

  public UniquelyNamedThread(Runnable runnable, String prefix) {
    super(runnable, nextName(prefix));
  }
}
