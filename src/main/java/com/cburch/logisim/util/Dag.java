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
import java.util.HashSet;
import java.util.LinkedList;

public class Dag {
  private static class Node {
    @SuppressWarnings("unused")
    Object data;

    final HashSet<Node> succs = new HashSet<>();
    int numPreds = 0;
    boolean mark;

    Node(Object data) {
      this.data = data;
    }
  }

  private final HashMap<Object, Node> nodes = new HashMap<>();

  public boolean addEdge(Object srcData, Object dstData) {
    if (!canFollow(dstData, srcData)) return false;

    final var src = createNode(srcData);
    if (src == null) return false;
    final var dst = createNode(dstData);
    if (dst == null) return false;
    if (src.succs.add(dst)) {
      // add since not already present
      ++dst.numPreds;
    }
    return true;
  }

  private boolean canFollow(Node query, Node base) {
    if (base == query) return false;

    // mark all as unvisited
    for (final var n : nodes.values()) {
      // will become true once reached
      n.mark = false;
    }

    // Search starting at query: If base is found, then it follows
    // the query already, and so query cannot follow base.
    final var fringe = new LinkedList<Node>();
    fringe.add(query);
    while (!fringe.isEmpty()) {
      final var n = fringe.removeFirst();
      for (Node next : n.succs) {
        if (!next.mark) {
          if (next == base) return false;
          next.mark = true;
          fringe.addLast(next);
        }
      }
    }
    return true;
  }

  public boolean canFollow(Object query, Object base) {
    if (base == null || query == null) return false;
    final var queryNode = findNode(query);
    final var baseNode = findNode(base);
    return (baseNode == null || queryNode == null)
        ? !query.equals(base)
        : canFollow(queryNode, baseNode);
  }

  private Node createNode(Object data) {
    var ret = findNode(data);
    if (ret != null) return ret;
    if (data == null) return null;

    ret = new Node(data);
    nodes.put(data, ret);
    return ret;
  }

  private Node findNode(Object data) {
    if (data == null) return null;
    return nodes.get(data);
  }

  public boolean hasPredecessors(Object data) {
    final var from = findNode(data);
    return from != null && from.numPreds != 0;
  }

  public boolean hasSuccessors(Object data) {
    final var to = findNode(data);
    return to != null && !to.succs.isEmpty();
  }

  public boolean removeEdge(Object srcData, Object dstData) {
    // returns true if the edge could be removed
    final var src = findNode(srcData);
    final var dst = findNode(dstData);
    if (src == null || dst == null) return false;
    if (!src.succs.remove(dst)) return false;

    --dst.numPreds;
    if (dst.numPreds == 0 && dst.succs.isEmpty()) nodes.remove(dstData);
    if (src.numPreds == 0 && src.succs.isEmpty()) nodes.remove(srcData);
    return true;
  }

  public void removeNode(Object data) {
    final var n = findNode(data);
    if (n == null) return;

    for (final var it = n.succs.iterator(); it.hasNext(); ) {
      final var succ = it.next();
      --(succ.numPreds);
      if (succ.numPreds == 0 && succ.succs.isEmpty()) it.remove();
    }

    if (n.numPreds > 0) {
      nodes.values().removeIf(q -> q.succs.remove(n) && q.numPreds == 0 && q.succs.isEmpty());
    }
  }
}
