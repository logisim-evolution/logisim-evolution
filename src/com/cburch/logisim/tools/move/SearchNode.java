/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.tools.move;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;

class SearchNode implements Comparable<SearchNode> {

	final static Logger logger = LoggerFactory.getLogger(SearchNode.class);

	private static final int CROSSING_PENALTY = 20;
	private static final int TURN_PENALTY = 50;

	private final Location loc;
	private final Direction dir;
	private ConnectionData conn;
	private final Location dest;
	private int dist;
	private int heur;
	private boolean extendsWire;
	private SearchNode prev;

	public SearchNode(ConnectionData conn, Location src, Direction srcDir,
			Location dst) {
		this(src, srcDir, conn, dst, 0, srcDir != null, null);
	}

	private SearchNode(Location loc, Direction dir, ConnectionData conn,
			Location dest, int dist, boolean extendsWire, SearchNode prev) {
		this.loc = loc;
		this.dir = dir;
		this.conn = conn;
		this.dest = dest;
		this.dist = dist;
		this.heur = dist + this.getHeuristic();
		this.extendsWire = extendsWire;
		this.prev = prev;
	}

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
		if (other instanceof SearchNode) {
			SearchNode o = (SearchNode) other;

			return (this.loc.equals(o.loc)
					&& (this.dir == null ? o.dir == null
							: (o.dir == null ? false : this.dir.equals(o.dir))) && this.dest
						.equals(o.dest));

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
				if (dx > 0)
					ret = dx / 10 * 9 + Math.abs(dy);
			} else if (curDir == Direction.WEST) {
				if (dx < 0)
					ret = -dx / 10 * 9 + Math.abs(dy);
			} else if (curDir == Direction.SOUTH) {
				if (dy > 0)
					ret = Math.abs(dx) + dy / 10 * 9;
			} else if (curDir == Direction.NORTH) {
				if (dy < 0)
					ret = Math.abs(dx) - dy / 10 * 9;
			}
		}
		if (ret < 0) {
			ret = Math.abs(dx) + Math.abs(dy);
		}
		boolean penalizeDoubleTurn = false;
		if (curDir == Direction.EAST) {
			penalizeDoubleTurn = dx < 0;
		} else if (curDir == Direction.WEST) {
			penalizeDoubleTurn = dx > 0;
		} else if (curDir == Direction.NORTH) {
			penalizeDoubleTurn = dy > 0;
		} else if (curDir == Direction.SOUTH) {
			penalizeDoubleTurn = dy < 0;
		} else if (curDir == null) {
			if (dx != 0 || dy != 0)
				ret += TURN_PENALTY;
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
		int newDist = dist;
		Direction connDir = conn.getDirection();
		Location nextLoc = loc.translate(moveDir, 10);
		boolean exWire = extendsWire && moveDir == connDir;
		if (exWire) {
			newDist += 9;
		} else {
			newDist += 10;
		}
		if (crossing)
			newDist += CROSSING_PENALTY;
		if (moveDir != dir)
			newDist += TURN_PENALTY;
		if (nextLoc.getX() < 0 || nextLoc.getY() < 0) {
			return null;
		} else {
			return new SearchNode(nextLoc, moveDir, conn, dest, newDist,
					exWire, this);
		}
	}

	@Override
	public String toString() {
		return loc + "/" + (dir == null ? "null" : dir.toString())
				+ (extendsWire ? "+" : "-") + "/" + dest + ":" + dist + "+"
				+ (heur - dist);
	}
}
