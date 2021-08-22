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

package com.cburch.logisim.tools.move;

import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import lombok.val;

class Connector {
  static MoveResult computeWires(MoveRequest req) {
    val gesture = req.getMoveGesture();
    val dx = req.getDeltaX();
    val dy = req.getDeltaY();
    val baseConnects = new ArrayList<ConnectionData>(gesture.getConnections());
    val impossible = pruneImpossible(baseConnects, gesture.getFixedAvoidanceMap(), dx, dy);

    val selAvoid = AvoidanceMap.create(gesture.getSelected(), dx, dy);
    val pathLocs = new HashMap<ConnectionData, Set<Location>>();
    val initNodes = new HashMap<ConnectionData, List<SearchNode>>();
    for (val conn : baseConnects) {
      val connLocs = new HashSet<Location>();
      val connNodes = new ArrayList<SearchNode>();
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
    val stopTime = System.currentTimeMillis() + MAX_SECONDS * 1000;
    for (var tryNum = 0; tryNum < tries && stopTime - System.currentTimeMillis() > 0; tryNum++) {
      if (ConnectorThread.isOverrideRequested()) {
        return null;
      }
      val connects = new ArrayList<ConnectionData>(baseConnects);
      if (tryNum < 2) {
        sortConnects(connects, dx, dy);
        if (tryNum == 1) {
          Collections.reverse(connects);
        }
      } else {
        Collections.shuffle(connects);
      }

      val candidate = tryList(req, gesture, connects, dx, dy, pathLocs, initNodes, stopTime);
      if (candidate == null) {
        return null;
      } else if (bestResult == null) {
        bestResult = candidate;
      } else {
        val unsatisfied1 = bestResult.getUnsatisfiedConnections().size();
        val unsatisfied2 = candidate.getUnsatisfiedConnections().size();
        if (unsatisfied2 < unsatisfied1) {
          bestResult = candidate;
        } else if (unsatisfied2 == unsatisfied1) {
          val dist1 = bestResult.getTotalDistance();
          val dist2 = candidate.getTotalDistance();
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
    var next = last;
    var prev = last.getPrevious();
    val ret = new ArrayList<Location>();
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
    val q = new PriorityQueue<SearchNode>(nodes);
    val visited = new HashSet<SearchNode>();
    var iters = 0;
    while (!q.isEmpty() && iters < MAX_SEARCH_ITERATIONS) {
      iters++;
      val n = q.remove();
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
      val loc = n.getLocation();
      var dir = n.getDirection();
      var neighbors = 3;
      var allowed = avoid.get(loc);
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
        val o = n.next(oDir, allowed != null);
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
    val cur = conn.getLocation();
    val dest = cur.translate(dx, dy);
    if (selAvoid.get(cur) == null) {
      var preferred = conn.getDirection();
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

    for (val wire : conn.getWirePath()) {
      for (val loc : wire) {
        if (selAvoid.get(loc) == null || loc.equals(dest)) {
          var added = connLocs.add(loc);
          if (added) {
            Direction dir = null;
            if (wire.endsAt(loc)) {
              if (wire.isVertical()) {
                val y0 = loc.getY();
                val y1 = wire.getOtherEnd(loc).getY();
                dir = y0 < y1 ? Direction.NORTH : Direction.SOUTH;
              } else {
                val x0 = loc.getX();
                val x1 = wire.getOtherEnd(loc).getX();
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
    val pathIt = path.iterator();
    var loc0 = pathIt.next();
    if (!loc0.equals(conn.getLocation())) {
      var pathLoc = conn.getWirePathStart();
      var found = loc0.equals(pathLoc);
      for (val wire : conn.getWirePath()) {
        val nextLoc = wire.getOtherEnd(pathLoc);
        if (found) { // existing wire will be removed
          repl.remove(wire);
          avoid.unmarkWire(wire, nextLoc, unmarkable);
        } else if (wire.contains(loc0)) { // wires after this will be
          // removed
          found = true;
          if (!loc0.equals(nextLoc)) {
            avoid.unmarkWire(wire, nextLoc, unmarkable);
            val shortenedWire = Wire.create(pathLoc, loc0);
            repl.replace(wire, shortenedWire);
            avoid.markWire(shortenedWire, 0, 0);
          }
        }
        pathLoc = nextLoc;
      }
    }
    while (pathIt.hasNext()) {
      val loc1 = pathIt.next();
      val newWire = Wire.create(loc0, loc1);
      repl.add(newWire);
      avoid.markWire(newWire, 0, 0);
      loc0 = loc1;
    }
  }

  private static ArrayList<ConnectionData> pruneImpossible(ArrayList<ConnectionData> connects, AvoidanceMap avoid, int dx, int dy) {
    val pathWires = new ArrayList<Wire>();
    for (val conn : connects) {
      pathWires.addAll(conn.getWirePath());
    }

    val impossible = new ArrayList<ConnectionData>();
    for (val it = connects.iterator(); it.hasNext(); ) {
      val conn = it.next();
      val dest = conn.getLocation().translate(dx, dy);
      if (avoid.get(dest) != null) {
        var isInPath = false;
        for (val wire : pathWires) {
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
  private static void sortConnects(ArrayList<ConnectionData> connects, final int dx, final int dy) {
    connects.sort((ac, bc) -> {
      val a = ac.getLocation();
      val b = bc.getLocation();
      val abx = a.getX() - b.getX();
      val aby = a.getY() - b.getY();
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
    val avoid = gesture.getFixedAvoidanceMap().cloneMap();
    avoid.markAll(gesture.getSelected(), dx, dy);

    val replacements = new ReplacementMap();
    val unconnected = new ArrayList<ConnectionData>();
    var totalDistance = 0;
    for (val conn : connects) {
      if (ConnectorThread.isOverrideRequested()) {
        return null;
      }
      if (System.currentTimeMillis() - stopTime > 0) {
        unconnected.add(conn);
        continue;
      }
      val connNodes = initNodes.get(conn);
      val connPathLocs = pathLocs.get(conn);
      val n = findShortestPath(connNodes, connPathLocs, avoid);
      if (n != null) { // normal case - a path was found
        totalDistance += n.getDistance();
        val path = convertToPath(n);
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
