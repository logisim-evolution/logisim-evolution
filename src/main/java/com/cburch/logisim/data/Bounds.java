/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.util.Cache;
import java.awt.Rectangle;
import lombok.Getter;

/**
 * Represents an immutable rectangular bounding box. This is analogous to java.awt's <code>Rectangle
 * </code> class, except that objects of this type are immutable.
 */
public class Bounds {
  public static Bounds create(int x, int y, int wid, int ht) {
    final var hashCode = 13 * (31 * (31 * x + y) + wid) + ht;
    Object cached = cache.get(hashCode);
    if (cached != null) {
      final var bds = (Bounds) cached;
      if (bds.x == x && bds.y == y && bds.width == wid && bds.height == ht) return bds;
    }
    final var ret = new Bounds(x, y, wid, ht);
    cache.put(hashCode, ret);
    return ret;
  }

  public static Bounds create(java.awt.Rectangle rect) {
    return create(rect.x, rect.y, rect.width, rect.height);
  }

  public static Bounds create(Location pt) {
    return create(pt.getX(), pt.getY(), 1, 1);
  }

  public static final Bounds EMPTY_BOUNDS = new Bounds(0, 0, 0, 0);

  private static final Cache cache = new Cache();

  @Getter private final int x;
  @Getter private final int y;
  @Getter private final int width;
  @Getter private final int height;

  private Bounds(int x, int y, int width, int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    if (width < 0) {
      x += width / 2;
      width = 0;
    }
    if (height < 0) {
      y += height / 2;
      height = 0;
    }
  }

  public Bounds add(Bounds bd) {
    if (this == EMPTY_BOUNDS) return bd;
    if (bd == EMPTY_BOUNDS) return this;
    final var retX = Math.min(bd.x, this.x);
    final var retY = Math.min(bd.y, this.y);
    final var retWidth = Math.max(bd.x + bd.width, this.x + this.width) - retX;
    final var retHeight = Math.max(bd.y + bd.height, this.y + this.height) - retY;
    if (retX == this.x && retY == this.y && retWidth == this.width && retHeight == this.height) {
      return this;
    } else if (retX == bd.x && retY == bd.y && retWidth == bd.width && retHeight == bd.height) {
      return bd;
    } else {
      return Bounds.create(retX, retY, retWidth, retHeight);
    }
  }

  public Bounds add(int x, int y) {
    if (this == EMPTY_BOUNDS) return Bounds.create(x, y, 1, 1);
    if (contains(x, y)) return this;

    var newX = this.x;
    var newWidth = this.width;
    var newY = this.y;
    var newHeight = this.height;
    if (x < this.x) {
      newX = x;
      newWidth = (this.x + this.width) - x;
    } else if (x >= this.x + this.width) {
      newX = this.x;
      newWidth = x - this.x + 1;
    }
    if (y < this.y) {
      newY = y;
      newHeight = (this.y + this.height) - y;
    } else if (y >= this.y + this.height) {
      newY = this.y;
      newHeight = y - this.y + 1;
    }
    return create(newX, newY, newWidth, newHeight);
  }

  public Bounds add(int x, int y, int wid, int ht) {
    if (this == EMPTY_BOUNDS) return Bounds.create(x, y, wid, ht);
    final var retX = Math.min(x, this.x);
    final var retY = Math.min(y, this.y);
    final var retWidth = Math.max(x + wid, this.x + this.width) - retX;
    final var retHeight = Math.max(y + ht, this.y + this.height) - retY;
    if (retX == this.x && retY == this.y && retWidth == this.width && retHeight == this.height) {
      return this;
    } else {
      return Bounds.create(retX, retY, retWidth, retHeight);
    }
  }

  public Bounds add(Location p) {
    return add(p.getX(), p.getY());
  }

  public boolean borderContains(int px, int py, int fudge) {
    final var x1 = x + width - 1;
    final var y1 = y + height - 1;
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
    return contains(bd.x, bd.y, bd.width, bd.height);
  }

  public boolean contains(int px, int py) {
    return contains(px, py, 0);
  }

  public boolean contains(int px, int py, int allowedError) {
    return px >= x - allowedError
        && px < x + width + allowedError
        && py >= y - allowedError
        && py < y + height + allowedError;
  }

  public boolean contains(int x, int y, int wid, int ht) {
    final var othX = (wid <= 0 ? x : x + wid - 1);
    final var othY = (ht <= 0 ? y : y + ht - 1);
    return contains(x, y) && contains(othX, othY);
  }

  public boolean contains(Location p) {
    return contains(p.getX(), p.getY(), 0);
  }

  public boolean contains(Location p, int allowedError) {
    return contains(p.getX(), p.getY(), allowedError);
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof Bounds other)
           ? x == other.x && y == other.y && width == other.width && height == other.height
           : false;
  }

  public Bounds expand(int d) { // d pixels in each direction
    if (this == EMPTY_BOUNDS) return this;
    if (d == 0) return this;
    return create(x - d, y - d, width + 2 * d, height + 2 * d);
  }

  public int getCenterX() {
    return (x + width / 2);
  }

  public int getCenterY() {
    return (y + height / 2);
  }

  @Override
  public int hashCode() {
    int ret = 31 * x + y;
    ret = 31 * ret + width;
    ret = 31 * ret + height;
    return ret;
  }

  public Bounds intersect(Bounds other) {
    var x0 = this.x;
    var y0 = this.y;
    var x1 = x0 + this.width;
    var y1 = y0 + this.height;
    final var x2 = other.x;
    final var y2 = other.y;
    final var x3 = x2 + other.width;
    final var y3 = y2 + other.height;
    if (x2 > x0) x0 = x2;
    if (y2 > y0) y0 = y2;
    if (x3 < x1) x1 = x3;
    if (y3 < y1) y1 = y3;

    return (x1 < x0 || y1 < y0) ? EMPTY_BOUNDS : create(x0, y0, x1 - x0, y1 - y0);
  }

  // rotates this around (xc,yc) assuming that this is facing in the
  // from direction and the returned bounds should face in the to direction.
  public Bounds rotate(Direction from, Direction to, int xc, int yc) {
    var degrees = to.toDegrees() - from.toDegrees();
    while (degrees >= 360) degrees -= 360;
    while (degrees < 0) degrees += 360;

    final var dx = x - xc;
    final var dy = y - yc;
    if (degrees == 90) {
      return create(xc + dy, yc - dx - width, height, width);
    } else if (degrees == 180) {
      return create(xc - dx - width, yc - dy - height, width, height);
    } else if (degrees == 270) {
      return create(xc - dy - height, yc + dx, height, width);
    } else {
      return this;
    }
  }

  public Rectangle toRectangle() {
    return new Rectangle(x, y, width, height);
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + "): " + width + "x" + height;
  }

  public Bounds translate(int dx, int dy) {
    if (this == EMPTY_BOUNDS) return this;
    if (dx == 0 && dy == 0) return this;
    return create(x + dx, y + dy, width, height);
  }
}
