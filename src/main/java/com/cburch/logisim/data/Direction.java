/**
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

package com.cburch.logisim.data;

import static com.cburch.logisim.data.Strings.S;

import com.cburch.logisim.util.StringGetter;

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
  public static final Direction WEST =
      new Direction("west", S.getter("directionWestOption"), S.getter("directionWestVertical"), 1);
  public static final Direction NORTH =
      new Direction("north", S.getter("directionNorthOption"), S.getter("directionNorthVertical"), 2);
  public static final Direction SOUTH =
      new Direction("south", S.getter("directionSouthOption"), S.getter("directionSouthVertical"), 3);

  public static final Direction[] cardinals = {NORTH, EAST, SOUTH, WEST};

  private String name;
  private StringGetter disp;
  private StringGetter vert;
  private int id;

  private Direction(String name, StringGetter disp, StringGetter vert, int id) {
    this.name = name;
    this.disp = disp;
    this.vert = vert;
    this.id = id;
  }

  public boolean equals(Direction other) {
    return this.id == other.id;
  }

  public StringGetter getDisplayGetter() {
    return disp;
  }

  public Direction getLeft() {
    if (this == Direction.EAST) return Direction.NORTH;
    if (this == Direction.WEST) return Direction.SOUTH;
    if (this == Direction.NORTH) return Direction.WEST;
    if (this == Direction.SOUTH) return Direction.EAST;
    return Direction.WEST;
  }

  public Direction getRight() {
    if (this == Direction.EAST) return Direction.SOUTH;
    if (this == Direction.WEST) return Direction.NORTH;
    if (this == Direction.NORTH) return Direction.EAST;
    if (this == Direction.SOUTH) return Direction.WEST;
    return Direction.WEST;
  }

  // for AttributeOptionInterface
  public Object getValue() {
    return this;
  }

  @Override
  public int hashCode() {
    return id;
  }

  public Direction reverse() {
    if (this == Direction.EAST) return Direction.WEST;
    if (this == Direction.WEST) return Direction.EAST;
    if (this == Direction.NORTH) return Direction.SOUTH;
    if (this == Direction.SOUTH) return Direction.NORTH;
    return Direction.WEST;
  }

  public int toDegrees() {
    if (this == Direction.EAST) return 0;
    if (this == Direction.WEST) return 180;
    if (this == Direction.NORTH) return 90;
    if (this == Direction.SOUTH) return 270;
    return 0;
  }

  public String toDisplayString() {
    return disp.toString();
  }

  public double toRadians() {
    if (this == Direction.EAST) return 0.0;
    if (this == Direction.WEST) return Math.PI;
    if (this == Direction.NORTH) return Math.PI / 2.0;
    if (this == Direction.SOUTH) return -Math.PI / 2.0;
    return 0.0;
  }

  @Override
  public String toString() {
    return name;
  }

  public String toVerticalDisplayString() {
    return vert.toString();
  }
}
