/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import static com.cburch.logisim.data.Strings.S;

import com.cburch.logisim.util.StringGetter;

/**
 * A class for representing 4-directional cardinal direction immutable objects.
 * <p>
 * Its only instances are <code>NORTH</code>, <code>SOUTH</code>, <code>EAST</code> and
 * <code>WEST</code>.
 */
public class Direction implements AttributeOptionInterface {
  public static Direction parse(String str) {
    if (str.equals(EAST.name)) return EAST;
    if (str.equals(WEST.name)) return WEST;
    if (str.equals(NORTH.name)) return NORTH;
    if (str.equals(SOUTH.name)) return SOUTH;
    throw new NumberFormatException("illegal direction '" + str + "'");
  }

  public static final Direction EAST =
      new Direction("east", S.getter("directionEastOption"), S.getter("directionEastVertical"), 0);
  public static final Direction NORTH =
      new Direction(
          "north", S.getter("directionNorthOption"), S.getter("directionNorthVertical"), 1);
  public static final Direction WEST =
      new Direction("west", S.getter("directionWestOption"), S.getter("directionWestVertical"), 2);
  public static final Direction SOUTH =
      new Direction(
          "south", S.getter("directionSouthOption"), S.getter("directionSouthVertical"), 3);

  /**
   * An array containing all 4 cardinal directions.
   */
  public static final Direction[] cardinals = {EAST, NORTH, WEST, SOUTH};

  private final String name;
  private final StringGetter disp;
  private final StringGetter vert;
  private final int id;

  private Direction(String name, StringGetter disp, StringGetter vert, int id) {
    this.name = name;
    this.disp = disp;
    this.vert = vert;
    this.id = id;
  }

  /**
   * Determines whether this direction object equals another one.
   * @param other The direction to compare to
   *
   * @return true iff these are the same direction objects.
   */
  public boolean equals(Direction other) {
    return this.id == other.id;
  }

  /**
   * @return a {@link StringGetter} with the description of this direction.
   */
  public StringGetter getDisplayGetter() {
    return disp;
  }

  /**
   * @return the cardinal direction resulting from a 90° counterclockwise rotation from this one.
   */
  public Direction getLeft() {
    return cardinals[(id + 1) % 4];
  }

  /**
   * @return the cardinal direction resulting from a 90° clockwise rotation from this one.
   */
  public Direction getRight() {
    return cardinals[(id + 3) % 4];
  }

  // for AttributeOptionInterface

  /**
   * The implementation for the equivalent method in {@link AttributeOptionInterface}
   *
   * @return This direction itself
   */
  public Object getValue() {
    return this;
  }

  @Override
  public int hashCode() {
    return id;
  }

  /**
   * @return the direction resulting from a 180º counterclockwise rotation.
   */
  public Direction reverse() {
    return cardinals[(id + 2) % 4];
  }

  /**
   * @return The angle associated with this direction, in degrees.
   *         The angle follows the mathematical convention of starting at EAST and
   *         increasing counterclockwise.
   */
  public int toDegrees() {
    return id * 90;
  }

  /**
   * @return The display string representing the cardinal direction name of this object,
   *         e.g. East.
   */
  public String toDisplayString() {
    return disp.toString();
  }

  /**
   * @return The angle associated with this direction, in radians.
   *         The angle follows the mathematical convention of starting at EAST and
   *         increasing counterclockwise.
   */
  public double toRadians() {
    return id * Math.PI / 2.0;
  }

  @Override
  public String toString() {
    return name;
  }

  /**
   * @return The display string representing the ordinary direction name of this object,
   *         e.g. Right.
   */
  public String toVerticalDisplayString() {
    return vert.toString();
  }
}
