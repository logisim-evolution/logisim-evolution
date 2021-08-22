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

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import lombok.Getter;
import lombok.val;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SearchNode implements Comparable<SearchNode> {

  static final Logger logger = LoggerFactory.getLogger(SearchNode.class);

  private static final int CROSSING_PENALTY = 20;
  private static final int TURN_PENALTY = 50;

  @Getter private final Location location;
  @Getter private final Direction direction;
  @Getter private final ConnectionData connection;
  @Getter private final Location destination;
  @Getter private final int distance;
  @Getter private final int heuristicValue;
  @Getter private final boolean exendingWire;
  @Getter private final SearchNode previous;

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
    this.location = loc;
    this.direction = dir;
    this.connection = conn;
    this.destination = dest;
    this.distance = dist;
    this.heuristicValue = dist + this.getHeuristic();
    this.exendingWire = extendsWire;
    this.previous = prev;
  }

  @Override
  public int compareTo(SearchNode o) {
    val ret = this.heuristicValue - o.heuristicValue;
    return (ret == 0) ? this.hashCode() - o.hashCode() : ret;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof SearchNode) {
      val o = (SearchNode) other;

      return (this.location.equals(o.location)
          && (this.direction == null ? o.direction == null : (o.direction != null && this.direction.equals(o.direction)))
          && this.destination.equals(o.destination));

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

  private int getHeuristic() {
    val cur = location;
    val dst = destination;
    val curDir = direction;
    val dx = dst.getX() - cur.getX();
    val dy = dst.getY() - cur.getY();
    var ret = -1;
    if (exendingWire) {
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

  @Override
  public int hashCode() {
    val dirHash = direction == null ? 0 : direction.hashCode();
    return ((location.hashCode() * 31) + dirHash) * 31 + destination.hashCode();
  }

  public boolean isDestination() {
    return destination.equals(location);
  }

  public boolean isStart() {
    return previous == null;
  }

  public SearchNode next(Direction moveDir, boolean crossing) {
    var newDist = distance;
    val connDir = connection.getDirection();
    val nextLoc = location.translate(moveDir, 10);
    val exWire = exendingWire && moveDir == connDir;
    if (exWire) {
      newDist += 9;
    } else {
      newDist += 10;
    }
    if (crossing) newDist += CROSSING_PENALTY;
    if (moveDir != direction) newDist += TURN_PENALTY;
    return (nextLoc.getX() < 0 || nextLoc.getY() < 0)
        ? null
        : new SearchNode(nextLoc, moveDir, connection, destination, newDist, exWire, this);
  }

  @Override
  public String toString() {
    return location
        + "/"
        + (direction == null ? "null" : direction.toString())
        + (exendingWire ? "+" : "-")
        + "/"
        + destination
        + ":"
        + distance
        + "+"
        + (heuristicValue - distance);
  }
}
