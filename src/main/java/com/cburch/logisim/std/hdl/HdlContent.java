/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.util.Arrays;

public abstract class HdlContent implements HdlModel, Cloneable {

  protected static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  protected EventSourceWeakSupport<HdlModelListener> listeners;

  protected HdlContent() {
    this.listeners = null;
  }

  @Override
  public void addHdlModelListener(HdlModelListener l) {
    if (listeners == null) {
      listeners = new EventSourceWeakSupport<>();
    }
    listeners.add(l);
  }

  @Override
  public HdlContent clone() throws CloneNotSupportedException {
    final var ret = (HdlContent) super.clone();
    ret.listeners = null;
    return ret;
  }

  protected void fireContentSet() {
    if (listeners == null) {
      return;
    }

    boolean found = false;
    for (HdlModelListener l : listeners) {
      found = true;
      l.contentSet(this);
    }

    if (!found) {
      listeners = null;
    }
  }

  @Override
  public void removeHdlModelListener(HdlModelListener l) {
    if (listeners == null) {
      return;
    }
    listeners.remove(l);
    if (listeners.isEmpty()) {
      listeners = null;
    }
  }
}
