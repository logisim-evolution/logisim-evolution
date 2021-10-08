/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;

class SearchNode implements Comparable<SearchNode> {

  private static final int CROSSING_PENALTY = 20;
  private static final int TURN_PENALTY = 50;

  private final Location loc;
  private final Direction dir;
  private final ConnectionData conn;
  private final Location dest;
  private final int dist;
  private final int heur;
  private final boolean extendsWire;
  private final SearchNode prev;

  public SearchNode(ConnectionData conn, Location src, Direction srcDir, Location dst) {
    this(src, srcDir, conn, dst, 0, srcDir != null, null);
  }

  private SearchNode(
      Location loc,
      Direction dir,
      ConnectionData conn,
      Location dest,
      int dist,
      boolean extendsWire,
      SearchNode prev) {
    this.loc = loc;
    this.dir = dir;
    this.conn = conn;
    this.dest = dest;
    this.dist = dist;
    this.heur = dist + this.getHeuristic();
    this.extendsWire = extendsWire;
    this.prev = prev;
  }

  @Override
  public int compareTo(SearchNode o) {
    int ret = this.heur - o.heur;

    if (ret == 0) {
      return this.hashCode() - o.hashCode();
    } else {
      return ret;
    }
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SearchNode o) {
      return (this.loc.equals(o.loc)
          && (this.dir == null ? o.dir == null : (o.dir != null && this.dir.equals(o.dir)))
          && this.dest.equals(o.dest));

      /*
       * // This code causes a null pointer exception whenever this.dir is
       * not // null but o.dir is null! return (this.loc.equals(o.loc) &&
       * (this.dir == null ? o.dir == null : this.dir.equals(o.dir)) &&
       * this.dest.equals(o.dest));
       */
    } else {
      return false;
    }
  }

  public ConnectionData getConnection() {
    return conn;
  }

  public Location getDestination() {
    return dest;
  }

  public Direction getDirection() {
    return dir;
  }

  public int getDistance() {
    return dist;
  }

  private int getHeuristic() {
    Location cur = loc;
    Location dst = dest;
    Direction curDir = dir;
    int dx = dst.getX() - cur.getX();
    int dy = dst.getY() - cur.getY();
    int ret = -1;
    if (extendsWire) {
      ret = -1;
      if (curDir == Direction.EAST) {
        if (dx > 0) ret = dx / 10 * 9 + Math.abs(dy);
      } else if (curDir == Direction.WEST) {
        if (dx < 0) ret = -dx / 10 * 9 + Math.abs(dy);
      } else if (curDir == Direction.SOUTH) {
        if (dy > 0) ret = Math.abs(dx) + dy / 10 * 9;
      } else if (curDir == Direction.NORTH) {
        if (dy < 0) ret = Math.abs(dx) - dy / 10 * 9;
      }
    }
    if (ret < 0) {
      ret = Math.abs(dx) + Math.abs(dy);
    }
    var penalizeDoubleTurn = false;
    if (curDir == Direction.EAST) {
      penalizeDoubleTurn = dx < 0;
    } else if (curDir == Direction.WEST) {
      penalizeDoubleTurn = dx > 0;
    } else if (curDir == Direction.NORTH) {
      penalizeDoubleTurn = dy > 0;
    } else if (curDir == Direction.SOUTH) {
      penalizeDoubleTurn = dy < 0;
    } else if (curDir == null) {
      if (dx != 0 || dy != 0) ret += TURN_PENALTY;
    }
    if (penalizeDoubleTurn) {
      ret += 2 * TURN_PENALTY;
    } else if (dx != 0 && dy != 0) {
      ret += TURN_PENALTY;
    }
    return ret;
  }

  public int getHeuristicValue() {
    return heur;
  }

  public Location getLocation() {
    return loc;
  }

  public SearchNode getPrevious() {
    return prev;
  }

  @Override
  public int hashCode() {
    int dirHash = dir == null ? 0 : dir.hashCode();
    return ((loc.hashCode() * 31) + dirHash) * 31 + dest.hashCode();
  }

  public boolean isDestination() {
    return dest.equals(loc);
  }

  public boolean isExtendingWire() {
    return extendsWire;
  }

  public boolean isStart() {
    return prev == null;
  }

  public SearchNode next(Direction moveDir, boolean crossing) {
    var newDist = dist;
    final var connDir = conn.getDirection();
    final var nextLoc = loc.translate(moveDir, 10);
    final var exWire = extendsWire && moveDir == connDir;
    newDist += (exWire) ? 9 : 10;
    if (crossing) newDist += CROSSING_PENALTY;
    if (moveDir != dir) newDist += TURN_PENALTY;
    return (nextLoc.getX() < 0 || nextLoc.getY() < 0)
      ? null
      : new SearchNode(nextLoc, moveDir, conn, dest, newDist, exWire, this);
  }

  @Override
  public String toString() {
    return loc
        + "/"
        + (dir == null ? "null" : dir.toString())
        + (extendsWire ? "+" : "-")
        + "/"
        + dest
        + ":"
        + dist
        + "+"
        + (heur - dist);
  }
}
