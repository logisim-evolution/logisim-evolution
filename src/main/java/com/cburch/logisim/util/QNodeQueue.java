/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

// This is a simplified version of the Java PriorityQueue interface.
public interface QNodeQueue<T extends QNode> {
  public boolean add(T item);
  public void clear();
  public boolean isEmpty();
  public T peek();
  public T remove();
  public int size();
}
