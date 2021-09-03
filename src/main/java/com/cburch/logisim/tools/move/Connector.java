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
import java.util.PriorityQueue;
import java.util.Set;

class Connector {
  static MoveResult computeWires(MoveRequest req) {
    MoveGesture gesture = req.getMoveGesture();
    int dx = req.getDeltaX();
    int dy = req.getDeltaY();
    ArrayList<ConnectionData> baseConnects;
    baseConnects = new ArrayList<>(gesture.getConnections());
    ArrayList<ConnectionData> impossible =
        pruneImpossible(baseConnects, gesture.getFixedAvoidanceMap(), dx, dy);

    AvoidanceMap selAvoid = AvoidanceMap.create(gesture.getSelected(), dx, dy);
    HashMap<ConnectionData, Set<Location>> pathLocs;
    pathLocs = new HashMap<>();
    HashMap<ConnectionData, List<SearchNode>> initNodes;
    initNodes = new HashMap<>();
    for (ConnectionData conn : baseConnects) {
      HashSet<Location> connLocs = new HashSet<>();
      ArrayList<SearchNode> connNodes = new ArrayList<>();
      processConnection(conn, dx, dy, connLocs, connNodes, selAvoid);
      pathLocs.put(conn, connLocs);
      initNodes.put(conn, connNodes);
    }

    MoveResult bestResult = null;
    int tries;
    switch (baseConnects.size()) {
      case 0:
        tries = 0;
        break;
      case 1:
        tries = 1;
        break;
      case 2:
        tries = 2;
        break;
      case 3:
        tries = 8;
        break;
      default:
        tries = MAX_ORDERING_TRIES;
    }
    long stopTime = System.currentTimeMillis() + MAX_SECONDS * 1000;
    for (int tryNum = 0; tryNum < tries && stopTime - System.currentTimeMillis() > 0; tryNum++) {
      if (ConnectorThread.isOverrideRequested()) {
        return null;
      }
      ArrayList<ConnectionData> connects;
      connects = new ArrayList<>(baseConnects);
      if (tryNum < 2) {
        sortConnects(connects, dx, dy);
        if (tryNum == 1) {
          Collections.reverse(connects);
        }
      } else {
        Collections.shuffle(connects);
      }

      MoveResult candidate = tryList(req, gesture, connects, dx, dy, pathLocs, initNodes, stopTime);
      if (candidate == null) {
        return null;
      } else if (bestResult == null) {
        bestResult = candidate;
      } else {
        int unsatisfied1 = bestResult.getUnsatisifiedConnections().size();
        int unsatisfied2 = candidate.getUnsatisifiedConnections().size();
        if (unsatisfied2 < unsatisfied1) {
          bestResult = candidate;
        } else if (unsatisfied2 == unsatisfied1) {
          int dist1 = bestResult.getTotalDistance();
          int dist2 = candidate.getTotalDistance();
          if (dist2 < dist1) {
            bestResult = candidate;
          }
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
    SearchNode next = last;
    SearchNode prev = last.getPrevious();
    ArrayList<Location> ret = new ArrayList<>();
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

  private static SearchNode findShortestPath(
      List<SearchNode> nodes, Set<Location> pathLocs, AvoidanceMap avoid) {
    PriorityQueue<SearchNode> q = new PriorityQueue<>(nodes);
    HashSet<SearchNode> visited = new HashSet<>();
    int iters = 0;
    while (!q.isEmpty() && iters < MAX_SEARCH_ITERATIONS) {
      iters++;
      SearchNode n = q.remove();
      if (iters % 64 == 0 && ConnectorThread.isOverrideRequested() || n == null) {
        return null;
      }
      if (n.isDestination()) {
        return n;
      }
      boolean added = visited.add(n);
      if (!added) {
        continue;
      }
      Location loc = n.getLocation();
      Direction dir = n.getDirection();
      int neighbors = 3;
      Object allowed = avoid.get(loc);
      if (allowed != null && n.isStart() && pathLocs.contains(loc)) {
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
        Direction oDir;
        switch (i) {
          case 0:
            oDir = dir;
            break;
          case 1:
            oDir = neighbors == 2 ? dir.reverse() : dir.getLeft();
            break;
          case 2:
            oDir = dir.getRight();
            break;
          default: // must be 3
            oDir = dir.reverse();
        }
        SearchNode o = n.next(oDir, allowed != null);
        if (o != null && !visited.contains(o)) {
          q.add(o);
        }
      }
    }
    return null;
  }

  private static void processConnection(
      ConnectionData conn,
      int dx,
      int dy,
      HashSet<Location> connLocs,
      ArrayList<SearchNode> connNodes,
      AvoidanceMap selAvoid) {
    Location cur = conn.getLocation();
    Location dest = cur.translate(dx, dy);
    if (selAvoid.get(cur) == null) {
      Direction preferred = conn.getDirection();
      if (preferred == null) {
        if (Math.abs(dx) > Math.abs(dy)) {
          preferred = dx > 0 ? Direction.EAST : Direction.WEST;
        } else {
          preferred = dy > 0 ? Direction.SOUTH : Direction.NORTH;
        }
      }

      connLocs.add(cur);
      connNodes.add(new SearchNode(conn, cur, preferred, dest));
    }

    for (Wire w : conn.getWirePath()) {
      for (Location loc : w) {
        if (selAvoid.get(loc) == null || loc.equals(dest)) {
          boolean added = connLocs.add(loc);
          if (added) {
            Direction dir = null;
            if (w.endsAt(loc)) {
              if (w.isVertical()) {
                int y0 = loc.getY();
                int y1 = w.getOtherEnd(loc).getY();
                dir = y0 < y1 ? Direction.NORTH : Direction.SOUTH;
              } else {
                int x0 = loc.getX();
                int x1 = w.getOtherEnd(loc).getX();
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
      ArrayList<Location> path,
      ConnectionData conn,
      AvoidanceMap avoid,
      ReplacementMap repl,
      Set<Location> unmarkable) {
    Iterator<Location> pathIt = path.iterator();
    Location loc0 = pathIt.next();
    if (!loc0.equals(conn.getLocation())) {
      Location pathLoc = conn.getWirePathStart();
      boolean found = loc0.equals(pathLoc);
      for (Wire w : conn.getWirePath()) {
        Location nextLoc = w.getOtherEnd(pathLoc);
        if (found) { // existing wire will be removed
          repl.remove(w);
          avoid.unmarkWire(w, nextLoc, unmarkable);
        } else if (w.contains(loc0)) { // wires after this will be
          // removed
          found = true;
          if (!loc0.equals(nextLoc)) {
            avoid.unmarkWire(w, nextLoc, unmarkable);
            Wire shortenedWire = Wire.create(pathLoc, loc0);
            repl.replace(w, shortenedWire);
            avoid.markWire(shortenedWire, 0, 0);
          }
        }
        pathLoc = nextLoc;
      }
    }
    while (pathIt.hasNext()) {
      Location loc1 = pathIt.next();
      Wire newWire = Wire.create(loc0, loc1);
      repl.add(newWire);
      avoid.markWire(newWire, 0, 0);
      loc0 = loc1;
    }
  }

  private static ArrayList<ConnectionData> pruneImpossible(
      ArrayList<ConnectionData> connects, AvoidanceMap avoid, int dx, int dy) {
    ArrayList<Wire> pathWires = new ArrayList<>();
    for (ConnectionData conn : connects) {
      pathWires.addAll(conn.getWirePath());
    }

    ArrayList<ConnectionData> impossible = new ArrayList<>();
    for (Iterator<ConnectionData> it = connects.iterator(); it.hasNext(); ) {
      ConnectionData conn = it.next();
      Location dest = conn.getLocation().translate(dx, dy);
      if (avoid.get(dest) != null) {
        boolean isInPath = false;
        for (Wire w : pathWires) {
          if (w.contains(dest)) {
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
  private static void sortConnects(ArrayList<ConnectionData> connects, final int dx, final int dy) {
    connects.sort((ac, bc) -> {
      Location a = ac.getLocation();
      Location b = bc.getLocation();
      int abx = a.getX() - b.getX();
      int aby = a.getY() - b.getY();
      return abx * dx + aby * dy;
    });
  }

  private static MoveResult tryList(
      MoveRequest req,
      MoveGesture gesture,
      ArrayList<ConnectionData> connects,
      int dx,
      int dy,
      HashMap<ConnectionData, Set<Location>> pathLocs,
      HashMap<ConnectionData, List<SearchNode>> initNodes,
      long stopTime) {
    AvoidanceMap avoid = gesture.getFixedAvoidanceMap().cloneMap();
    avoid.markAll(gesture.getSelected(), dx, dy);

    ReplacementMap replacements = new ReplacementMap();
    ArrayList<ConnectionData> unconnected = new ArrayList<>();
    int totalDistance = 0;
    for (ConnectionData conn : connects) {
      if (ConnectorThread.isOverrideRequested()) {
        return null;
      }
      if (System.currentTimeMillis() - stopTime > 0) {
        unconnected.add(conn);
        continue;
      }
      List<SearchNode> connNodes = initNodes.get(conn);
      Set<Location> connPathLocs = pathLocs.get(conn);
      SearchNode n = findShortestPath(connNodes, connPathLocs, avoid);
      if (n != null) { // normal case - a path was found
        totalDistance += n.getDistance();
        ArrayList<Location> path = convertToPath(n);
        processPath(path, conn, avoid, replacements, connPathLocs);
      } else if (ConnectorThread.isOverrideRequested()) {
        return null; // search was aborted: return null to indicate this
      } else {
        unconnected.add(conn);
      }
    }
    return new MoveResult(req, replacements, unconnected, totalDistance);
  }

  private static final int MAX_SECONDS = 10;

  private static final int MAX_ORDERING_TRIES = 10;

  private static final int MAX_SEARCH_ITERATIONS = 20000;

  static final String ALLOW_NEITHER = "neither";

  static final String ALLOW_VERTICAL = "vert";

  static final String ALLOW_HORIZONTAL = "horz";

  private Connector() {}
}
