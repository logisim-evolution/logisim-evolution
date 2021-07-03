/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

public class Dag {
  private static class Node {
    @SuppressWarnings("unused")
    Object data;

    final HashSet<Node> succs = new HashSet<>(); // of Nodes
    int numPreds = 0;
    boolean mark;

    Node(Object data) {
      this.data = data;
    }
  }

  private final HashMap<Object, Node> nodes = new HashMap<>();

  public Dag() {}

  public boolean addEdge(Object srcData, Object dstData) {
    if (!canFollow(dstData, srcData)) return false;

    final var src = createNode(srcData);
    final var dst = createNode(dstData);
    if (src.succs.add(dst)) ++dst.numPreds; // add since not already present
    return true;
  }

  private boolean canFollow(Node query, Node base) {
    if (base == query) return false;

    // mark all as unvisited
    for (final var n : nodes.values()) {
      n.mark = false; // will become true once reached
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
    final var queryNode = findNode(query);
    final var baseNode = findNode(base);
    if (baseNode == null || queryNode == null) {
      return !base.equals(query);
    } else {
      return canFollow(queryNode, baseNode);
    }
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
