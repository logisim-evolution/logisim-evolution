/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

class Connector {

  private static final int MAX_SECONDS = 10;
  private static final int MAX_ORDERING_TRIES = 10;
  private static final int MAX_SEARCH_ITERATIONS = 20000;

  static final String ALLOW_NEITHER = "neither";
  static final String ALLOW_VERTICAL = "vert";
  static final String ALLOW_HORIZONTAL = "horz";

  private Connector() {}

  static MoveResult computeWires(MoveRequest req) {
    final var gesture = req.getMoveGesture();
    final var dx = req.getDeltaX();
    final var dy = req.getDeltaY();
    final var baseConnects = new ArrayList<>(gesture.getConnections());
    final var impossible = pruneImpossible(baseConnects, gesture.getFixedAvoidanceMap(), dx, dy);

    final var selAvoid = AvoidanceMap.create(gesture.getSelected(), dx, dy);
    final var pathLocs = new HashMap<ConnectionData, Set<Location>>();
    final var initNodes = new HashMap<ConnectionData, List<SearchNode>>();
    for (final var conn : baseConnects) {
      final var connLocs = new HashSet<Location>();
      final var connNodes = new ArrayList<SearchNode>();
      processConnection(conn, dx, dy, connLocs, connNodes, selAvoid);
      pathLocs.put(conn, connLocs);
      initNodes.put(conn, connNodes);
    }

    MoveResult bestResult = null;
    final var tries = switch (baseConnects.size()) {
      case 0 -> 0;
      case 1 -> 1;
      case 2 -> 2;
      case 3 -> 8;
      default -> MAX_ORDERING_TRIES;
    };
    final var stopTime = System.currentTimeMillis() + MAX_SECONDS * 1000;
    for (var tryNum = 0; tryNum < tries && stopTime - System.currentTimeMillis() > 0; tryNum++) {
      if (ConnectorThread.isOverrideRequested()) return null;
      final var connects = new ArrayList<ConnectionData>(baseConnects);
      if (tryNum < 2) {
        sortConnects(connects, dx, dy);
        if (tryNum == 1) Collections.reverse(connects);
      } else {
        Collections.shuffle(connects);
      }

      final var candidate = tryList(req, gesture, connects, dx, dy, pathLocs, initNodes, stopTime);
      if (candidate == null) {
        return null;
      } else if (bestResult == null) {
        bestResult = candidate;
      } else {
        final var unsatisfied1 = bestResult.getUnsatisifiedConnections().size();
        final var unsatisfied2 = candidate.getUnsatisifiedConnections().size();
        if (unsatisfied2 < unsatisfied1) {
          bestResult = candidate;
        } else if (unsatisfied2 == unsatisfied1) {
          final var dist1 = bestResult.getTotalDistance();
          final var dist2 = candidate.getTotalDistance();
          if (dist2 < dist1) bestResult = candidate;
        }
      }
    }
    if (bestResult == null) { // should only happen for no connections
      bestResult = new MoveResult(req, new ReplacementMap(), impossible, 0);
    } else {
      bestResult.addUnsatisfiedConnections(impossible);
    }
    return bestResult;
  }

  private static ArrayList<Location> convertToPath(SearchNode last) {
    var next = last;
    var prev = last.getPrevious();
    final var ret = new ArrayList<Location>();
    ret.add(next.getLocation());
    while (prev != null) {
      if (prev.getDirection() != next.getDirection()) {
        ret.add(prev.getLocation());
      }
      next = prev;
      prev = prev.getPrevious();
    }
    if (!ret.get(ret.size() - 1).equals(next.getLocation())) {
      ret.add(next.getLocation());
    }
    Collections.reverse(ret);
    return ret;
  }

  private static SearchNode findShortestPath(List<SearchNode> nodes, Set<Location> pathLocs, AvoidanceMap avoid) {
    final var q = new PriorityQueue<SearchNode>(nodes);
    final var visited = new HashSet<SearchNode>();
    var iters = 0;
    while (!q.isEmpty() && iters < MAX_SEARCH_ITERATIONS) {
      iters++;
      final var node = q.remove();
      if (iters % 64 == 0 && ConnectorThread.isOverrideRequested() || node == null) {
        return null;
      }
      if (node.isDestination()) {
        return node;
      }
      boolean added = visited.add(node);
      if (!added) {
        continue;
      }
      final var loc = node.getLocation();
      var dir = node.getDirection();
      var neighbors = 3;
      var allowed = avoid.get(loc);
      if (allowed != null && node.isStart() && pathLocs.contains(loc)) {
        allowed = null;
      }
      if (allowed == ALLOW_NEITHER) {
        neighbors = 0;
      } else if (allowed == ALLOW_VERTICAL) {
        if (dir == null) {
          dir = Direction.NORTH;
          neighbors = 2;
        } else if (dir == Direction.NORTH || dir == Direction.SOUTH) {
          neighbors = 1;
        } else {
          neighbors = 0;
        }
      } else if (allowed == ALLOW_HORIZONTAL) {
        if (dir == null) {
          dir = Direction.EAST;
          neighbors = 2;
        } else if (dir == Direction.EAST || dir == Direction.WEST) {
          neighbors = 1;
        } else {
          neighbors = 0;
        }
      } else {
        if (dir == null) {
          dir = Direction.NORTH;
          neighbors = 4;
        } else {
          neighbors = 3;
        }
      }
      for (int i = 0; i < neighbors; i++) {
        Direction oDir = switch (i) {
          case 0 -> dir;
          case 1 -> neighbors == 2 ? dir.reverse() : dir.getLeft();
          case 2 -> dir.getRight();
          // must be 3
          default -> dir.reverse();
        };
        var nextSearchNode = node.next(oDir, allowed != null);
        if (nextSearchNode != null && !visited.contains(nextSearchNode)) {
          q.add(nextSearchNode);
        }
      }
    }
    return null;
  }

  private static void processConnection(
      ConnectionData conn,
      int dx,
      int dy,
      Set<Location> connLocs,
      List<SearchNode> connNodes,
      AvoidanceMap selAvoid) {
    final var cur = conn.getLocation();
    final var dest = cur.translate(dx, dy);
    if (selAvoid.get(cur) == null) {
      var preferred = conn.getDirection();
      if (preferred == null) {
        preferred = (Math.abs(dx) > Math.abs(dy))
                    ? dx > 0 ? Direction.EAST : Direction.WEST
                    : dy > 0 ? Direction.SOUTH : Direction.NORTH;
      }

      connLocs.add(cur);
      connNodes.add(new SearchNode(conn, cur, preferred, dest));
    }

    for (final var wire : conn.getWirePath()) {
      for (final var loc : wire) {
        if (selAvoid.get(loc) == null || loc.equals(dest)) {
          var added = connLocs.add(loc);
          if (added) {
            Direction dir = null;
            if (wire.endsAt(loc)) {
              if (wire.isVertical()) {
                final var y0 = loc.getY();
                final var y1 = wire.getOtherEnd(loc).getY();
                dir = y0 < y1 ? Direction.NORTH : Direction.SOUTH;
              } else {
                final var x0 = loc.getX();
                final var x1 = wire.getOtherEnd(loc).getX();
                dir = x0 < x1 ? Direction.WEST : Direction.EAST;
              }
            }
            connNodes.add(new SearchNode(conn, loc, dir, dest));
          }
        }
      }
    }
  }

  private static void processPath(
      List<Location> path,
      ConnectionData conn,
      AvoidanceMap avoid,
      ReplacementMap repl,
      Set<Location> unmarkable) {
    final var pathIt = path.iterator();
    var loc0 = pathIt.next();
    if (!loc0.equals(conn.getLocation())) {
      var pathLoc = conn.getWirePathStart();
      var found = loc0.equals(pathLoc);
      for (final var wire : conn.getWirePath()) {
        final var nextLoc = wire.getOtherEnd(pathLoc);
        if (found) { // existing wire will be removed
          repl.remove(wire);
          avoid.unmarkWire(wire, nextLoc, unmarkable);
        } else if (wire.contains(loc0)) { // wires after this will be
          // removed
          found = true;
          if (!loc0.equals(nextLoc)) {
            avoid.unmarkWire(wire, nextLoc, unmarkable);
            final var shortenedWire = Wire.create(pathLoc, loc0);
            repl.replace(wire, shortenedWire);
            avoid.markWire(shortenedWire, 0, 0);
          }
        }
        pathLoc = nextLoc;
      }
    }
    while (pathIt.hasNext()) {
      final var loc1 = pathIt.next();
      final var newWire = Wire.create(loc0, loc1);
      repl.add(newWire);
      avoid.markWire(newWire, 0, 0);
      loc0 = loc1;
    }
  }

  private static List<ConnectionData> pruneImpossible(List<ConnectionData> connects, AvoidanceMap avoid, int dx, int dy) {
    final var pathWires = new ArrayList<Wire>();
    for (final var conn : connects) {
      pathWires.addAll(conn.getWirePath());
    }

    final var impossible = new ArrayList<ConnectionData>();
    for (final var it = connects.iterator(); it.hasNext(); ) {
      final var conn = it.next();
      final var dest = conn.getLocation().translate(dx, dy);
      if (avoid.get(dest) != null) {
        var isInPath = false;
        for (final var wire : pathWires) {
          if (wire.contains(dest)) {
            isInPath = true;
            break;
          }
        }
        if (!isInPath) {
          it.remove();
          impossible.add(conn);
        }
      }
    }
    return impossible;
  }

  /**
   * Creates a list of the connections to make, sorted according to their location. If, for example,
   * we are moving an east-facing AND gate southeast, then we prefer to connect the inputs from the
   * top down to minimize the chances that the created wires will interfere with each other - but if
   * we are moving that gate northeast, we prefer to connect the inputs from the bottom up.
   */
  private static void sortConnects(List<ConnectionData> connects, final int dx, final int dy) {
    connects.sort((ac, bc) -> {
      final var a = ac.getLocation();
      final var b = bc.getLocation();
      final var abx = a.getX() - b.getX();
      final var aby = a.getY() - b.getY();
      return abx * dx + aby * dy;
    });
  }

  private static MoveResult tryList(
      MoveRequest req,
      MoveGesture gesture,
      List<ConnectionData> connects,
      int dx,
      int dy,
      Map<ConnectionData, Set<Location>> pathLocs,
      Map<ConnectionData, List<SearchNode>> initNodes,
      long stopTime) {
    final var avoid = gesture.getFixedAvoidanceMap().cloneMap();
    avoid.markAll(gesture.getSelected(), dx, dy);

    final var replacements = new ReplacementMap();
    final var unconnected = new ArrayList<ConnectionData>();
    var totalDistance = 0;
    for (final var conn : connects) {
      if (ConnectorThread.isOverrideRequested()) return null;
      if (System.currentTimeMillis() - stopTime > 0) {
        unconnected.add(conn);
        continue;
      }
      final var connNodes = initNodes.get(conn);
      final var connPathLocs = pathLocs.get(conn);
      final var node = findShortestPath(connNodes, connPathLocs, avoid);
      if (node != null) {
        // normal case - a path was found
        totalDistance += node.getDistance();
        final var path = convertToPath(node);
        processPath(path, conn, avoid, replacements, connPathLocs);
      } else if (ConnectorThread.isOverrideRequested()) {
        // search was aborted: return null to indicate this
        return null;
      } else {
        unconnected.add(conn);
      }
    }
    return new MoveResult(req, replacements, unconnected, totalDistance);
  }

}
