/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

/** QNodeQueue is a simplified version of the Java PriorityQueue interface. */
public interface QNodeQueue<T extends QNode> {
  /**
   * Adds an item to the queue.
   *
   * @param item to be added.
   *
   * @return true if added.
   */
  public boolean add(T item);

  /**
   * Removes all items from the queue.
   */
  public void clear();

  /**
   * @return true if queue is empty, false if it has some items.
   */
  public boolean isEmpty();

  /**
   * @return the smallest node, or null if the queue is empty.
   */
  public T peek();

  /**
   * Removes the smallest node, if any.
   *
   * @return the removed node or null if the queue is empty.
   */
  public T remove();

  /**
   * @return the number of nodes in the queue.
   */
  public int size();
}
