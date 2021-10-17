/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class EventSourceWeakSupport<L> implements Iterable<L> {
  private final ConcurrentLinkedQueue<WeakReference<L>> listeners = new ConcurrentLinkedQueue<>();

  public void add(L listener) {
    listeners.add(new WeakReference<>(listener));
  }

  public boolean isEmpty() {
    for (final var it = listeners.iterator(); it.hasNext(); ) {
      final var l = it.next().get();
      if (l == null) {
        it.remove();
      } else {
        return false;
      }
    }
    return true;
  }

  @Override
  public Iterator<L> iterator() {
    // copy elements into another list in case any event handlers
    // want to add a listener
    final var ret = new ArrayList<L>(listeners.size());
    for (final var it = listeners.iterator(); it.hasNext(); ) {
      final var l = it.next().get();
      if (l == null) {
        it.remove();
      } else {
        ret.add(l);
      }
    }
    return ret.iterator();
  }

  public void remove(L listener) {
    for (final var it = listeners.iterator(); it.hasNext(); ) {
      final var l = it.next().get();
      if (l == null || l == listener) it.remove();
    }
  }
}
