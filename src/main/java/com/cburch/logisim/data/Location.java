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
import java.util.Comparator;
import java.util.List;

/**
 * Represents an immutable rectangular bounding box. This is analogous to java.awt's <code>Point
 * </code> class, except that objects of this type are immutable.
 * <p>
 * When a location object is created, one may specify whether the location should be
 * truncated to the nearest location whose components are multiple of 5, as a
 * <code>hasToSnap</code> parameter. When a location is derived from another
 * with operations such as {@link Location#translate translate} or {@link Location#rotate rotate},
 * the resulting location
 * inherits the <code>hasToSnap</code> attribute from the original location.
 * <p>
 *  Note that whether a location has to snap or not does not affect comparisons such as <code>.equals</code>
 *  and <code>.hashCode</code> in any way other than the fact they may adjust the X and Y components
 *  of locations.
 */
public class Location implements Comparable<Location> {

  /**
   * Creates a location object with the provided attributes.
   * @param x The <code>x</code> coordinate of the returned Location
   * @param y The <code>y</code> coordinate of the returned Location
   * @param hasToSnap Whether the x and y coordinates should be snapped to
   *                  the nearest multiple of 5.
   * @return The newly created Location object.
   */
  public static Location create(int x, int y, boolean hasToSnap) {
    // we round to half-grid base
    final var xRounded = hasToSnap ? Math.round(x / 5) * 5 : x;
    final var yRounded = hasToSnap ? Math.round(y / 5) * 5 : y;
    final var hashCode = 31 * xRounded + yRounded;
    final var ret = cache.get(hashCode);
    if (ret != null) {
      final var loc = (Location) ret;
      if (loc.x == xRounded && loc.y == yRounded) return loc;
    }
    final var loc = new Location(hashCode, xRounded, yRounded, hasToSnap);
    cache.put(hashCode, loc);
    return loc;
  }

  // TODO: The math.round in this method is unnecessary/incorrect
  //  as x / 5 is an integer, Math.round(x / 5) is also an integer.
  //  should I remove it? or replace it with Math.round((double) x / 5)

  /**
   * Constructs a Location from the provided coordinate string.
   * The string is expected to be of the form "(x,y)", "(x y)", "x, y" or "x y"
   * where <code>x</code> and <code>y</code> represent the parsing rules implemented by
   * Integer.parseInt.
   *
   * @param value The string to parse
   * @return The newly created location object.
   */
  public static Location parse(String value) {
    final var base = value;

    value = value.trim();
    if (value.charAt(0) == '(') {
      final var len = value.length();
      if (value.charAt(len - 1) != ')') {
        throw new NumberFormatException("invalid point '" + base + "'");
      }
      value = value.substring(1, len - 1);
    }
    value = value.trim();
    var comma = value.indexOf(',');
    if (comma < 0) {
      comma = value.indexOf(' ');
      if (comma < 0) {
        throw new NumberFormatException("invalid point '" + base + "'");
      }
    }
    final var x = Integer.parseInt(value.substring(0, comma).trim());
    final var y = Integer.parseInt(value.substring(comma + 1).trim());
    return Location.create(x, y, true);
  }

  private static final Cache cache = new Cache();
  private final int hashCode;
  private final int x;
  private final int y;
  private final boolean hasToSnap;

  private Location(int hashCode, int x, int y, boolean hasToSnap) {
    this.hashCode = hashCode;
    this.hasToSnap = hasToSnap;
    this.x = x;
    this.y = y;
  }

  @Override
  public int compareTo(Location other) {
    return (this.x != other.x) ? this.x - other.x : this.y - other.y;
  }

  @Override
  public boolean equals(Object otherObj) {
    return (otherObj instanceof Location other)
           ? this.x == other.x && this.y == other.y
           : false;
  }

  /**
   * The X coordinate of this location.
   *
   * @return The X coordinate of this location.
   */
  public int getX() {
    return x;
  }

  /**
   * The Y coordinate of this location.
   *
   * @return The Y coordinate of this location.
   */
  public int getY() {
    return y;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  /**
   * Determines the
   * <a href="https://simple.wikipedia.org/wiki/Manhattan_distance"/>
   * manhattan distance from this location and a given location.
   *
   * @param x The x coordinate of the location.
   * @param y The y coordinate of the location.
   * @return The manhattan distance of the two locations.
   */
  public int manhattanDistanceTo(int x, int y) {
    return Math.abs(x - this.x) + Math.abs(y - this.y);
  }

  /**
   * Determines the
   * <a href="https://simple.wikipedia.org/wiki/Manhattan_distance">
   * manhattan distance
   * </a>
   * from this location and a given location.
   *
   * @param o The location to find the manhattan distance with
   * @return The manhattan distance of the two locations.
   */
  public int manhattanDistanceTo(Location o) {
    return Math.abs(o.x - this.x) + Math.abs(o.y - this.y);
  }

  // rotates this around (xc,yc) assuming that this is facing in the
  // from direction and the returned bounds should face in the to direction.

  /**
   *
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Rotation_(mathematics)">
   * mathematical rotation
   * </a>
   * of this point with (xc,yc) as the rotation center.
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
   * <p>
   * The following is an example of the behaviour expressed by
   * <code>A2 = A1.rotate(EAST, SOUTH, B)</code>
   * <pre>
   * <code>
   *
   *                _ .~¯ A1
   *           B  `
   *            \
   *             \
   *              A2
   * </code>
   * </pre>
   * @param from The base direction to draw the angle from
   * @param to The target direction to draw the angle from
   * @param xc The X coordinate of the center of the rotation
   * @param yc the Y coordinate of the center of the rotation.
   * @return The result of the rotation.
   */
  public Location rotate(Direction from, Direction to, int xc, int yc) {
    var degrees = to.toDegrees() - from.toDegrees();
    while (degrees >= 360) degrees -= 360;
    while (degrees < 0) degrees += 360;

    final var dx = x - xc;
    final var dy = y - yc;
    if (degrees == 90) {
      return create(xc + dy, yc - dx, hasToSnap);
    } else if (degrees == 180) {
      return create(xc - dx, yc - dy, hasToSnap);
    } else if (degrees == 270) {
      return create(xc - dy, yc + dx, hasToSnap);
    } else {
      return this;
    }
  }

  @Override
  public String toString() {
    return String.format("(%d,%d)", x, y);
  }

  /**
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Translation_(geometry)">
   * mathematical translation
   * </a>
   * of this point according to the given direction and distance.
   * The X axis increases to the east direction,
   * whereas the Y axis increases to the south direction.
   *
   * @param dir The direction to translate
   * @param dist The distance to translate
   * @return The newly created location object.
   */
  public Location translate(Direction dir, int dist) {
    return translate(dir, dist, 0);
  }

  /**
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Translation_(geometry)">
   * mathematical translation
   * </a>
   * of this point according to the given direction and distance.
   * The X axis increases to the east direction,
   * whereas the Y axis increases to the south direction.
   * The <code>right</code> parameter also allows for the translation of the location according
   * to the axis orthogonal to the provided direction. The name right derives from the fact
   * that the direction of its translation results from a clockwise 90° rotation of <code>dir</code>.
   *
   * @param dir The main axis direction
   * @param dist The distance to translate on the main axis
   * @param right The distance to translate orthogonal to the main axis
   * @return The newly created location object.
   */
  public Location translate(Direction dir, int dist, int right) {
    if (dist == 0 && right == 0) return this;
    if (dir == Direction.EAST) return Location.create(x + dist, y + right, hasToSnap);
    if (dir == Direction.WEST) return Location.create(x - dist, y - right, hasToSnap);
    if (dir == Direction.SOUTH) return Location.create(x - right, y + dist, hasToSnap);
    if (dir == Direction.NORTH) return Location.create(x + right, y - dist, hasToSnap);
    return Location.create(x + dist, y + right, hasToSnap);
  }

  /**
   * Performs the
   * <a href="https://en.wikipedia.org/wiki/Translation_(geometry)">
   * mathematical translation
   * </a>
   * of this point according to the provided x and y variation.
   * The X axis increases to the east direction,
   * whereas the Y axis increases to the south direction.
   * This operation can also be interpreted as the vector addition of <code>this</code> with
   * <code>(dx,dy)</code>
   *
   * @param dx The translation to perform on the X axis
   * @param dy The translation to perform on the Y axis
   * @return The newly created location object.
   */
  public Location translate(int dx, int dy) {
    if (dx == 0 && dy == 0) return this;
    return Location.create(x + dx, y + dy, hasToSnap);
  }

  /**
   * A general interface that defines objects that possess a current location.
   */
  public interface At {

    /**
     * The current location of <code>this</code> object.
     * The result of this operation is not final; an object may return different
     * locations upon separate calls of <code>getLocation</code>.
     * @return The current location of <code>this</code> object.
     */
    Location getLocation();
  }

  // Left before right, ties broken top before bottom, ties broken with hashcode
  // (same as default ordering using Location.compareTo() except hashcode).
  private static class Horizontal implements Comparator<At> {
    @Override
    public int compare(At a, At b) {
      final var aloc = a.getLocation();
      final var bloc = b.getLocation();
      if (aloc.x != bloc.x)
        return aloc.x - bloc.x;
      else if (aloc.y != bloc.y)
        return aloc.y - bloc.y;
      else
        return a.hashCode() - b.hashCode();
    }
  }

  // TODO: ^ it doesn't make a lot of sense to me to return the difference of their hashCodes,
  //  as it is guaranteed that they will have the same x and y components, thus the .equal and
  //  and their hashCodes have to be the same.

  /**
   * A {@link java.util.Comparator} that compares <code>At</code> objects with regard to the
   * X component of their locations, Ties regarding the X component are broken with the Y component.
   * If both components tie, the location are said to equal.
   */
  public static final Comparator<At> CompareHorizontal = new Horizontal();

  /**
   * Sorts a list of <code>At</code> object by the X component of their locations,
   * with <code>CompareHorizontal</code>.
   * @param list The list of locations to sort.
   * @param <T> The type of the objects stored in <code>list</code>.
   */
  public static <T extends At> void sortHorizontal(List<T> list) {
    list.sort(CompareHorizontal);
  }

  // Top before bottom, ties broken left before right, ties broken with hashcode.
  private static class Vertical implements Comparator<At> {
    @Override
    public int compare(At a, At b) {
      final var aloc = a.getLocation();
      final var bloc = b.getLocation();
      if (aloc.y != bloc.y)
        return aloc.y - bloc.y;
      else if (aloc.x != bloc.x)
        return aloc.x - bloc.x;
      else
        return a.hashCode() - b.hashCode();
    }
  }

  /**
   * A {@link java.util.Comparator} that compares <code>At</code> objects with regard to the
   * Y component of their locations, Ties regarding the Y component are broken with the X component.
   * If both components tie, the location are said to equal.
   */
  public static final Comparator<At> CompareVertical = new Vertical();

  /**
   * Sorts a list of <code>At</code> object by the Y component of their locations,
   * with <code>CompareVertical</code>.
   * @param list The list of locations to sort.
   * @param <T> The type of the objects stored in <code>list</code>.
   */
  public static <T extends At> void sortVertical(List<T> list) {
    list.sort(CompareVertical);
  }
}
