/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;
import static com.cburch.logisim.circuit.SplayQueue.Node;

// A simple linked-list queue implementation, using keys of type long, and
// values that extend type SplayQueue.Node. This supports (approximately) a
// subset of the java.util.PriorityQueue API, but only enough to support
// Propagator.
public class LinkedQueue<T extends SplayQueue.Node> {

  // Objects in the queue must be subclasses of SplayQueue.Node.

  private Node head, tail;
  private int size;

  // add(t) inserts a new node into the queue.
  public void add(T t) {
    size++;

    if (tail == null) {
      head = tail = t;
      t.left = t.right = null;
      return;
    }

    // Find node p that should precede t.
    Node p = tail;
    while (p != null && t.key < p.key)
      p = p.left;

    if (p == null) {
      t.right = head;
      t.left = null;
      head.left = t;
      head = t;
    } else {
      t.right = p.right;
      t.left = p;
      if (p.right == null)
        tail = t;
      else
        p.right.left = t;
      p.right = t;
    }
  }

  public int size() {
    return size;
  }

  public boolean isEmpty() {
    return size == 0;
  }

  public void clear() {
    head = tail = null;
    size = 0;
  }

  // peek() returns the smallest node, or null if the queue is empty.
  public T peek() {
    @SuppressWarnings("unchecked")
    T ret = (T) head;
    return ret;
  }

  // remove() removes the smallest node, or null if the queue is empty.
  public T remove() {
    if (head == null)
      return null;
    size--;
    @SuppressWarnings("unchecked")
    T t = (T) head;
    head = head.right;
    if (head == null)
      tail = null;
    else
      head.left = null;
    return t;
  }

  String id(Node n) {
    if (n == null)
      return "null";
    else
      return "@" + n.hashCode();
  }

}
