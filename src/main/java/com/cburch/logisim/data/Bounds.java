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

/**
 * Represents an immutable rectangular bounding box. This is analogous to the
 * {@link java.awt.Rectangle Rectangle} class, except that objects of this type are immutable.
 */
public class Bounds {

  /**
   * Returns a bounds object with the provided location and height.
   *
   * @param x The X coordinate of the object
   * @param y The Y coordinate of the object
   * @param wid The width of the object
   * @param ht The height of the object
   * @return A <code>Bounds</code> object with the provided fields.
   */
  public static Bounds create(int x, int y, int wid, int ht) {
    final var hashCode = 13 * (31 * (31 * x + y) + wid) + ht;
    Object cached = cache.get(hashCode);
    if (cached != null) {
      final var bds = (Bounds) cached;
      if (bds.x == x && bds.y == y && bds.wid == wid && bds.ht == ht) return bds;
    }
    final var ret = new Bounds(x, y, wid, ht);
    cache.put(hashCode, ret);
    return ret;
  }

  /**
   * Returns a bounds object from a <code>Rectangle</code> object.
   * @return A <code>Bounds</code> object with x, y, width and height of the provided rectangle.
   */
  public static Bounds create(java.awt.Rectangle rect) {
    return create(rect.x, rect.y, rect.width, rect.height);
  }

  /**
   * Returns a 1x1 bounds object with the given location.
   *
   * @return A 1x1 <code>Bounds</code> object with the X and Y from the provided location.
   */
  public static Bounds create(Location pt) {
    return create(pt.getX(), pt.getY(), 1, 1);
  }

  /**
   * A <code>Bounds</code> object with 0 as its X, Y, width and height.
   */
  public static final Bounds EMPTY_BOUNDS = new Bounds(0, 0, 0, 0);

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
  }

  /**
   * Determines the smallest rectangle that contains this rectangle and a given one.
   * If one of the two rectangles is empty, returns the nonempty one, if present.
   * If one of the two rectangles contains the other, returns the containing one.
   * If none of the two rectangles contain each other, returns
   * the smallest rectangle that contains both given rectangles.
   *
   * <p>
   * Example:
   * <pre>
   * <code>
   *
   *    +-------+-+
   *  /‾| this  | |
   * R  +-------+ |
   * e  |     +---+
   * s  |     | o |
   * u  |     | t |
   * u  |     | h |
   * l  |     | e |
   * t  |     | r |
   *  \_|     |   |
   *    +-----+---+
   * </code>
   * </pre>
   *
   * @param bd The other rectangle to find a container with
   * @return A bounds object with the least non-negative width and height that contains both
   *         <code>this</code> and <code>bd</code>
   */
  public Bounds add(Bounds bd) {
    if (this == EMPTY_BOUNDS) return bd;
    if (bd == EMPTY_BOUNDS) return this;
    final var retX = Math.min(bd.x, this.x);
    final var retY = Math.min(bd.y, this.y);
    final var retWidth = Math.max(bd.x + bd.wid, this.x + this.wid) - retX;
    final var retHeight = Math.max(bd.y + bd.ht, this.y + this.ht) - retY;
    if (retX == this.x && retY == this.y && retWidth == this.wid && retHeight == this.ht) {
      return this;
    } else if (retX == bd.x && retY == bd.y && retWidth == bd.wid && retHeight == bd.ht) {
      return bd;
    } else {
      return Bounds.create(retX, retY, retWidth, retHeight);
    }
  }

  /**
   * Determines the least rectangle that contains this and the given point.
   * If this rectangle is empty, returns a 1x1 rectangle around the given point.
   * Otherwise, returns a rectangle that contains both this and the provided point, with the least
   * possible width and height.
   *
   * @param x The x coordinate of the given point
   * @param y The x coordinate of the given point
   * @return A bounds object with the least dimensions that contains this and the given point.
   */
  public Bounds add(int x, int y) {
    if (this == EMPTY_BOUNDS) return Bounds.create(x, y, 1, 1);
    if (contains(x, y)) return this;

    var newX = this.x;
    var newWidth = this.wid;
    var newY = this.y;
    var newHeight = this.ht;
    if (x < this.x) {
      newX = x;
      newWidth = (this.x + this.wid) - x;
    } else if (x >= this.x + this.wid) {
      newX = this.x;
      newWidth = x - this.x + 1;
    }
    if (y < this.y) {
      newY = y;
      newHeight = (this.y + this.ht) - y;
    } else if (y >= this.y + this.ht) {
      newY = this.y;
      newHeight = y - this.y + 1;
    }
    return create(newX, newY, newWidth, newHeight);
  }

  /**
   * Determines the smallest rectangle that contains this rectangle and a given one.
   *
   * @param x The x coordinate of the given rectangle
   * @param y The y coordinate of the given rectangle
   * @param wid The width of the given rectangle
   * @param ht The height of the given rectangle
   * @return A bounds object with the least non-negative width and height that contains
   *         both this and the given rectangle.
   * @see Bounds#add(Bounds)
   */
  public Bounds add(int x, int y, int wid, int ht) {
    if (this == EMPTY_BOUNDS) return Bounds.create(x, y, wid, ht);
    final var retX = Math.min(x, this.x);
    final var retY = Math.min(y, this.y);
    final var retWidth = Math.max(x + wid, this.x + this.wid) - retX;
    final var retHeight = Math.max(y + ht, this.y + this.ht) - retY;
    if (retX == this.x && retY == this.y && retWidth == this.wid && retHeight == this.ht) {
      return this;
    } else {
      return Bounds.create(retX, retY, retWidth, retHeight);
    }
  }

  /**
   * Determines the least rectangle that contains this and the given point.
   * If this rectangle is empty, returns a 1x1 rectangle around the given point.
   * Otherwise, returns a rectangle that contains both this and the provided point, with the least
   * possible width and height.
   *
   * @param p The given point
   * @return A bounds object with the least dimensions that contains this and the given point.
   * @see Bounds#add(int, int)
   */
  public Bounds add(Location p) {
    return add(p.getX(), p.getY());
  }

  /**
   * Determines whether a given point is within a certain distance of
   * a border of this rectangle.
   * This method considers the right and bottom borders of this rectangle to be reduced by one
   * in x and y respectively.
   *
   * @param px The X coordinate of the point
   * @param py The Y coordinate of the point
   * @param fudge The maximum distance from the border the given point might be.
   * @return Whether the provided point is within <code>fudge</code> units from a border.
   */
  public boolean borderContains(int px, int py, int fudge) {
    final var x1 = x + wid - 1;
    final var y1 = y + ht - 1;
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

  /**
   * Determines whether a given point is within a certain distance of
   * a border of this rectangle.
   * This method considers the right and bottom borders of this rectangle to be reduced by one
   * in x and y respectively.
   *
   * @param p The point to check
   * @param fudge The maximum distance from the border the given point might be.
   * @return Whether the provided point is within <code>fudge</code> units from a border.
   */
  public boolean borderContains(Location p, int fudge) {
    return borderContains(p.getX(), p.getY(), fudge);
  }

  /**
   * Determines whether this rectangle contains the given one.
   *
   * @param bd The other rectangle to check
   * @return Whether the provided rectangle is contained by this one.
   */
  public boolean contains(Bounds bd) {
    return contains(bd.x, bd.y, bd.wid, bd.ht);
  }

  /**
   * Determines whether this rectangle contains a given point.
   *
   * @param px The X coordinate of the point
   * @param py The Y coordinate of the point
   * @return Whether this rectangle contains the given point.
   */
  public boolean contains(int px, int py) {
    return contains(px, py, 0);
  }

  /**
   * Determines whether this rectangle contains a given point, within a certain margin of error.
   * If the least distance between the point and this rectangle is smaller or equal
   * than the allowed error, returns true.
   *
   * @param px The X coordinate of the point
   * @param py The Y coordinate of the point
   * @param allowedError The maximum margin of error.
   * @return Whether this rectangle contains the given point.
   */
  public boolean contains(int px, int py, int allowedError) {
    return px >= x - allowedError
        && px < x + wid + allowedError
        && py >= y - allowedError
        && py < y + ht + allowedError;
  }

  /**
   * Determines whether this rectangle contains a given one.
   *
   * @param x The x coordinate of the given rectangle
   * @param y The y coordinate of the given rectangle
   * @param wid The width of the given rectangle
   * @param ht The height of the given rectangle
   * @return Whether the provided rectangle is contained by this one.
   */
  public boolean contains(int x, int y, int wid, int ht) {
    final var othX = (wid <= 0 ? x : x + wid - 1);
    final var othY = (ht <= 0 ? y : y + ht - 1);
    return contains(x, y) && contains(othX, othY);
  }

  /**
   * Determines whether this rectangle contains a given point.
   *
   * @param p The point to check
   * @return Whether this rectangle contains the given point.
   */
  public boolean contains(Location p) {
    return contains(p.getX(), p.getY(), 0);
  }

  /**
   * Determines whether this rectangle contains a given point, within a certain margin of error.
   * If the least distance between the point and this rectangle is smaller or equal
   * than the allowed error, returns true.
   *
   * @param p The point to check
   * @param allowedError The maximum margin of error.
   * @return Whether this rectangle contains the given point.
   */
  public boolean contains(Location p, int allowedError) {
    return contains(p.getX(), p.getY(), allowedError);
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof Bounds other)
           ? x == other.x && y == other.y && wid == other.wid && ht == other.ht
           : false;
  }

  /**
   * Expands this rectangle, increasing its proportions in all directions.
   *
   * @param d The distance to increase
   * @return A rectangle which equals this one, but <code>d</code> more units in the
   *         north, south, east and west direction. The returned object's original position may not
   *         equal this one's.
   */
  public Bounds expand(int d) { // d pixels in each direction
    if (this == EMPTY_BOUNDS) return this;
    if (d == 0) return this;
    return create(x - d, y - d, wid + 2 * d, ht + 2 * d);
  }

  /**
   * @return The X coordinate of the center of this rectangle.
   */
  public int getCenterX() {
    return (x + wid / 2);
  }

  /**
   * @return The Y coordinate of the center of this rectangle.
   */
  public int getCenterY() {
    return (y + ht / 2);
  }

  /**
   * @return The height of this rectangle.
   */
  public int getHeight() {
    return ht;
  }

  /**
   * @return The width of this rectangle.
   */
  public int getWidth() {
    return wid;
  }

  /**
   * @return The X coordinate of this rectangle.
   */
  public int getX() {
    return x;
  }

  /**
   * @return The Y coordinate of this rectangle.
   */
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

  /**
   * @return The rectangle that is the intersection between this rectangle and another one.
   *         If no such rectangle exists, <code>EMPTY_BOUNDS</code> is returned.
   */
  public Bounds intersect(Bounds other) {
    var x0 = this.x;
    var y0 = this.y;
    var x1 = x0 + this.wid;
    var y1 = y0 + this.ht;
    final var x2 = other.x;
    final var y2 = other.y;
    final var x3 = x2 + other.wid;
    final var y3 = y2 + other.ht;
    if (x2 > x0) x0 = x2;
    if (y2 > y0) y0 = y2;
    if (x3 < x1) x1 = x3;
    if (y3 < y1) y1 = y3;

    return (x1 < x0 || y1 < y0) ? EMPTY_BOUNDS : create(x0, y0, x1 - x0, y1 - y0);
  }

  /**
   *
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Rotation_(mathematics)">
   * mathematical rotation
   * </a>
   * of the points of this rectangle with (xc,yc) as the rotation center.
   * <p>
   * <img src="https://upload.wikimedia.org/wikipedia/commons/thumb/8/87/Rotation_illustration2.svg/672px-Rotation_illustration2.svg.png"
   * alt="Image of a Rotation of P (this) centered around O (xc, yc) with an angle of alpha"
   * width="200"
   * height="200"
   * />
   * <p>
   * The angle of the rotation is determined by the two directions <code>from</code> and
   * <code>to</code>, with it being the angle necessary to turn from the <code>from</code>
   * direction to the <code>to</code> direction.
   * For example, if one does a rotation with <code>from = Direction.EAST</code> and
   * <code>to = Direction.SOUTH</code>, then the rotation will be a clockwise 90º around (xc,yc).
   * Angle combinations are not unique, this could also be achieved with
   * <code>from = Direction.NORTH</code> and <code>to = Direction.EAST</code>.
   * All points from this rectangle are rotated according to this angle, around (xc,yc).
   *
   * @param from The base direction to draw the angle from
   * @param to The target direction to draw the angle from
   * @param xc The X coordinate of the center of the rotation
   * @param yc the Y coordinate of the center of the rotation.
   * @see Location#rotate
   * @return A rectangle whose 4 edge points result from the rotation of this' edge points around
   *         the point (xc,yc)
   */
  public Bounds rotate(Direction from, Direction to, int xc, int yc) {
    var degrees = to.toDegrees() - from.toDegrees();
    while (degrees >= 360) degrees -= 360;
    while (degrees < 0) degrees += 360;

    final var dx = x - xc;
    final var dy = y - yc;
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

  /**
   * Converts this <code>Bounds</code> object to a {@link java.awt.Rectangle} object.
   * @return A <code>Rectangle</code> object with this object's x, y, width and height.
   */
  public Rectangle toRectangle() {
    return new Rectangle(x, y, wid, ht);
  }

  @Override
  public String toString() {
    return "(" + x + "," + y + "): " + wid + "x" + ht;
  }

  /**
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Translation_(geometry)">
   * mathematical translation
   * </a>
   * of this rectangle's points according to the given x and y displacement.
   *
   * @param dx The x displacement to move this rectangle.
   * @param dy The y displacement to move this rectangle.
   * @return A rectangle that equals this one in width and height, but with its x and y positions
   *         changed by the <code>dx</code> and <code>dy</code> displacements.
   */
  public Bounds translate(int dx, int dy) {
    if (this == EMPTY_BOUNDS) return this;
    if (dx == 0 && dy == 0) return this;
    return create(x + dx, y + dy, wid, ht);
  }
}
