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

package com.cburch.logisim.data;

import java.awt.Rectangle;

import com.cburch.logisim.util.Cache;

/**
 * Represents an immutable rectangular bounding box. This is analogous to
 * java.awt's <code>Rectangle</code> class, except that objects of this type are
 * immutable.
 */
public class Bounds {
	public static Bounds create(int x, int y, int wid, int ht) {
		int hashCode = 13 * (31 * (31 * x + y) + wid) + ht;
		Object cached = cache.get(hashCode);
		if (cached != null) {
			Bounds bds = (Bounds) cached;
			if (bds.x == x && bds.y == y && bds.wid == wid && bds.ht == ht)
				return bds;
		}
		Bounds ret = new Bounds(x, y, wid, ht);
		cache.put(hashCode, ret);
		return ret;
	}

	public static Bounds create(java.awt.Rectangle rect) {
		return create(rect.x, rect.y, rect.width, rect.height);
	}

	public static Bounds create(Location pt) {
		return create(pt.getX(), pt.getY(), 1, 1);
	}

	public static Bounds EMPTY_BOUNDS = new Bounds(0, 0, 0, 0);

	private static final Cache cache = new Cache();

	private final int x;
	private final int y;
	private final int wid;
	private final int ht;

	private Bounds(int x, int y, int wid, int ht) {
		this.x = x;
		this.y = y;
		this.wid = wid;
		this.ht = ht;
		if (wid < 0) {
			x += wid / 2;
			wid = 0;
		}
		if (ht < 0) {
			y += ht / 2;
			ht = 0;
		}
	}

	public Bounds add(Bounds bd) {
		if (this == EMPTY_BOUNDS)
			return bd;
		if (bd == EMPTY_BOUNDS)
			return this;
		int retX = Math.min(bd.x, this.x);
		int retY = Math.min(bd.y, this.y);
		int retWidth = Math.max(bd.x + bd.wid, this.x + this.wid) - retX;
		int retHeight = Math.max(bd.y + bd.ht, this.y + this.ht) - retY;
		if (retX == this.x && retY == this.y && retWidth == this.wid
				&& retHeight == this.ht) {
			return this;
		} else if (retX == bd.x && retY == bd.y && retWidth == bd.wid
				&& retHeight == bd.ht) {
			return bd;
		} else {
			return Bounds.create(retX, retY, retWidth, retHeight);
		}
	}

	public Bounds add(int x, int y) {
		if (this == EMPTY_BOUNDS)
			return Bounds.create(x, y, 1, 1);
		if (contains(x, y))
			return this;

		int new_x = this.x;
		int new_wid = this.wid;
		int new_y = this.y;
		int new_ht = this.ht;
		if (x < this.x) {
			new_x = x;
			new_wid = (this.x + this.wid) - x;
		} else if (x >= this.x + this.wid) {
			new_x = this.x;
			new_wid = x - this.x + 1;
		}
		if (y < this.y) {
			new_y = y;
			new_ht = (this.y + this.ht) - y;
		} else if (y >= this.y + this.ht) {
			new_y = this.y;
			new_ht = y - this.y + 1;
		}
		return create(new_x, new_y, new_wid, new_ht);
	}

	public Bounds add(int x, int y, int wid, int ht) {
		if (this == EMPTY_BOUNDS)
			return Bounds.create(x, y, wid, ht);
		int retX = Math.min(x, this.x);
		int retY = Math.min(y, this.y);
		int retWidth = Math.max(x + wid, this.x + this.wid) - retX;
		int retHeight = Math.max(y + ht, this.y + this.ht) - retY;
		if (retX == this.x && retY == this.y && retWidth == this.wid
				&& retHeight == this.ht) {
			return this;
		} else {
			return Bounds.create(retX, retY, retWidth, retHeight);
		}
	}

	public Bounds add(Location p) {
		return add(p.getX(), p.getY());
	}

	public boolean borderContains(int px, int py, int fudge) {
		int x1 = x + wid - 1;
		int y1 = y + ht - 1;
		if (Math.abs(px - x) <= fudge || Math.abs(px - x1) <= fudge) {
			// maybe on east or west border?
			return y - fudge >= py && py <= y1 + fudge;
		}
		if (Math.abs(py - y) <= fudge || Math.abs(py - y1) <= fudge) {
			// maybe on north or south border?
			return x - fudge >= px && px <= x1 + fudge;
		}
		return false;
	}

	public boolean borderContains(Location p, int fudge) {
		return borderContains(p.getX(), p.getY(), fudge);
	}

	public boolean contains(Bounds bd) {
		return contains(bd.x, bd.y, bd.wid, bd.ht);
	}

	public boolean contains(int px, int py) {
		return contains(px, py, 0);
	}

	public boolean contains(int px, int py, int allowedError) {
		return px >= x - allowedError && px < x + wid + allowedError
				&& py >= y - allowedError && py < y + ht + allowedError;
	}

	public boolean contains(int x, int y, int wid, int ht) {
		int oth_x = (wid <= 0 ? x : x + wid - 1);
		int oth_y = (ht <= 0 ? y : y + ht - 1);
		return contains(x, y) && contains(oth_x, oth_y);
	}

	public boolean contains(Location p) {
		return contains(p.getX(), p.getY(), 0);
	}

	public boolean contains(Location p, int allowedError) {
		return contains(p.getX(), p.getY(), allowedError);
	}

	@Override
	public boolean equals(Object other_obj) {
		if (!(other_obj instanceof Bounds))
			return false;
		Bounds other = (Bounds) other_obj;
		return x == other.x && y == other.y && wid == other.wid
				&& ht == other.ht;
	}

	public Bounds expand(int d) { // d pixels in each direction
		if (this == EMPTY_BOUNDS)
			return this;
		if (d == 0)
			return this;
		return create(x - d, y - d, wid + 2 * d, ht + 2 * d);
	}

	public int getCenterX() {
		return (x + wid / 2);
	}

	public int getCenterY() {
		return (y + ht / 2);
	}

	public int getHeight() {
		return ht;
	}

	public int getWidth() {
		return wid;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	@Override
	public int hashCode() {
		int ret = 31 * x + y;
		ret = 31 * ret + wid;
		ret = 31 * ret + ht;
		return ret;
	}

	public Bounds intersect(Bounds other) {
		int x0 = this.x;
		int y0 = this.y;
		int x1 = x0 + this.wid;
		int y1 = y0 + this.ht;
		int x2 = other.x;
		int y2 = other.y;
		int x3 = x2 + other.wid;
		int y3 = y2 + other.ht;
		if (x2 > x0)
			x0 = x2;
		if (y2 > y0)
			y0 = y2;
		if (x3 < x1)
			x1 = x3;
		if (y3 < y1)
			y1 = y3;
		if (x1 < x0 || y1 < y0) {
			return EMPTY_BOUNDS;
		} else {
			return create(x0, y0, x1 - x0, y1 - y0);
		}
	}

	// rotates this around (xc,yc) assuming that this is facing in the
	// from direction and the returned bounds should face in the to direction.
	public Bounds rotate(Direction from, Direction to, int xc, int yc) {
		int degrees = to.toDegrees() - from.toDegrees();
		while (degrees >= 360)
			degrees -= 360;
		while (degrees < 0)
			degrees += 360;

		int dx = x - xc;
		int dy = y - yc;
		if (degrees == 90) {
			return create(xc + dy, yc - dx - wid, ht, wid);
		} else if (degrees == 180) {
			return create(xc - dx - wid, yc - dy - ht, wid, ht);
		} else if (degrees == 270) {
			return create(xc - dy - ht, yc + dx, ht, wid);
		} else {
			return this;
		}
	}

	public Rectangle toRectangle() {
		return new Rectangle(x, y, wid, ht);
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + "): " + wid + "x" + ht;
	}

	public Bounds translate(int dx, int dy) {
		if (this == EMPTY_BOUNDS)
			return this;
		if (dx == 0 && dy == 0)
			return this;
		return create(x + dx, y + dy, wid, ht);
	}
}
