/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;

public class EndData {
  public static final int INPUT_ONLY = 1;
  public static final int OUTPUT_ONLY = 2;
  public static final int INPUT_OUTPUT = 3;

  private final Location loc;
  private final BitWidth width;
  private final int i_o;
  private final boolean exclusive;

  public EndData(Location loc, BitWidth width, int type) {
    this(loc, width, type, type == OUTPUT_ONLY);
  }

  public EndData(Location loc, BitWidth width, int type, boolean exclusive) {
    this.loc = loc;
    this.width = width;
    this.i_o = type;
    this.exclusive = exclusive;
  }

  @Override
  public boolean equals(Object other) {
    if (!(other instanceof EndData o)) return false;
    if (other == this) return true;
    return o.loc.equals(this.loc)
        && o.width.equals(this.width)
        && o.i_o == this.i_o
        && o.exclusive == this.exclusive;
  }

  public Location getLocation() {
    return loc;
  }

  public int getType() {
    return i_o;
  }

  public BitWidth getWidth() {
    return width;
  }

  public boolean isExclusive() {
    return exclusive;
  }

  public boolean isInput() {
    return (i_o & INPUT_ONLY) != 0;
  }

  public boolean isOutput() {
    return (i_o & OUTPUT_ONLY) != 0;
  }
}
