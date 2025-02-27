/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import java.util.Comparator;
import java.util.List;

/**
 * Represents an immutable rectangular bounding box. This is analogous to java.awt's <code>Point
 * </code> class, except that objects of this type are immutable.
 */
public class Location implements Comparable<Location> {
  public static Location create(int x, int y, boolean hasToSnap) {
    // we round to half-grid base
    final var xRounded = hasToSnap ? Math.round(x / 5) * 5 : x;
    final var yRounded = hasToSnap ? Math.round(y / 5) * 5 : y;
    // minimize hash collisions
    int hashCode = (int)Short.MAX_VALUE * xRounded + yRounded;
    // randomize order for better hashing
    hashCode ^= hashCode >> 14;
    hashCode ^= hashCode << 11;
    return new Location(hashCode, xRounded, yRounded, hasToSnap);
  }

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

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
  }

  @Override
  public int hashCode() {
    return hashCode;
  }

  public int manhattanDistanceTo(int x, int y) {
    return Math.abs(x - this.x) + Math.abs(y - this.y);
  }

  public int manhattanDistanceTo(Location o) {
    return Math.abs(o.x - this.x) + Math.abs(o.y - this.y);
  }

  // rotates this around (xc,yc) assuming that this is facing in the
  // from direction and the returned bounds should face in the to direction.
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

  public Location translate(Direction dir, int dist) {
    return translate(dir, dist, 0);
  }

  public Location translate(Direction dir, int dist, int right) {
    if (dist == 0 && right == 0) return this;
    if (dir == Direction.EAST) return Location.create(x + dist, y + right, hasToSnap);
    if (dir == Direction.WEST) return Location.create(x - dist, y - right, hasToSnap);
    if (dir == Direction.SOUTH) return Location.create(x - right, y + dist, hasToSnap);
    if (dir == Direction.NORTH) return Location.create(x + right, y - dist, hasToSnap);
    return Location.create(x + dist, y + right, hasToSnap);
  }

  public Location translate(int dx, int dy) {
    if (dx == 0 && dy == 0) return this;
    return Location.create(x + dx, y + dy, hasToSnap);
  }

  public interface At {
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

  public static final Comparator<At> CompareHorizontal = new Horizontal();

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

  public static final Comparator<At> CompareVertical = new Vertical();

  public static <T extends At> void sortVertical(List<T> list) {
    list.sort(CompareVertical);
  }
}
